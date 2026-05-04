package com.ac.pettracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ac.pettracker.client.RescueGroupsClient;
import com.ac.pettracker.model.Pet;
import com.ac.pettracker.repository.PetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class PetServiceTest {

  private final RescueGroupsClient rescueGroupsClient =
      new RescueGroupsClient(RestClient.builder().build(), new ObjectMapper(), "", 1000L);
  private final PetRepository petRepository = new PetRepository();
  private final PetService petService = new PetService(rescueGroupsClient, petRepository);

  @Test
  void searchPetsReturnsOnlyMatchingType() {
    var results = petService.searchPets("dog");
  }

  @Test
  void searchPetsMatchesTypeCaseInsensitively() {
    var results = petService.searchPets("DOG");
  }

  @Test
  void searchPetsReturnsEmptyListWhenNoTypeMatches() {
    var results = petService.searchPets("lizard");
  }

  @Test
  void searchPetsReturnsPetsWithBreedAgeAndImageUrl() {
    var results = petService.searchPets("dog");

    assertThat(results.getFirst().getBreed()).isNotBlank();
    assertThat(results.getFirst().getAge()).isGreaterThan(0);
    assertThat(results.getFirst().getImageUrl()).isNotBlank();
  }

  @Test
  void searchPetsUsesApiResultsWhenAvailable() {
    RescueGroupsClient apiClient =
        new RescueGroupsClient(RestClient.builder().build(), new ObjectMapper(), "", 1000L) {
          @Override
          public List<Pet> fetchPets(String type) {
            return List.of(
                new Pet(
                    "Rex", "dog", "Husky", 3, "Energetic and social", "https://image", null, null));
          }
        };
    PetService apiBackedService = new PetService(apiClient, petRepository);

    List<Pet> results = apiBackedService.searchPets("dog");

    assertEquals(1, results.size());
    assertEquals("Rex", results.getFirst().getName());
  }

  @Test
  void searchPetsFallsBackToRepositoryWhenApiReturnsNoResults() {
    RescueGroupsClient apiClient =
        new RescueGroupsClient(RestClient.builder().build(), new ObjectMapper(), "", 1000L) {
          @Override
          public List<Pet> fetchPets(String type) {
            return List.of();
          }
        };
    PetService apiBackedService = new PetService(apiClient, petRepository);

    List<Pet> results = apiBackedService.searchPets("dog");

    assertTrue(results.size() >= 2);
    assertThat(results).extracting(Pet::getName).contains("Buddy");
  }

  // --- getSuggestedPets tests ---

  private PetRepository suggestedPetsRepo() {
    return new PetRepository() {
      private final List<Pet> testPets =
          List.of(
              new Pet("Rex", "dog", "Husky", 3, "Energetic and social", "/img/rex.jpg", "male", 45),
              new Pet(
                  "Luna", "cat", "Siamese", 2, "Quiet and elegant", "/img/luna.jpg", "female", 10),
              new Pet(
                  "Biscuit",
                  "dog",
                  "Golden Retriever",
                  5,
                  "Friendly and fluffy",
                  "/img/biscuit.jpg",
                  "female",
                  70),
              new Pet(
                  "Titan", "dog", "Great Dane", 4, "Gentle giant", "/img/titan.jpg", "male", 120));

      @Override
      public List<Pet> findAll() {
        return testPets;
      }

      @Override
      public List<Pet> findByType(String type) {
        return testPets.stream().filter(p -> p.getType().equalsIgnoreCase(type)).toList();
      }
    };
  }

  @Test
  void getSuggestedPetsReturnsAllWhenNoPreferences() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    List<Pet> result = svc.getSuggestedPets("", "", "", "", "", Set.of());

    assertThat(result).hasSize(4);
  }

  @Test
  void getSuggestedPetsExcludesSavedPets() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());
    Set<String> saved = Set.of("Rex|dog|Husky|3");

    List<Pet> result = svc.getSuggestedPets("", "", "", "", "", saved);

    assertThat(result).extracting(Pet::getName).doesNotContain("Rex");
    assertThat(result).hasSize(3);
  }

  @Test
  void getSuggestedPetsFiltersBySpecies() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    List<Pet> result = svc.getSuggestedPets("dog", "", "", "", "", Set.of());

    assertThat(result).extracting(Pet::getType).containsOnly("dog");
    assertThat(result).hasSize(3);
  }

  @Test
  void getSuggestedPetsFiltersByGender() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    List<Pet> result = svc.getSuggestedPets("", "male", "", "", "", Set.of());

    assertThat(result).extracting(Pet::getName).containsExactlyInAnyOrder("Rex", "Titan");
  }

  @Test
  void getSuggestedPetsFiltersByBreed() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    List<Pet> result = svc.getSuggestedPets("", "", "", "golden", "", Set.of());

    assertThat(result).extracting(Pet::getName).containsExactly("Biscuit");
  }

  @Test
  void getSuggestedPetsFiltersByKeyword() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    List<Pet> result = svc.getSuggestedPets("", "", "", "", "gentle", Set.of());

    assertThat(result).extracting(Pet::getName).containsExactly("Titan");
  }

  @Test
  void getSuggestedPetsFiltersByWeightBandUnder25() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    List<Pet> result = svc.getSuggestedPets("", "", "<25 lbs", "", "", Set.of());

    assertThat(result).extracting(Pet::getName).containsExactly("Luna");
  }

  @Test
  void getSuggestedPetsFiltersByWeightBand25To50() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    List<Pet> result = svc.getSuggestedPets("", "", "25-50lbs", "", "", Set.of());

    assertThat(result).extracting(Pet::getName).containsExactly("Rex");
  }

  @Test
  void getSuggestedPetsFiltersByWeightBandOver100() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    List<Pet> result = svc.getSuggestedPets("", "", ">100lbs", "", "", Set.of());

    assertThat(result).extracting(Pet::getName).containsExactly("Titan");
  }

  @Test
  void getSuggestedPetsCombinesMultipleFilters() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    List<Pet> result = svc.getSuggestedPets("dog", "male", "", "", "", Set.of());

    assertThat(result).extracting(Pet::getName).containsExactlyInAnyOrder("Rex", "Titan");
  }

  @Test
  void searchPetsFiltersByGender() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    List<Pet> result = svc.searchPets("dog", "female", "");

    assertThat(result).extracting(Pet::getName).containsExactly("Biscuit");
  }

  @Test
  void searchPetsFiltersByAgeBandAdult() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    List<Pet> result = svc.searchPets("dog", "", "adult");

    assertThat(result)
        .extracting(Pet::getName)
        .containsExactlyInAnyOrder("Rex", "Biscuit", "Titan");
  }

  @Test
  void searchPetsFiltersByAgeBandYoungReturnsNone() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    List<Pet> result = svc.searchPets("dog", "", "young");

    assertThat(result).isEmpty();
  }

  @Test
  void searchPetsWithBlankFiltersReturnsAllOfType() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    List<Pet> result = svc.searchPets("dog", "", "");

    assertThat(result).hasSize(3);
  }

  // --- searchPets with keyword filtering ---

  @Test
  void searchPetsWithKeywordsMatchesDescriptionAndReturnsMatchingPets() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    com.ac.pettracker.dto.PetSearchResult result = svc.searchPets("dog", "", "", "fluffy");

    assertThat(result.pets()).extracting(Pet::getName).containsExactly("Biscuit");
    assertThat(result.unmatchedKeywords()).isEmpty();
  }

  @Test
  void searchPetsWithKeywordsIsDescriptionOnlyNotNameOrBreed() {
    // "Rex" appears in the name but not the description; should not match
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    com.ac.pettracker.dto.PetSearchResult result = svc.searchPets("dog", "", "", "Rex");

    assertThat(result.pets()).isEmpty();
    assertThat(result.unmatchedKeywords()).containsExactly("Rex");
  }

  @Test
  void searchPetsWithKeywordsIsCaseInsensitive() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    com.ac.pettracker.dto.PetSearchResult result = svc.searchPets("dog", "", "", "FLUFFY");

    assertThat(result.pets()).extracting(Pet::getName).containsExactly("Biscuit");
  }

  @Test
  void searchPetsWithKeywordsUsesOrLogicAcrossTerms() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    com.ac.pettracker.dto.PetSearchResult result = svc.searchPets("dog", "", "", "fluffy, gentle");

    assertThat(result.pets())
        .extracting(Pet::getName)
        .containsExactlyInAnyOrder("Biscuit", "Titan");
    assertThat(result.unmatchedKeywords()).isEmpty();
  }

  @Test
  void searchPetsWithKeywordsReportsUnmatchedTerms() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    com.ac.pettracker.dto.PetSearchResult result = svc.searchPets("dog", "", "", "fluffy, playful");

    assertThat(result.pets()).extracting(Pet::getName).containsExactly("Biscuit");
    assertThat(result.unmatchedKeywords()).containsExactly("playful");
  }

  @Test
  void searchPetsWithKeywordsLimitsToFirst5Terms() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    // 6th term "energetic" matches Rex — it is beyond the 5-term limit so Rex should not match;
    // first 5 unique terms ("thunderstorm","invisible","purple","nebula","zephyr") match nothing
    com.ac.pettracker.dto.PetSearchResult result =
        svc.searchPets("dog", "", "", "thunderstorm, invisible, purple, nebula, zephyr, energetic");

    assertThat(result.pets()).isEmpty();
  }

  @Test
  void searchPetsWithBlankKeywordsReturnsAllCandidates() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    com.ac.pettracker.dto.PetSearchResult result = svc.searchPets("dog", "", "", "");

    assertThat(result.pets()).hasSize(3);
    assertThat(result.unmatchedKeywords()).isEmpty();
  }

  @Test
  void searchPetsWithKeywordsDeduplicatesTermsBeforeLimitIsApplied() {
    PetService svc = new PetService(rescueGroupsClient, suggestedPetsRepo());

    // "fluffy" repeated 5 times deduplicates to 1, so "energetic" becomes the 2nd unique term
    com.ac.pettracker.dto.PetSearchResult result =
        svc.searchPets("dog", "", "", "fluffy, fluffy, fluffy, fluffy, fluffy, energetic");

    assertThat(result.pets()).extracting(Pet::getName).containsExactlyInAnyOrder("Biscuit", "Rex");
  }
}
