package com.ac.pettracker.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.ac.pettracker.model.Pet;
import java.util.List;
import org.junit.jupiter.api.Test;

class PetRepositoryTest {

  private final PetRepository repo = new PetRepository();

  @Test
  void findAllReturnsPets() {
    List<Pet> pets = repo.findAll();

    assertFalse(pets.isEmpty());
  }

  @Test
  void findByTypeReturnsMatchingPets() {
    List<Pet> dogs = repo.findByType("dog");

    assertFalse(dogs.isEmpty());
    assertTrue(dogs.stream().allMatch((pet) -> pet.getType().equalsIgnoreCase("dog")));
  }

  @Test
  void findByTypeReturnsEmptyListWhenNoTypeMatches() {
    List<Pet> lizards = repo.findByType("lizard");

    assertTrue(lizards.isEmpty());
  }
}
