package com.ac.pettracker.client;

import com.ac.pettracker.model.Pet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

/**
 * RescueGroups API client using TOKEN BUCKET rate limiting for retries
 *
 * <p>Instead of exponential backoff, uses token bucket to respect API rate limits: - Allows bursty
 * retries up to bucket capacity - Then enforces rate limit wait time - Better than backoff when API
 * has known rate limits
 *
 * <p>Exponential: 1s, 2s, 4s, 8s (you're guessing delays) - Token bucket: Respects actual API rate
 * limit (more intelligent) - Token bucket: Fair queuing (everyone gets same tokens per second) -
 * Use exponential when you don't know rate limit - Use token bucket when you know API limit (e.g.,
 * 10 req/sec)
 */
@Service
public class RescueGroupsClientTokenBucket {

  private static final Logger logger = LoggerFactory.getLogger(RescueGroupsClientTokenBucket.class);
  private static final int MAX_RETRIES = 3;

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final TokenBucket rateLimiter; // 3 requests allowed per second

  @Autowired
  public RescueGroupsClientTokenBucket(
      @Value("${rescuegroups.base-url:https://api.rescuegroups.org/v5}") String baseUrl,
      @Value("${rescuegroups.api-key:}") String apiKey) {
    this(RestClient.builder().baseUrl(baseUrl).build(), new ObjectMapper(), apiKey);
  }

  /**
   * Testing constructor: uses a pre-built {@link RestClient} with a 3-token-per-second bucket.
   *
   * @param restClient the HTTP client to use
   * @param objectMapper the JSON mapper
   * @param apiKey the RescueGroups API key
   */
  public RescueGroupsClientTokenBucket(
      RestClient restClient, ObjectMapper objectMapper, String apiKey) {
    this.restClient = restClient;
    this.objectMapper = objectMapper;
    this.apiKey = apiKey;
    // TokenBucket(capacity, refill_rate_ms): 3 requests per 1000ms = 3 req/sec
    this.rateLimiter = new TokenBucket(3, 1000);
  }

  /**
   * Fetches available pets from the RescueGroups API for the given type and location, using
   * token-bucket rate limiting between retry attempts.
   *
   * <p>Returns an empty list when the API key is blank, on a 4xx error, or after all retries fail.
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
        // Check if we have a token available for this request
        long waitMs = rateLimiter.tryConsume();
        if (waitMs > 0) {
          logger.info(
              "Rate limit: waiting {}ms before attempt {}/{}", waitMs, attempt, MAX_RETRIES);
          Thread.sleep(waitMs);
        }

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
            "Successfully fetched pets from RescueGroups (attempt {}/{}, rate-limited)",
            attempt,
            MAX_RETRIES);
        return parseAnimals(responseBody, type);

      } catch (HttpServerErrorException e) {
        if (attempt < MAX_RETRIES) {
          logger.warn(
              "Token-bucket retry: Transient error (HTTP {}) on attempt {}/{};"
                  + " token bucket will enforce rate limit",
              e.getStatusCode(),
              attempt,
              MAX_RETRIES);
        } else {
          logger.warn(
              "Token-bucket retry: Transient error (HTTP {}) on final attempt {}/{}; giving up",
              e.getStatusCode(),
              attempt,
              MAX_RETRIES);
        }

      } catch (HttpClientErrorException e) {
        logger.warn(
            "Token-bucket retry: Permanent error (HTTP {}) on attempt {}/{}; no retry",
            e.getStatusCode(),
            attempt);
        return List.of();

      } catch (Exception e) {
        logger.warn("Failed to process RescueGroups request/response: {}", e.getMessage());
        return List.of();
      }
    }

    logger.warn("RescueGroups API call failed after {} attempts (rate limited)", MAX_RETRIES);
    return List.of();
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
          new Pet(
              name,
              type.toLowerCase(),
              breed,
              mapAgeBucket(ageGroup),
              description,
              imageUrl,
              null,
              null));
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
              return url;
            }
            return inc.path("attributes").path("url").asText("");
          }
        }
      }
    }
    return "";
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
