package com.ac.pettracker.service;

import com.ac.pettracker.model.Pet;
import com.ac.pettracker.repository.PetRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PetService {

  private final PetRepository petRepository;
  private static final Logger logger = LoggerFactory.getLogger(PetService.class);

  public PetService(PetRepository petRepository) {
    this.petRepository = petRepository;
  }

  public List<Pet> searchPets(String type, String location) {
    logger.info("Filtering pets by type={} location={}", type, location);
    return petRepository.findByType(type);
  }

  public List<Pet> searchPets(String type, String location, String sort) {
    List<Pet> results = searchPets(type, location);
    logger.info("Sorting pets by sort={}", sort);
    if ("name".equalsIgnoreCase(sort)) {
      return results.stream()
          .sorted((first, second) -> first.getName().compareToIgnoreCase(second.getName()))
          .toList();
    }

    if ("type".equalsIgnoreCase(sort)) {
      return results.stream()
          .sorted((first, second) -> first.getType().compareToIgnoreCase(second.getType()))
          .toList();
    }

    return results;
  }

  public List<Pet> searchPets(String type, String location, String sort, int page, int size) {
    logger.info("Paginating pets with page={} size={}", page, size);
    List<Pet> results = searchPets(type, location, sort);

    int start = page * size;
    int end = Math.min(start + size, results.size());

    if (start >= results.size()) {
      return List.of();
    }

    return results.subList(start, end);
  }
}
