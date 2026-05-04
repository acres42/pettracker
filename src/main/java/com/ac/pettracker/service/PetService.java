package com.ac.pettracker.service;

import com.ac.pettracker.client.RescueGroupsClient;
import com.ac.pettracker.model.Pet;
import com.ac.pettracker.model.PetKeys;
import com.ac.pettracker.repository.PetRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
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
   * Returns pets that match the user's profile preferences, excluding already-saved pets.
   *
   * @param species the preferred species; empty string means no filter
   * @param gender the preferred gender ({@code "male"} or {@code "female"}); empty means no filter
   * @param weightBand the preferred weight range (e.g., {@code "<25 lbs"}); empty means no filter
   * @param breed a case-insensitive substring to match against the pet's breed; empty means no
   *     filter
   * @param keywords comma-separated terms to match against name, breed, or description; empty means
   *     no filter
   * @param savedPetKeys keys of already-saved pets in the format {@code name|type|breed|age}
   * @return list of matching, unsaved pets
   */
  public List<Pet> getSuggestedPets(
      String species,
      String gender,
      String weightBand,
      String breed,
      String keywords,
      Set<String> savedPetKeys) {
    List<Pet> pets =
        (species != null && !species.isBlank())
            ? petRepository.findByType(species)
            : petRepository.findAll();

    return pets.stream()
        .filter(pet -> matchesGender(pet, gender))
        .filter(pet -> matchesWeightBand(pet, weightBand))
        .filter(pet -> matchesBreed(pet, breed))
        .filter(pet -> matchesKeywords(pet, keywords))
        .filter(
            pet ->
                !savedPetKeys.contains(
                    PetKeys.of(
                        pet.getName(),
                        pet.getType(),
                        pet.getBreed(),
                        pet.getAge() != null ? pet.getAge() : 0)))
        .toList();
  }

  private boolean matchesGender(Pet pet, String gender) {
    if (gender == null || gender.isBlank()) {
      return true;
    }
    return gender.equalsIgnoreCase(pet.getGender());
  }

  private boolean matchesWeightBand(Pet pet, String weightBand) {
    if (weightBand == null || weightBand.isBlank()) {
      return true;
    }
    if (pet.getWeightLbs() == null) {
      return false;
    }
    int w = pet.getWeightLbs();
    return switch (weightBand) {
      case "<25 lbs" -> w < 25;
      case "25-50lbs" -> w >= 25 && w <= 50;
      case "60-100lbs" -> w >= 60 && w <= 100;
      case ">100lbs" -> w > 100;
      default -> true;
    };
  }

  private boolean matchesBreed(Pet pet, String breed) {
    if (breed == null || breed.isBlank()) {
      return true;
    }
    if (pet.getBreed() == null) {
      return false;
    }
    return pet.getBreed().toLowerCase().contains(breed.toLowerCase());
  }

  private boolean matchesKeywords(Pet pet, String keywords) {
    if (keywords == null || keywords.isBlank()) {
      return true;
    }
    String haystack =
        String.join(
                " ",
                pet.getName() != null ? pet.getName() : "",
                pet.getBreed() != null ? pet.getBreed() : "",
                pet.getDescription() != null ? pet.getDescription() : "")
            .toLowerCase();
    return Arrays.stream(keywords.split(","))
        .map(String::trim)
        .filter(kw -> !kw.isBlank())
        .anyMatch(kw -> haystack.contains(kw.toLowerCase()));
  }
}
