package com.ac.pettracker.controller;

import com.ac.pettracker.dto.PetDto;
import com.ac.pettracker.dto.PetMapper;
import com.ac.pettracker.service.PetService;
import com.ac.pettracker.service.ProfileService;
import jakarta.servlet.http.HttpSession;
import java.util.HashSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** MVC controller handling pet search and results. */
@Controller
public class SearchController {

  private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

  private final PetService petService;
  private final ProfileService profileService;

  /** Creates a {@code SearchController} with the given services. */
  public SearchController(PetService petService, ProfileService profileService) {
    this.petService = petService;
    this.profileService = profileService;
  }

  /**
   * Renders the pet search form.
   *
   * @param query optional pre-filled search query
   * @param model the Spring MVC model
   * @param session the current HTTP session
   * @return the {@code search} view
   */
  @GetMapping("/search")
  public String search(
      @RequestParam(name = "q", required = false) String query, Model model, HttpSession session) {
    profileService.populateSessionPreferences(SessionHelper.getUserId(session), session);
    model.addAttribute("query", query);

    // Pass empty savedPetKeys so all matching pets appear in the carousel (saved pets show
    // with a disabled button rather than being excluded from discovery).
    List<PetDto> suggestedPets =
        petService
            .getSuggestedPets(
                SessionHelper.getSessionString(session, "profileSpecies"),
                SessionHelper.getSessionString(session, "profileGender"),
                SessionHelper.getSessionString(session, "profileWeight"),
                SessionHelper.getSessionString(session, "profileBreed"),
                SessionHelper.getSessionString(session, "profileKeywords"),
                new HashSet<>())
            .stream()
            .map(PetMapper::toDto)
            .toList();
    model.addAttribute("suggestedPets", suggestedPets);
    model.addAttribute("savedPetKeys", SessionHelper.buildSavedPetKeys(session));
    return "search";
  }

  /**
   * Performs a pet search and renders the results page.
   *
   * @param type the pet species to search for
   * @param location the location to search in
   * @param model the Spring MVC model
   * @param session the current HTTP session
   * @return the {@code results} view
   */
  @GetMapping("/pets/results")
  public String results(
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String location,
      Model model,
      HttpSession session) {
    if (type == null || type.isBlank() || location == null || location.isBlank()) {
      throw new IllegalArgumentException("Missing search parameters");
    }
    logger.info("Searching pets with type={} location={}", type, location);
    List<PetDto> pets =
        petService.searchPets(type, location).stream().map(PetMapper::toDto).toList();
    model.addAttribute("type", type);
    model.addAttribute("location", location);
    model.addAttribute("pets", pets);
    model.addAttribute("savedPetKeys", SessionHelper.buildSavedPetKeys(session));
    return "results";
  }
}
