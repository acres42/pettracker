package com.ac.pettracker.service;

import com.ac.pettracker.dto.ProfilePreferences;
import com.ac.pettracker.model.UserPreferences;
import com.ac.pettracker.repository.UserPreferencesRepository;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for reading and persisting user profile preferences.
 *
 * <p>Input normalization (whitespace trimming, allowed-value validation, truncation) lives here so
 * the controller remains a thin HTTP adapter.
 */
@Service
public class ProfileService {

  private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

  private final UserPreferencesRepository userPreferencesRepository;

  /** Creates a {@code ProfileService} with the given preferences repository. */
  public ProfileService(UserPreferencesRepository userPreferencesRepository) {
    this.userPreferencesRepository = userPreferencesRepository;
  }

  /**
   * Normalizes, persists, and returns the user's updated pet preferences.
   *
   * @param userId the authenticated user's database ID
   * @param species raw species input (e.g. {@code "Dog"}, {@code "CAT"}, or blank)
   * @param gender raw gender input ({@code "male"}, {@code "female"}, or blank)
   * @param weight raw weight-band input (e.g. {@code "25-50lbs"})
   * @param breed raw breed text; trimmed and capped at 120 characters
   * @param keywords raw keyword text; trimmed and capped at 500 characters
   * @return a {@link ProfilePreferences} record containing the normalized values that were saved
   */
  @Transactional
  public ProfilePreferences savePreferences(
      Long userId, String species, String gender, String weight, String breed, String keywords) {
    String normalizedSpecies = normalizeSpecies(species);
    String normalizedGender = normalizeGender(gender);
    String normalizedWeight = normalizeWeight(weight);
    String normalizedBreed = truncate(normalizeTextField(breed), 120);
    String normalizedKeywords = truncate(normalizeTextField(keywords), 500);

    logger.debug("Saving preferences for user");
    UserPreferences prefs =
        userPreferencesRepository.findByUserAccountId(userId).orElse(new UserPreferences(userId));
    prefs.setPreferredSpecies(emptyToNull(normalizedSpecies));
    prefs.setPreferredGender(emptyToNull(normalizedGender));
    prefs.setPreferredWeightBand(emptyToNull(normalizedWeight));
    prefs.setPreferredBreed(emptyToNull(normalizedBreed));
    prefs.setPreferredKeywords(emptyToNull(normalizedKeywords));
    userPreferencesRepository.save(prefs);
    logger.debug("Preferences saved successfully");

    return new ProfilePreferences(
        normalizedSpecies, normalizedGender, normalizedWeight, normalizedBreed, normalizedKeywords);
  }

  /**
   * Loads the user's persisted preferences from the database into the HTTP session. Has no effect
   * if no preferences exist for the given user.
   *
   * @param userId the authenticated user's database ID
   * @param session the current HTTP session to populate
   */
  public void populateSessionPreferences(Long userId, HttpSession session) {
    userPreferencesRepository
        .findByUserAccountId(userId)
        .ifPresent(
            prefs -> {
              session.setAttribute("profileSpecies", nullToEmpty(prefs.getPreferredSpecies()));
              session.setAttribute("profileGender", nullToEmpty(prefs.getPreferredGender()));
              session.setAttribute("profileWeight", nullToEmpty(prefs.getPreferredWeightBand()));
              session.setAttribute("profileBreed", nullToEmpty(prefs.getPreferredBreed()));
              session.setAttribute("profileKeywords", nullToEmpty(prefs.getPreferredKeywords()));
            });
  }

  // -------------------------------------------------------------------------
  // Normalization helpers
  // -------------------------------------------------------------------------

  private String normalizeTextField(String value) {
    return value == null ? "" : value.trim();
  }

  private String normalizeSpecies(String species) {
    String normalized = normalizeTextField(species).toLowerCase();
    return switch (normalized) {
      case "dog", "cat", "bird", "bunny", "lizard" -> normalized;
      default -> "";
    };
  }

  private String normalizeGender(String gender) {
    String normalized = normalizeTextField(gender).toLowerCase();
    return switch (normalized) {
      case "male", "female" -> normalized;
      default -> "";
    };
  }

  private String normalizeWeight(String weight) {
    String normalized = normalizeTextField(weight);
    return switch (normalized) {
      case "<25 lbs", "25-50lbs", "60-100lbs", ">100lbs" -> normalized;
      default -> "";
    };
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value == null ? "" : value;
    }
    return value.substring(0, maxLength);
  }

  private String emptyToNull(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
