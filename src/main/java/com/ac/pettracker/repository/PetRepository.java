package com.ac.pettracker.repository;

import com.ac.pettracker.model.Pet;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * Pet data access component that delegates to the JPA repository when running under Spring, or
 * falls back to a hardcoded list when used directly in unit tests.
 */
@Repository
public class PetRepository {

  private static final List<Pet> FALLBACK_PETS =
      List.of(
          new Pet(
              "Buddy",
              "dog",
              "Golden Retriever",
              4,
              "Friendly and loyal.",
              "/images/pets/default.jpg",
              "male",
              null),
          new Pet(
              "Mittens",
              "cat",
              "Domestic Shorthair",
              2,
              "Curious and affectionate.",
              "/images/pets/default.jpg",
              "female",
              null),
          new Pet(
              "Daisy",
              "dog",
              "Beagle",
              3,
              "Playful and food-motivated.",
              "/images/pets/default.jpg",
              "female",
              null),
          new Pet(
              "Clover",
              "rabbit",
              "Mini Rex",
              1,
              "Gentle and quiet.",
              "/images/pets/clover.jpg",
              "female",
              null),
          new Pet(
              "Sunny",
              "bird",
              "Cockatiel",
              5,
              "Social and chatty.",
              "/images/pets/default.jpg",
              "male",
              null));

  private final PetJpaRepository petJpaRepository;

  /** No-arg constructor used directly in unit tests (no Spring context). */
  public PetRepository() {
    this.petJpaRepository = null;
  }

  /** Spring-managed constructor: uses DB-backed JPA repository with hardcoded fallback. */
  @Autowired
  public PetRepository(PetJpaRepository petJpaRepository) {
    this.petJpaRepository = petJpaRepository;
  }

  /**
   * Returns all pets, using the database when available or the fallback list otherwise.
   *
   * @return non-null list of all pets
   */
  public List<Pet> findAll() {
    if (petJpaRepository == null) {
      return FALLBACK_PETS;
    }
    List<Pet> dbPets = petJpaRepository.findAll();
    return dbPets.isEmpty() ? FALLBACK_PETS : dbPets;
  }

  /**
   * Returns pets matching the given type, using the database when available.
   *
   * @param type the pet species to filter by (case-insensitive)
   * @return non-null list of matching pets
   */
  public List<Pet> findByType(String type) {
    if (petJpaRepository == null) {
      return FALLBACK_PETS.stream().filter(pet -> pet.getType().equalsIgnoreCase(type)).toList();
    }
    List<Pet> dbPets = petJpaRepository.findByTypeIgnoreCase(type);
    return dbPets.isEmpty()
        ? FALLBACK_PETS.stream().filter(pet -> pet.getType().equalsIgnoreCase(type)).toList()
        : dbPets;
  }
}
