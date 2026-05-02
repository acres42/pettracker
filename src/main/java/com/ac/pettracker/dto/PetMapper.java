package com.ac.pettracker.dto;

import com.ac.pettracker.model.Pet;

public final class PetMapper {

  private PetMapper() {}

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
