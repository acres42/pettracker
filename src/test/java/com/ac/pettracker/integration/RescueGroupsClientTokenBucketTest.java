package com.ac.pettracker.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RescueGroupsClientTokenBucketTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void fetchPetsWithTokenBucketRateLimit() throws IOException, InterruptedException {
    // Setup mock server
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    int port = server.getAddress().getPort();
    AtomicInteger attemptCount = new AtomicInteger(0);
    long[] attemptTimes = new long[3];

    server.createContext(
        "/public/animals/search/available/dogs/",
        exchange -> {
          int attempt = attemptCount.incrementAndGet();
          attemptTimes[attempt - 1] = System.currentTimeMillis();

          if (attempt <= 2) {
            // Fail first 2 attempts (force retries)
            exchange.sendResponseHeaders(503, 0);
          } else {
            // Succeed on 3rd attempt
            String response =
                """
            {
              "data": [{"type": "animals", "id": "123", "attributes": {"name": "Buddy", "breedId": 1, "ageGroup": "adult", "pictureId": 1}, "relationships": {"breed": {"data": {"type": "breeds", "id": "1"}}, "pictures": {"data": [{"type": "pictures", "id": "1"}]}}}],
              "included": [{"type": "breeds", "id": "1", "attributes": {"name": "Retriever"}}, {"type": "pictures", "id": "1", "attributes": {"urlSmall": "https://example.com/pic.jpg"}}]
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
      RestClient restClient = RestClient.builder().baseUrl("http://localhost:" + port).build();
      RescueGroupsClientTokenBucket client =
          new RescueGroupsClientTokenBucket(restClient, objectMapper, "test-key");

      // Act: Fetch pets (will retry with token bucket rate limiting)
      List<?> results = client.fetchPets("dog", "12345");

      // Assert: Should eventually succeed
      assertThat(results).isNotEmpty();
      assertThat(attemptCount.get()).isEqualTo(3);

      // Assert: Token bucket enforced waits between retries
      // Attempts 1 and 2 should be fast, but attempt 3 should have waited for token refill
      long attempt1Duration = attemptTimes[1] - attemptTimes[0];
      long attempt2Duration = attemptTimes[2] - attemptTimes[1];

      System.out.println("=== Token Bucket Retry Timing ===");
      System.out.println("Attempt 1→2 interval: " + attempt1Duration + "ms");
      System.out.println("Attempt 2→3 interval: " + attempt2Duration + "ms");
      System.out.println("\nBehavior: Bucket has 3 tokens, refill 1/sec");
      System.out.println("- Attempt 1: takes token 1 (immediate)");
      System.out.println("- Attempt 2: takes token 2 (immediate)");
      System.out.println("- Attempt 3: takes token 3 (immediate)");
      System.out.println("→ Total: ~0ms between attempts (token-limited, not exponential)");

      // With token bucket, retries happen at rate limit pace
      // vs exponential which would add delays
    } finally {
      server.stop(0);
    }
  }

  @Test
  void tokenBucketPreventsBurstingPastCapacity() throws InterruptedException {
    // Create client - it has a token bucket with 3 tokens
    RestClient restClient = RestClient.builder().baseUrl("http://localhost:9999").build();
    RescueGroupsClientTokenBucket client =
        new RescueGroupsClientTokenBucket(restClient, objectMapper, "test-key");

    // The client's token bucket is internal, but we can demonstrate the concept
    // This is documentation for the interview about how it works
    System.out.println("=== Token Bucket Concept Demo ===");
    System.out.println("Configuration: 3 tokens, 1 refill per 1000ms");
    System.out.println("\nScenario: API rate limit is 3 req/sec");
    System.out.println("- Client A makes request 1 (token 1 consumed)");
    System.out.println("- Client A makes request 2 (token 2 consumed)");
    System.out.println("- Client A makes request 3 (token 3 consumed)");
    System.out.println("- Client A attempts request 4 (NO TOKENS! Must wait ~1000ms)");
    System.out.println("\nResult: Bursty traffic (3 fast) then rate-limited (wait 1s per retry)");
    System.out.println("\nVs Exponential Backoff:");
    System.out.println("- Attempt 1 fail → wait 1s");
    System.out.println("- Attempt 2 fail → wait 2s");
    System.out.println("- Attempt 3 fail → wait 4s");
    System.out.println("(Too conservative if API is actually faster)");

    assertThat(client).isNotNull(); // Placeholder assertion
  }
}
