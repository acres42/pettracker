package com.ac.pettracker.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ac.pettracker.repository.PetRepository;
import org.junit.jupiter.api.Test;

class PetServiceTest {

  private final PetRepository petRepository = new PetRepository();
  private final PetService petService = new PetService(petRepository);

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
}
