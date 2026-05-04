package com.ac.pettracker.controller;

import com.ac.pettracker.dto.PetDto;
import com.ac.pettracker.dto.PetMapper;
import com.ac.pettracker.repository.UserPreferencesRepository;
import com.ac.pettracker.service.PetService;
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
public class SearchController extends BaseController {

  private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

  private final PetService petService;
  private final UserPreferencesRepository userPreferencesRepository;

  public SearchController(
      PetService petService, UserPreferencesRepository userPreferencesRepository) {
    this.petService = petService;
    this.userPreferencesRepository = userPreferencesRepository;
  }

  /**
   * Renders the pet search form, redirecting to home if the user is not authenticated.
   *
   * @param query optional pre-filled search query
   * @param model the Spring MVC model
   * @param session the current HTTP session
   * @return the {@code search} view, or a redirect to {@code /}
   */
  @GetMapping("/search")
  public String search(
      @RequestParam(name = "q", required = false) String query, Model model, HttpSession session) {
    if (!isAuthenticated(session)) {
      return "redirect:/";
    }
    loadPreferencesFromDb(session, userPreferencesRepository);
    model.addAttribute("query", query);

    // Pass empty savedPetKeys so all matching pets appear in the carousel (saved pets show
    // with a disabled button rather than being excluded from discovery).
    List<PetDto> suggestedPets =
        petService
            .getSuggestedPets(
                getSessionString(session, "profileSpecies"),
                getSessionString(session, "profileGender"),
                getSessionString(session, "profileWeight"),
                getSessionString(session, "profileBreed"),
                getSessionString(session, "profileKeywords"),
                new HashSet<>())
            .stream()
            .map(PetMapper::toDto)
            .toList();
    model.addAttribute("suggestedPets", suggestedPets);
    model.addAttribute("savedPetKeys", buildSavedPetKeys(session));
    return "search";
  }

  /**
   * Performs a pet search and renders the results page.
   *
   * @param type the pet species to search for
   * @param location the location to search in
   * @param model the Spring MVC model
   * @param session the current HTTP session
   * @return the {@code results} view, or a redirect to {@code /}
   */
  @GetMapping("/pets/results")
  public String results(
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String location,
      Model model,
      HttpSession session) {
    if (!isAuthenticated(session)) {
      return "redirect:/";
    }
    if (type == null || type.isBlank() || location == null || location.isBlank()) {
      throw new IllegalArgumentException("Missing search parameters");
    }
    logger.info("Searching pets with type={} location={}", type, location);
    List<PetDto> pets =
        petService.searchPets(type, location).stream().map(PetMapper::toDto).toList();
    model.addAttribute("type", type);
    model.addAttribute("location", location);
    model.addAttribute("pets", pets);
    model.addAttribute("savedPetKeys", buildSavedPetKeys(session));
    return "results";
  }
}
