package com.ac.pettracker.service;

import com.ac.pettracker.model.Pet;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PetService {

  private final List<Pet> pets =
      List.of(
          new Pet("Buddy", "dog", "Friendly golden retriever"),
          new Pet("Mittens", "cat", "Calm indoor cat"),
          new Pet("Thumper", "rabbit", "Energetic and playful"),
          new Pet("Max", "dog", "Loyal and protective"),
          new Pet("Luna", "cat", "Curious and affectionate"));

  public List<Pet> searchPets(String type, String location) {
    return pets.stream()
        .filter(pet -> pet.getType().equalsIgnoreCase(type))
        .collect(Collectors.toList());
  }
}
