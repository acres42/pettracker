package com.ac.pettracker.controller;

import com.ac.pettracker.model.UserPreferences;
import com.ac.pettracker.repository.UserPreferencesRepository;
import com.ac.pettracker.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** MVC controller handling user profile viewing and editing. */
@Controller
public class ProfileController extends BaseController {

  private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

  private final AuthService authService;
  private final UserPreferencesRepository userPreferencesRepository;

  public ProfileController(
      AuthService authService, UserPreferencesRepository userPreferencesRepository) {
    this.authService = authService;
    this.userPreferencesRepository = userPreferencesRepository;
  }

  /**
   * Renders the user profile page pre-filled with session and database preferences.
   *
   * @param model the Spring MVC model
   * @param session the current HTTP session
   * @return the {@code profile} view, or a redirect to {@code /}
   */
  @GetMapping("/profile")
  public String profile(Model model, HttpSession session) {
    if (!isAuthenticated(session)) {
      return "redirect:/";
    }
    loadPreferencesFromDb(session, userPreferencesRepository);
    model.addAttribute("profileFirstName", getSessionString(session, "profileFirstName"));
    model.addAttribute("profileLastName", getSessionString(session, "profileLastName"));
    model.addAttribute("profileEmail", getSessionString(session, "authUserEmail"));
    model.addAttribute("profileSpecies", getSessionString(session, "profileSpecies"));
    model.addAttribute("profileGender", getSessionString(session, "profileGender"));
    model.addAttribute("profileWeight", getSessionString(session, "profileWeight"));
    model.addAttribute("profileBreed", getSessionString(session, "profileBreed"));
    model.addAttribute("profileKeywords", getSessionString(session, "profileKeywords"));
    return "profile";
  }

  /**
   * Handles profile preference updates.
   *
   * @param species preferred pet species
   * @param gender preferred pet gender ({@code male}, {@code female}, or blank for any)
   * @param weight preferred weight range
   * @param breed preferred breed
   * @param keywords search keywords
   * @param session the current HTTP session
   * @return a redirect to {@code /profile}, or {@code /} if not authenticated
   */
  @Transactional
  @PostMapping("/profile/preferences")
  public String updateProfilePreferences(
      @RequestParam(defaultValue = "") String species,
      @RequestParam(defaultValue = "") String gender,
      @RequestParam(defaultValue = "") String weight,
      @RequestParam(defaultValue = "") String breed,
      @RequestParam(defaultValue = "") String keywords,
      HttpSession session) {
    if (!isAuthenticated(session)) {
      return "redirect:/";
    }
    String normalizedSpecies = normalizeSpecies(species);
    String normalizedGender = normalizeGender(gender);
    String normalizedWeight = normalizeWeight(weight);
    String normalizedBreed = truncate(normalizeTextField(breed), 120);
    String normalizedKeywords = truncate(normalizeTextField(keywords), 500);
    session.setAttribute("profileSpecies", normalizedSpecies);
    session.setAttribute("profileGender", normalizedGender);
    session.setAttribute("profileWeight", normalizedWeight);
    session.setAttribute("profileBreed", normalizedBreed);
    session.setAttribute("profileKeywords", normalizedKeywords);
    savePreferencesToDb(
        session,
        normalizedSpecies,
        normalizedGender,
        normalizedWeight,
        normalizedBreed,
        normalizedKeywords);
    return "redirect:/profile";
  }

  /**
   * Handles password change requests.
   *
   * @param currentPassword the user's current plain-text password
   * @param newPassword the desired new password
   * @param confirmPassword must match {@code newPassword}
   * @param session the current HTTP session
   * @return a redirect to {@code /profile?passwordUpdated=1} on success, or an error redirect
   */
  @PostMapping("/profile/password")
  public String updatePassword(
      @RequestParam(defaultValue = "") String currentPassword,
      @RequestParam(defaultValue = "") String newPassword,
      @RequestParam(defaultValue = "") String confirmPassword,
      HttpSession session) {
    if (!isAuthenticated(session)) {
      return "redirect:/";
    }
    if (!newPassword.equals(confirmPassword)) {
      return "redirect:/profile?passwordError=mismatch";
    }
    String email = getSessionString(session, "authUserEmail");
    boolean updated = authService.updatePassword(email, currentPassword, newPassword);
    if (!updated) {
      return "redirect:/profile?passwordError=invalid";
    }
    return "redirect:/profile?passwordUpdated=1";
  }

  private void savePreferencesToDb(
      HttpSession session,
      String species,
      String gender,
      String weight,
      String breed,
      String keywords) {
    Object raw = session.getAttribute("authUserId");
    if (raw == null) {
      logger.warn("savePreferencesToDb: authUserId not in session, skipping save");
      return;
    }
    Long userId = ((Number) raw).longValue();
    logger.debug("savePreferencesToDb: saving preferences for user");
    UserPreferences prefs =
        userPreferencesRepository.findByUserAccountId(userId).orElse(new UserPreferences(userId));
    prefs.setPreferredSpecies(emptyToNull(species));
    prefs.setPreferredGender(emptyToNull(gender));
    prefs.setPreferredWeightBand(emptyToNull(weight));
    prefs.setPreferredBreed(emptyToNull(breed));
    prefs.setPreferredKeywords(emptyToNull(keywords));
    userPreferencesRepository.save(prefs);
    logger.debug("savePreferencesToDb: preferences saved successfully");
  }

  private String normalizeTextField(String value) {
    if (value == null) {
      return "";
    }
    return value.trim();
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

  private String emptyToNull(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }

  private String truncate(String value, int maxLen) {
    if (value == null || value.length() <= maxLen) {
      return value;
    }
    return value.substring(0, maxLen);
  }
}
