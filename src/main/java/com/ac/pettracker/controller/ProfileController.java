package com.ac.pettracker.controller;

import com.ac.pettracker.dto.ProfilePreferences;
import com.ac.pettracker.service.AuthService;
import com.ac.pettracker.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** MVC controller handling user profile viewing and editing. */
@Controller
public class ProfileController {

  private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

  private final AuthService authService;
  private final ProfileService profileService;

  /** Creates a {@code ProfileController} with the given services. */
  public ProfileController(AuthService authService, ProfileService profileService) {
    this.authService = authService;
    this.profileService = profileService;
  }

  /**
   * Renders the user profile page pre-filled with session and database preferences.
   *
   * @param model the Spring MVC model
   * @param session the current HTTP session
   * @return the {@code profile} view
   */
  @GetMapping("/profile")
  public String profile(Model model, HttpSession session) {
    Long userId = SessionHelper.getUserId(session);
    profileService.populateSessionPreferences(userId, session);
    model.addAttribute(
        "profileFirstName", SessionHelper.getSessionString(session, "profileFirstName"));
    model.addAttribute(
        "profileLastName", SessionHelper.getSessionString(session, "profileLastName"));
    model.addAttribute("profileEmail", SessionHelper.getSessionString(session, "authUserEmail"));
    model.addAttribute("profileSpecies", SessionHelper.getSessionString(session, "profileSpecies"));
    model.addAttribute("profileGender", SessionHelper.getSessionString(session, "profileGender"));
    model.addAttribute("profileWeight", SessionHelper.getSessionString(session, "profileWeight"));
    model.addAttribute("profileBreed", SessionHelper.getSessionString(session, "profileBreed"));
    model.addAttribute(
        "profileKeywords", SessionHelper.getSessionString(session, "profileKeywords"));
    return "profile";
  }

  /**
   * Handles profile preference updates. Normalization is delegated to {@link ProfileService}; the
   * returned {@link ProfilePreferences} is written to the session so subsequent page loads reflect
   * the change without an extra database round-trip.
   *
   * @param species preferred pet species
   * @param gender preferred pet gender ({@code male}, {@code female}, or blank for any)
   * @param weight preferred weight range
   * @param breed preferred breed
   * @param keywords search keywords
   * @param session the current HTTP session
   * @return a redirect to {@code /profile}
   */
  @PostMapping("/profile/preferences")
  public String updateProfilePreferences(
      @RequestParam(defaultValue = "") String species,
      @RequestParam(defaultValue = "") String gender,
      @RequestParam(defaultValue = "") String weight,
      @RequestParam(defaultValue = "") String breed,
      @RequestParam(defaultValue = "") String keywords,
      HttpSession session) {
    Long userId = SessionHelper.getUserId(session);
    ProfilePreferences prefs =
        profileService.savePreferences(userId, species, gender, weight, breed, keywords);
    session.setAttribute("profileSpecies", prefs.species());
    session.setAttribute("profileGender", prefs.gender());
    session.setAttribute("profileWeight", prefs.weight());
    session.setAttribute("profileBreed", prefs.breed());
    session.setAttribute("profileKeywords", prefs.keywords());
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
    if (!newPassword.equals(confirmPassword)) {
      return "redirect:/profile?passwordError=mismatch";
    }
    String email = SessionHelper.getSessionString(session, "authUserEmail");
    logger.info("Password update requested");
    if (!authService.updatePassword(email, currentPassword, newPassword)) {
      return "redirect:/profile?passwordError=invalid";
    }
    return "redirect:/profile?passwordUpdated=1";
  }
}
