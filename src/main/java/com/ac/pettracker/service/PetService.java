package com.ac.pettracker.service;

import com.ac.pettracker.integration.RescueGroupsClient;
import com.ac.pettracker.model.Pet;
import com.ac.pettracker.repository.PetRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service that orchestrates pet search by trying the RescueGroups API and falling back to local
 * data.
 */
@Service
public class PetService {

  private final RescueGroupsClient rescueGroupsClient;
  private final PetRepository petRepository;
  private static final Logger logger = LoggerFactory.getLogger(PetService.class);

  public PetService(RescueGroupsClient rescueGroupsClient, PetRepository petRepository) {
    this.rescueGroupsClient = rescueGroupsClient;
    this.petRepository = petRepository;
  }

  /**
   * Searches for pets by type and location, falling back to the local repository if the API returns
   * no results.
   *
   * @param type the pet species (e.g., "dog", "cat")
   * @param location the search location string
   * @return non-null list of matching pets
   */
  public List<Pet> searchPets(String type, String location) {
    logger.info("Filtering pets by type={} location={}", type, location);
    List<Pet> apiPets = rescueGroupsClient.fetchPets(type, location);
    if (!apiPets.isEmpty()) {
      return apiPets;
    }

    logger.info("Falling back to local repository data for type={}", type);
    return petRepository.findByType(type);
  }

  /**
   * Searches for pets and sorts the results by the specified field.
   *
   * @param type the pet species
   * @param location the search location string
   * @param sort the field to sort by: {@code "name"} or {@code "type"}; any other value preserves
   *     the original order
   * @return sorted, non-null list of matching pets
   */
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

  /**
   * Searches, sorts, and paginates pets.
   *
   * @param type the pet species
   * @param location the search location string
   * @param sort the sort field
   * @param page zero-based page index
   * @param size number of results per page
   * @return a sub-list of pets for the requested page; empty if {@code page} is out of range
   */
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
