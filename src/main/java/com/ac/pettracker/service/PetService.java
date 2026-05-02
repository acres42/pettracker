package com.ac.pettracker.service;

import com.ac.pettracker.model.Pet;
import com.ac.pettracker.repository.PetRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PetService {

  private final PetRepository petRepository;

  public PetService(PetRepository petRepository) {
    this.petRepository = petRepository;
  }

  public List<Pet> searchPets(String type, String location) {
    return petRepository.findByType(type);
  }
}
