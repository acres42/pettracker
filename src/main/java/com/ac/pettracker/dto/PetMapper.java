package com.ac.pettracker.dto;

import com.ac.pettracker.model.Pet;

/** Utility class for converting {@link com.ac.pettracker.model.Pet} entities to DTOs. */
public final class PetMapper {

  private PetMapper() {}

  /**
   * Converts a {@link com.ac.pettracker.model.Pet} entity to a {@link PetDto}.
   *
   * @param pet the pet entity to convert
   * @return a new {@link PetDto} populated from the entity's fields
   */
  public static PetDto toDto(Pet pet) {
    return new PetDto(
        pet.getName(),
        pet.getType(),
        pet.getBreed(),
        pet.getAge(),
        pet.getDescription(),
        pet.getImageUrl());
  }
}
