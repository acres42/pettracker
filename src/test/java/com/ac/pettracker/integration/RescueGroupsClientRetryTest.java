package com.ac.pettracker.integration;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

class RescueGroupsClientRetryTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void fetchPetsRetriesOnTransientFailureAndEventuallySucceeds()
      throws IOException, InterruptedException {
    // Capture logs
    Logger logger = (Logger) LoggerFactory.getLogger(RescueGroupsClient.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    // Setup mock server: fail twice with 503, succeed on 3rd
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    int port = server.getAddress().getPort();
    AtomicInteger attemptCount = new AtomicInteger(0);

    server.createContext(
        "/public/animals/search/available/dogs/",
        exchange -> {
          int attempt = attemptCount.incrementAndGet();
          if (attempt <= 2) {
            // Fail first 2 attempts
            exchange.sendResponseHeaders(503, 0);
          } else {
            // Succeed on 3rd attempt
            String response =
                """
            {
              "data": [
                {
                  "type": "animals",
                  "id": "123",
                  "attributes": {
                    "name": "Buddy",
                    "breedId": 1,
                    "ageGroup": "adult",
                    "pictureId": 1
                  },
                  "relationships": {
                    "breed": {"data": {"type": "breeds", "id": "1"}},
                    "pictures": {"data": [{"type": "pictures", "id": "1"}]}
                  }
                }
              ],
              "included": [
                {
                  "type": "breeds",
                  "id": "1",
                  "attributes": {"name": "Golden Retriever"}
                },
                {
                  "type": "pictures",
                  "id": "1",
                  "attributes": {
                    "urlSmall": "https://example.com/small.jpg",
                    "url": "https://example.com/large.jpg"
                  }
                }
              ]
            }
            """;
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
          }
          exchange.close();
        });

    server.start();

    try {
      // Act: Create client with fast backoff (10ms) for testing
      RestClient restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
      RescueGroupsClient client =
          new RescueGroupsClient(
              restClient, objectMapper, "test-key", 10L); // 10ms backoff for fast tests
      List<?> results = client.fetchPets("dog", "12345");

      // Assert: Results should come back (proving retry eventually succeeded)
      assertThat(results).isNotEmpty();
      assertThat(attemptCount.get()).isEqualTo(3); // Should have taken 3 attempts

      // Assert: Logs should show retry attempts
      List<ILoggingEvent> logEvents = listAppender.list;
      String logOutput =
          String.join("\n", logEvents.stream().map(ILoggingEvent::getFormattedMessage).toList());
      assertThat(logOutput).contains("Retry"); // Verify retry happened
    } finally {
      server.stop(0);
      logger.detachAppender(listAppender);
    }
  }

  @Test
  void fetchPetsDoesNotRetryOn400BadRequest() throws IOException, InterruptedException {
    // Capture logs
    Logger logger = (Logger) LoggerFactory.getLogger(RescueGroupsClient.class);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    logger.addAppender(listAppender);

    // Setup mock server: always return 400
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    int port = server.getAddress().getPort();
    AtomicInteger attemptCount = new AtomicInteger(0);

    server.createContext(
        "/public/animals/search/available/dogs/",
        exchange -> {
          attemptCount.incrementAndGet();
          exchange.sendResponseHeaders(400, 0);
          exchange.close();
        });

    server.start();

    try {
      // Act: Create client and call fetchPets
      RestClient restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
      RescueGroupsClient client = new RescueGroupsClient(restClient, objectMapper, "test-key");

      // Assert: Should return empty list on 400 (no retries), not throw
      List<?> results = client.fetchPets("dog", "12345");
      assertThat(results).isEmpty(); // Returns empty on permanent error
      assertThat(attemptCount.get()).isEqualTo(1); // Only 1 attempt, no retries
    } finally {
      server.stop(0);
      logger.detachAppender(listAppender);
    }
  }
}
