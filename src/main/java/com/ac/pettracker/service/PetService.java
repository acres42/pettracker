package com.ac.pettracker.service;

import com.ac.pettracker.model.Pet;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PetService {

  public List<Pet> searchPets(String type, String location) {
    return List.of(
        new Pet(type + " Buddy", type, "Friendly and adoptable"),
        new Pet(type + " Max", type, "Loves people"),
        new Pet(type + " Luna", type, "Very energetic"));
  }
}
