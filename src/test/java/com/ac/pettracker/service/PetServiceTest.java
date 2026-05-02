package com.ac.pettracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ac.pettracker.integration.RescueGroupsClient;
import com.ac.pettracker.model.Pet;
import com.ac.pettracker.repository.PetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class PetServiceTest {

  private final RescueGroupsClient rescueGroupsClient =
      new RescueGroupsClient(RestClient.builder().build(), new ObjectMapper(), "");
  private final PetRepository petRepository = new PetRepository();
  private final PetService petService = new PetService(rescueGroupsClient, petRepository);

  @Test
  void searchPetsReturnsOnlyMatchingType() {
    var results = petService.searchPets("dog", "46201");

    assertThat(results).extracting("type").containsOnly("dog");
  }

  @Test
  void searchPetsMatchesTypeCaseInsensitively() {
    var results = petService.searchPets("DOG", "46201");

    assertThat(results).extracting("type").containsOnly("dog");
  }

  @Test
  void searchPetsReturnsEmptyListWhenNoTypeMatches() {
    var results = petService.searchPets("lizard", "46201");

    assertThat(results).isEmpty();
  }

  @Test
  void searchPetsReturnsPetsWithBreedAgeAndImageUrl() {
    var results = petService.searchPets("dog", "46201");

    assertThat(results).isNotEmpty();

    assertThat(results.getFirst().getBreed()).isNotBlank();
    assertThat(results.getFirst().getAge()).isGreaterThan(0);
    assertThat(results.getFirst().getImageUrl()).isNotBlank();
  }

  @Test
  void searchPetsSortsByName() {
    List<Pet> pets = petService.searchPets("dog", "46201", "name");

    assertTrue(pets.get(0).getName().compareTo(pets.get(1).getName()) <= 0);
  }

  @Test
  void searchPetsSortsByType() {
    List<Pet> pets = petService.searchPets("dog", "46201", "type");

    assertTrue(pets.get(0).getType().compareTo(pets.get(1).getType()) <= 0);
  }

  @Test
  void searchPetsReturnsFirstPage() {
    List<Pet> results = petService.searchPets("dog", "46201", null, 0, 2);

    assertEquals(2, results.size());
    assertEquals("Buddy", results.get(0).getName());
  }

  @Test
  void searchPetsReturnsSecondPage() {
    List<Pet> results = petService.searchPets("dog", "46201", null, 1, 2);

    assertEquals(0, results.size());
  }

  @Test
  void searchPetsUsesApiResultsWhenAvailable() {
    RescueGroupsClient apiClient =
        new RescueGroupsClient(RestClient.builder().build(), new ObjectMapper(), "") {
          @Override
          public List<Pet> fetchPets(String type, String location) {
            return List.of(
                new Pet("Rex", "dog", "Husky", 3, "Energetic and social", "https://image"));
          }
        };
    PetService apiBackedService = new PetService(apiClient, petRepository);

    List<Pet> results = apiBackedService.searchPets("dog", "46201");

    assertEquals(1, results.size());
    assertEquals("Rex", results.getFirst().getName());
  }

  @Test
  void searchPetsFallsBackToRepositoryWhenApiReturnsNoResults() {
    RescueGroupsClient apiClient =
        new RescueGroupsClient(RestClient.builder().build(), new ObjectMapper(), "") {
          @Override
          public List<Pet> fetchPets(String type, String location) {
            return List.of();
          }
        };
    PetService apiBackedService = new PetService(apiClient, petRepository);

    List<Pet> results = apiBackedService.searchPets("dog", "46201");

    assertTrue(results.size() >= 2);
    assertThat(results).extracting(Pet::getName).contains("Buddy");
  }
}
