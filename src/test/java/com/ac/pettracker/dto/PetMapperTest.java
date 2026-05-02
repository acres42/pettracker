package com.ac.pettracker.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ac.pettracker.model.Pet;
import org.junit.jupiter.api.Test;

class PetMapperTest {

  @Test
  void mapsPetToPetDto() {
    Pet pet =
        new Pet(
            "Buddy", "dog", "Golden Retriever", 4, "Friendly and loyal.", "/images/pets/buddy.jpg");

    PetDto dto = PetMapper.toDto(pet);

    assertEquals("Buddy", dto.name());
    assertEquals("dog", dto.type());
    assertEquals("Golden Retriever", dto.breed());
    assertEquals(4, dto.age());
    assertEquals("Friendly and loyal.", dto.description());
    assertEquals("/images/pets/buddy.jpg", dto.imageUrl());
  }
}
