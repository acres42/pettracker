package com.ac.pettracker.controller;

import com.ac.pettracker.dto.PetDto;
import com.ac.pettracker.dto.PetMapper;
import com.ac.pettracker.dto.PetSearchResult;
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
   * @param model the Spring MVC model
   * @param session the current HTTP session
   * @return the {@code search} view
   */
  @GetMapping("/search")
  public String search(Model model, HttpSession session) {
    profileService.populateSessionPreferences(SessionHelper.getUserId(session), session);

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
   * <p>Keywords from the form take precedence over the user's profile keywords; if the form
   * keywords are blank, the profile keywords are used. A blank effective-keywords value means no
   * keyword filtering is applied.
   *
   * @param type the pet species to search for
   * @param gender optional gender filter ({@code "male"} or {@code "female"}); blank means any
   * @param ageBand optional age range filter ({@code "young"}, {@code "adult"}, or {@code
   *     "senior"}); blank means any
   * @param keywords optional comma-separated keyword filter for pet descriptions; blank defers to
   *     profile keywords
   * @param model the Spring MVC model
   * @param session the current HTTP session
   * @return the {@code results} view
   */
  @GetMapping("/pets/results")
  public String results(
      @RequestParam(required = false) String type,
      @RequestParam(defaultValue = "") String gender,
      @RequestParam(defaultValue = "") String ageBand,
      @RequestParam(defaultValue = "") String keywords,
      Model model,
      HttpSession session) {
    if (type == null || type.isBlank()) {
      throw new IllegalArgumentException("Missing search parameters");
    }
    String effectiveKeywords =
        keywords.isBlank() ? SessionHelper.getSessionString(session, "profileKeywords") : keywords;
    logger.info(
        "Searching pets with type={} gender={} ageBand={} keywords={}",
        type,
        gender,
        ageBand,
        effectiveKeywords);
    PetSearchResult searchResult = petService.searchPets(type, gender, ageBand, effectiveKeywords);
    List<PetDto> pets = searchResult.pets().stream().map(PetMapper::toDto).toList();
    model.addAttribute("type", type);
    model.addAttribute("gender", gender);
    model.addAttribute("ageBand", ageBand);
    model.addAttribute("keywords", keywords);
    model.addAttribute("pets", pets);
    model.addAttribute("unmatchedKeywords", searchResult.unmatchedKeywords());
    model.addAttribute("savedPetKeys", SessionHelper.buildSavedPetKeys(session));
    return "results";
  }
}
