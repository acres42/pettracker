package com.ac.pettracker.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Spring configuration for the RescueGroups API HTTP client. */
@Configuration
public class RescueGroupsConfig {

  /**
   * Creates a {@link RestClient} pre-configured with the RescueGroups API base URL.
   *
   * @param baseUrl the RescueGroups API base URL
   * @return a configured {@link RestClient}
   */
  @Bean
  public RestClient rescueGroupsRestClient(
      @Value("${rescuegroups.base-url:https://api.rescuegroups.org/v5}") String baseUrl) {
    return RestClient.builder().baseUrl(baseUrl).build();
  }

  /**
   * Provides the shared {@link ObjectMapper} for JSON serialization.
   *
   * @return a default {@link ObjectMapper}
   */
  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }
}
