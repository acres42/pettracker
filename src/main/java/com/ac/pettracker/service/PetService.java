package com.ac.pettracker.service;

import com.ac.pettracker.model.Pet;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PetService {

  private final List<Pet> pets =
      List.of(
          new Pet(
              "Buddy",
              "dog",
              "Golden Retriever",
              4,
              "Friendly and loyal.",
              "/images/pets/buddy.jpg"),
          new Pet(
              "Mittens",
              "cat",
              "Domestic Shorthair",
              2,
              "Curious and affectionate.",
              "/images/pets/mittens.jpg"),
          new Pet(
              "Daisy", "dog", "Beagle", 3, "Playful and food-motivated.", "/images/pets/daisy.jpg"),
          new Pet(
              "Clover", "rabbit", "Mini Rex", 1, "Gentle and quiet.", "/images/pets/clover.jpg"),
          new Pet("Sunny", "bird", "Cockatiel", 5, "Social and chatty.", "/images/pets/sunny.jpg"));

  public List<Pet> searchPets(String type, String location) {
    return pets.stream()
        .filter(pet -> pet.getType().equalsIgnoreCase(type))
        .collect(Collectors.toList());
  }
}
