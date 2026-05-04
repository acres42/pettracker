package com.ac.pettracker.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ac.pettracker.model.Pet;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class RescueGroupsClientTest {

  @Test
  void fetchPetsReturnsMappedResultsFromApi() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/public/animals/search/available/dogs/", this::handleAnimalsRequest);
    server.start();

    String baseUrl = "http://localhost:" + server.getAddress().getPort();
    RescueGroupsClient client =
        new RescueGroupsClient(
            RestClient.builder().baseUrl(baseUrl).build(),
            new ObjectMapper(),
            "test-api-key",
            1000L);

    try {
      List<Pet> pets = client.fetchPets("dog", "46201");

      assertEquals(1, pets.size());
      Pet pet = pets.getFirst();
      assertEquals("Maple", pet.getName());
      assertEquals("dog", pet.getType());
      assertEquals("Labrador Mix", pet.getBreed());
      assertEquals(4, pet.getAge());
      assertTrue(pet.getImageUrl().contains("example.com"));
    } finally {
      server.stop(0);
    }
  }

  @Test
  void fetchPetsReturnsEmptyWhenApiKeyIsMissing() {
    RescueGroupsClient client =
        new RescueGroupsClient(RestClient.builder().build(), new ObjectMapper(), "", 1000L);

    List<Pet> pets = client.fetchPets("dog", "46201");

    assertTrue(pets.isEmpty());
  }

  private void handleAnimalsRequest(HttpExchange exchange) throws IOException {
    assertEquals("POST", exchange.getRequestMethod());
    assertEquals("test-api-key", exchange.getRequestHeaders().getFirst("Authorization"));

    String response =
        """
        {
          "data": [
            {
              "type": "animals",
              "id": "111",
              "attributes": {
                "name": "Maple",
                "ageGroup": "Adult",
                "descriptionText": "Calm and affectionate."
              },
              "relationships": {
                "breeds": { "data": [{ "type": "breeds", "id": "35" }] },
                "pictures": { "data": [{ "type": "pictures", "id": "99" }] }
              }
            }
          ],
          "included": [
            {
              "type": "breeds",
              "id": "35",
              "attributes": { "name": "Labrador Mix" }
            },
            {
              "type": "pictures",
              "id": "99",
              "attributes": { "urlSmall": "https://example.com/maple.jpg" }
            }
          ]
        }
        """;
    sendJsonResponse(exchange, response);
  }

  private void sendJsonResponse(HttpExchange exchange, String response) throws IOException {
    exchange.getResponseHeaders().add("Content-Type", "application/vnd.api+json");
    byte[] responseBytes = response.getBytes();
    exchange.sendResponseHeaders(200, responseBytes.length);
    try (OutputStream outputStream = exchange.getResponseBody()) {
      outputStream.write(responseBytes);
    }
  }
}
