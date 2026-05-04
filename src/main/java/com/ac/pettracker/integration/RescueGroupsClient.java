package com.ac.pettracker.integration;

import com.ac.pettracker.model.Pet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for the RescueGroups API with exponential back-off retry on transient errors.
 *
 * <p>Retries up to {@code MAX_RETRIES} times with jittered exponential delays before giving up and
 * returning an empty list.
 */
@Component
public class RescueGroupsClient {

  private static final Logger logger = LoggerFactory.getLogger(RescueGroupsClient.class);
  private static final int MAX_RETRIES = 3;

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final long baseBackoffMs;
  private final Random random;

  public RescueGroupsClient(
      RestClient restClient,
      ObjectMapper objectMapper,
      @Value("${rescuegroups.api-key:}") String apiKey,
      @Value("${rescuegroups.retry-backoff-ms:1000}") long baseBackoffMs) {
    this.restClient = restClient;
    this.objectMapper = objectMapper;
    this.apiKey = apiKey;
    this.baseBackoffMs = baseBackoffMs;
    this.random = new Random();
  }

  /**
   * Fetches available pets from the RescueGroups API for the given type and location.
   *
   * <p>Retries up to {@code MAX_RETRIES} times on 5xx server errors using exponential back-off.
   * Returns an empty list when the API key is blank, on a 4xx error, or after all retries fail.
   *
   * @param type the pet species (e.g., "dog", "cat")
   * @param location the search location string passed in the request body
   * @return non-null list of pets returned by the API; empty on failure or no results
   */
  public List<Pet> fetchPets(String type, String location) {
    if (apiKey == null || apiKey.isBlank()) {
      logger.warn("RescueGroups API key not configured; returning no API results");
      return List.of();
    }

    for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
      try {
        String species = toSpeciesView(type);
        String requestBody = buildRequestBody(location);

        String responseBody =
            restClient
                .post()
                .uri("/public/animals/search/available/" + species + "/")
                .header("Authorization", apiKey)
                .contentType(MediaType.parseMediaType("application/vnd.api+json"))
                .body(requestBody)
                .retrieve()
                .onStatus(
                    status -> status.is5xxServerError(),
                    (request, response) -> {
                      throw new HttpServerErrorException(response.getStatusCode());
                    })
                .onStatus(
                    status -> status.is4xxClientError(),
                    (request, response) -> {
                      throw new HttpClientErrorException(response.getStatusCode());
                    })
                .body(String.class);

        logger.info(
            "Successfully fetched pets from RescueGroups (attempt {}/{})", attempt, MAX_RETRIES);
        return parseAnimals(responseBody, type);

      } catch (HttpServerErrorException e) {
        if (attempt < MAX_RETRIES) {
          long backoffMs = exponentialBackoffWithJitter(attempt);
          logger.warn(
              "Retry: Transient error (HTTP {}) on attempt {}/{}; waiting {}ms before retry",
              e.getStatusCode(),
              attempt,
              MAX_RETRIES,
              backoffMs);
          sleep(backoffMs);
        } else {
          logger.warn(
              "Retry: Transient error (HTTP {}) on final attempt {}/{}; giving up",
              e.getStatusCode(),
              attempt,
              MAX_RETRIES);
        }

      } catch (HttpClientErrorException e) {
        logger.warn(
            "No retry: Permanent error (HTTP {}) on attempt {}/{}",
            e.getStatusCode(),
            attempt,
            MAX_RETRIES);
        return List.of();

      } catch (Exception e) {
        logger.warn(
            "Failed to process RescueGroups request/response on attempt {}/{}: {}",
            attempt,
            MAX_RETRIES,
            e.getMessage());
        return List.of();
      }
    }

    // All retries exhausted due to transient errors
    logger.warn("RescueGroups API call failed after {} retry attempts", MAX_RETRIES);
    return List.of();
  }

  private long exponentialBackoffWithJitter(int attempt) {
    long baseDelay = baseBackoffMs * (long) Math.pow(2, attempt - 1);
    long jitter = random.nextLong(baseDelay);
    return baseDelay + jitter;
  }

  private void sleep(long milliseconds) {
    try {
      Thread.sleep(milliseconds);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.warn("Retry sleep interrupted");
    }
  }

  private String toSpeciesView(String type) {
    return switch (type.toLowerCase()) {
      case "dog" -> "dogs";
      case "cat" -> "cats";
      case "rabbit" -> "rabbits";
      case "bird" -> "birds";
      default -> type.toLowerCase() + "s";
    };
  }

  private String buildRequestBody(String location) throws Exception {
    ObjectNode root = objectMapper.createObjectNode();
    ObjectNode data = objectMapper.createObjectNode();
    if (location != null && !location.isBlank()) {
      ObjectNode filterRadius = objectMapper.createObjectNode();
      filterRadius.put("postalcode", location);
      filterRadius.put("miles", 100);
      data.set("filterRadius", filterRadius);
    }
    root.set("data", data);
    return objectMapper.writeValueAsString(root);
  }

  private List<Pet> parseAnimals(String responseBody, String type) throws Exception {
    JsonNode root = objectMapper.readTree(responseBody);
    JsonNode dataArray = root.path("data");
    JsonNode included = root.path("included");

    if (!dataArray.isArray()) {
      return List.of();
    }

    List<Pet> pets = new ArrayList<>();
    for (JsonNode animal : dataArray) {
      JsonNode attrs = animal.path("attributes");
      String name = attrs.path("name").asText("Unknown");
      String ageGroup = attrs.path("ageGroup").asText("");
      String description = attrs.path("descriptionText").asText("No description available.");
      String breed = findBreed(animal, included);
      String imageUrl = findImageUrl(animal, included);
      pets.add(
          new Pet(name, type.toLowerCase(), breed, mapAgeBucket(ageGroup), description, imageUrl));
    }
    return pets;
  }

  private String findBreed(JsonNode animal, JsonNode included) {
    JsonNode breedData = animal.path("relationships").path("breeds").path("data");
    if (breedData.isArray() && !breedData.isEmpty()) {
      String breedId = breedData.get(0).path("id").asText();
      if (included.isArray()) {
        for (JsonNode inc : included) {
          if ("breeds".equals(inc.path("type").asText())
              && breedId.equals(inc.path("id").asText())) {
            return inc.path("attributes").path("name").asText("Unknown");
          }
        }
      }
    }
    return "Unknown";
  }

  private String findImageUrl(JsonNode animal, JsonNode included) {
    JsonNode picData = animal.path("relationships").path("pictures").path("data");
    if (picData.isArray() && !picData.isEmpty()) {
      String picId = picData.get(0).path("id").asText();
      if (included.isArray()) {
        for (JsonNode inc : included) {
          if ("pictures".equals(inc.path("type").asText())
              && picId.equals(inc.path("id").asText())) {
            String url = inc.path("attributes").path("urlSmall").asText("");
            if (!url.isBlank()) {
              return validateImageUrl(url);
            }
            return validateImageUrl(inc.path("attributes").path("url").asText(""));
          }
        }
      }
    }
    return "";
  }

  /**
   * Returns the URL if it begins with {@code https://}, otherwise returns an empty string. This
   * prevents {@code javascript:} and {@code data:} URIs from being stored or rendered.
   *
   * @param url the candidate image URL
   * @return the URL unchanged if it is HTTPS, or {@code ""} if not
   */
  private String validateImageUrl(String url) {
    if (url == null || !url.startsWith("https://")) {
      return "";
    }
    return url;
  }

  private int mapAgeBucket(String ageGroup) {
    return switch (ageGroup.toLowerCase()) {
      case "baby" -> 1;
      case "young" -> 2;
      case "adult" -> 4;
      case "senior" -> 8;
      default -> 0;
    };
  }
}
