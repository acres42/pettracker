package com.ac.pettracker.service;

import com.ac.pettracker.client.RescueGroupsClient;
import com.ac.pettracker.dto.PetSearchResult;
import com.ac.pettracker.model.Pet;
import com.ac.pettracker.model.PetKeys;
import com.ac.pettracker.repository.PetRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
   * Searches for pets by type, falling back to the local repository if the API returns no results.
   *
   * @param type the pet species (e.g., "dog", "cat")
   * @return non-null list of matching pets
   */
  public List<Pet> searchPets(String type) {
    logger.info("Filtering pets by type={}", type);
    List<Pet> apiPets = rescueGroupsClient.fetchPets(type);
    if (!apiPets.isEmpty()) {
      return apiPets;
    }

    logger.info("Falling back to local repository data for type={}", type);
    return petRepository.findByType(type);
  }

  /**
   * Searches for pets by type, then filters by gender and age band.
   *
   * <p>Falls back to the local repository if the API returns no results. Empty strings for {@code
   * gender} and {@code ageBand} disable their respective filters.
   *
   * @param type the pet species (e.g., "dog", "cat")
   * @param gender the pet gender ({@code "male"} or {@code "female"}); blank means no filter
   * @param ageBand age range: {@code "young"} (0–2), {@code "adult"} (3–7), {@code "senior"} (8+);
   *     blank means no filter
   * @return non-null filtered list of matching pets
   */
  public List<Pet> searchPets(String type, String gender, String ageBand) {
    List<Pet> results = searchPets(type);
    return results.stream()
        .filter(pet -> matchesGender(pet, gender))
        .filter(pet -> matchesAgeBand(pet, ageBand))
        .toList();
  }

  /**
   * Searches for pets by type, location, gender, and age band, then filters by keywords against
   * each pet's description.
   *
   * <p>Keywords are sourced from {@code keywords}: a comma-separated string whose terms are
   * trimmed, deduplicated, and capped at <strong>5</strong> before matching. A pet is included if
   * at least one keyword appears (case-insensitive) in its description. When {@code keywords} is
   * blank, all candidates are returned and no keywords are reported as unmatched.
   *
   * <p>The filtering and unmatched-keyword detection are done in a single O(n) pass over the
   * candidate list.
   *
   * @param type the pet species (e.g., "dog", "cat")
   * @param gender the pet gender ({@code "male"} or {@code "female"}); blank means no filter
   * @param ageBand age range ({@code "young"}, {@code "adult"}, {@code "senior"}); blank means no
   *     filter
   * @param keywords comma-separated search terms; blank means return all candidates
   * @return a {@link PetSearchResult} containing matched pets and any unmatched keywords
   */
  public PetSearchResult searchPets(String type, String gender, String ageBand, String keywords) {
    List<Pet> candidates = searchPets(type, gender, ageBand);
    List<String> keywordList = parseKeywords(keywords);

    if (keywordList.isEmpty()) {
      return new PetSearchResult(candidates, List.of());
    }

    // Single O(n × k) pass: filter pets and record which keywords produced at least one match
    Set<String> matchedKeywords = new HashSet<>();
    List<Pet> matched = new ArrayList<>();
    for (Pet pet : candidates) {
      String descLower = pet.getDescription() != null ? pet.getDescription().toLowerCase() : "";
      boolean petMatched = false;
      for (String kw : keywordList) {
        if (descLower.contains(kw.toLowerCase())) {
          matchedKeywords.add(kw);
          petMatched = true;
        }
      }
      if (petMatched) {
        matched.add(pet);
      }
    }

    List<String> unmatched =
        keywordList.stream().filter(kw -> !matchedKeywords.contains(kw)).toList();
    return new PetSearchResult(matched, unmatched);
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

  /**
   * Parses a raw comma-separated keyword string into a trimmed, deduplicated list capped at 5
   * terms. Returns an empty list when {@code raw} is blank.
   */
  private static List<String> parseKeywords(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of();
    }
    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(kw -> !kw.isBlank())
        .distinct()
        .limit(5)
        .toList();
  }

  private boolean matchesGender(Pet pet, String gender) {
    if (gender == null || gender.isBlank()) {
      return true;
    }
    return gender.equalsIgnoreCase(pet.getGender());
  }

  private boolean matchesAgeBand(Pet pet, String ageBand) {
    if (ageBand == null || ageBand.isBlank()) {
      return true;
    }
    if (pet.getAge() == null) {
      return false;
    }
    int age = pet.getAge();
    return switch (ageBand) {
      case "young" -> age <= 2;
      case "adult" -> age >= 3 && age <= 7;
      case "senior" -> age >= 8;
      default -> true;
    };
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
