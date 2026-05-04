package com.ac.pettracker.controller;

import com.ac.pettracker.model.Pet;
import com.ac.pettracker.model.SavedPetEntry;
import com.ac.pettracker.model.SavedPetStatus;
import com.ac.pettracker.model.UserPreferences;
import com.ac.pettracker.repository.UserPreferencesRepository;
import com.ac.pettracker.service.AuthService;
import com.ac.pettracker.service.PetService;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** MVC controller handling all page routes, form submissions, and session-based pet tracking. */
@Controller
public class PageController {

  private final PetService petService;
  private final AuthService authService;
  private final UserPreferencesRepository userPreferencesRepository;
  private static final Logger logger = LoggerFactory.getLogger(PageController.class);

  /**
   * Constructs a {@code PageController} with its required dependencies.
   *
   * @param petService the pet search and suggestion service
   * @param authService the user authentication service
   * @param userPreferencesRepository the repository for persisting user preferences
   */
  public PageController(
      PetService petService,
      AuthService authService,
      UserPreferencesRepository userPreferencesRepository) {
    this.petService = petService;
    this.authService = authService;
    this.userPreferencesRepository = userPreferencesRepository;
  }

  /** Renders the home (landing) page. */
  @GetMapping("/")
  public String home() {
    return "index";
  }

  /** Renders the sign-up page. */
  @GetMapping("/signup")
  public String signup() {
    return "signup";
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
    loadPreferencesFromDb(session);
    model.addAttribute("query", query);

    Set<String> savedPetKeys = buildSavedPetKeys(session);
    // Pass empty savedPetKeys so all matching pets appear in the carousel (saved pets show
    // with a disabled button rather than being excluded from discovery).
    List<Pet> suggestedPets =
        petService.getSuggestedPets(
            getSessionString(session, "profileSpecies"),
            getSessionString(session, "profileGender"),
            getSessionString(session, "profileWeight"),
            getSessionString(session, "profileBreed"),
            getProfileKeywords(session),
            new HashSet<>());
    model.addAttribute("suggestedPets", suggestedPets);
    model.addAttribute("savedPetKeys", savedPetKeys);

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

    List<Pet> pets = petService.searchPets(type, location);

    model.addAttribute("type", type);
    model.addAttribute("location", location);
    model.addAttribute("pets", pets);
    model.addAttribute("savedPetKeys", buildSavedPetKeys(session));
    return "results";
  }

  /**
   * Renders the saved-pets dashboard with optional sorting.
   *
   * @param sort the field to sort by (default: {@code savedAt})
   * @param dir the sort direction — {@code asc} or {@code desc} (default: {@code desc})
   * @param model the Spring MVC model
   * @param session the current HTTP session
   * @return the {@code dashboard} view, or a redirect to {@code /}
   */
  @GetMapping("/dashboard")
  public String dashboard(
      @RequestParam(defaultValue = "savedAt") String sort,
      @RequestParam(defaultValue = "desc") String dir,
      Model model,
      HttpSession session) {
    if (!isAuthenticated(session)) {
      return "redirect:/";
    }
    List<SavedPetEntry> savedPets = getSavedPets(session);
    savedPets.sort(buildComparator(sort, dir));

    model.addAttribute("savedPets", savedPets);
    model.addAttribute("sort", sort);
    model.addAttribute("dir", dir);
    return "dashboard";
  }

  /**
   * Renders the user profile page pre-filled with session data.
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
    loadPreferencesFromDb(session);
    model.addAttribute("profileFirstName", getSessionString(session, "profileFirstName"));
    model.addAttribute("profileLastName", getSessionString(session, "profileLastName"));
    model.addAttribute("profileEmail", getSessionString(session, "authUserEmail"));
    model.addAttribute("profileSpecies", getSessionString(session, "profileSpecies"));
    model.addAttribute("profileGender", getSessionString(session, "profileGender"));
    model.addAttribute("profileWeight", getSessionString(session, "profileWeight"));
    model.addAttribute("profileBreed", getSessionString(session, "profileBreed"));
    model.addAttribute("profileKeywords", getProfileKeywords(session));
    return "profile";
  }

  /**
   * Handles user registration and, on success, establishes a session.
   *
   * @param email the new user's email address
   * @param password the new user's plain-text password
   * @param session the current HTTP session
   * @return a redirect to the search page on success, or {@code /?authError=register} on failure
   */
  @PostMapping("/auth/register")
  public String register(
      @RequestParam String email, @RequestParam String password, HttpSession session) {
    boolean registered = authService.register(email, password);
    if (!registered) {
      return "redirect:/?authError=register";
    }
    return establishSessionAndRedirect(email, password, session);
  }

  /**
   * Handles user login and establishes a session on success.
   *
   * @param email the user's email address
   * @param password the user's plain-text password
   * @param session the current HTTP session
   * @return a redirect to the search page on success, or home on failure
   */
  @PostMapping("/auth/login")
  public String login(
      @RequestParam String email, @RequestParam String password, HttpSession session) {
    return establishSessionAndRedirect(email, password, session);
  }

  /**
   * Handles profile preference updates submitted via the {@code /profile} form.
   *
   * @param species preferred pet species
   * @param gender preferred pet gender ({@code male}, {@code female}, or blank for any)
   * @param weight preferred weight range
   * @param breed preferred breed
   * @param keywords search keywords
   * @param session the current HTTP session
   * @return a redirect to {@code /profile}
   */
  @PostMapping("/profile")
  public String updateProfile(
      @RequestParam(defaultValue = "") String species,
      @RequestParam(defaultValue = "") String gender,
      @RequestParam(defaultValue = "") String weight,
      @RequestParam(defaultValue = "") String breed,
      @RequestParam(defaultValue = "") String keywords,
      HttpSession session) {
    return updateProfilePreferences(species, gender, weight, breed, keywords, session);
  }

  /**
   * Handles profile preference updates submitted via the {@code /profile/preferences} form.
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
    String normalizedBreed = normalizeTextField(breed);
    String normalizedKeywords = normalizeTextField(keywords);
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

  /**
   * Invalidates the current session and redirects to the home page.
   *
   * @param session the current HTTP session
   * @return a redirect to {@code /}
   */
  @GetMapping("/logout")
  public String logout(HttpSession session) {
    session.invalidate();
    return "redirect:/";
  }

  /**
   * Saves a pet to the user's dashboard if it has not already been saved.
   *
   * @param name pet name
   * @param type pet species
   * @param breed pet breed
   * @param age pet age in years
   * @param description pet description
   * @param imageUrl pet image URL
   * @param notes initial user notes
   * @param session the current HTTP session
   * @return a redirect to {@code /dashboard}
   */
  @PostMapping("/dashboard/save")
  public String savePetToDashboard(
      @RequestParam String name,
      @RequestParam String type,
      @RequestParam(defaultValue = "Unknown") String breed,
      @RequestParam(defaultValue = "0") int age,
      @RequestParam(defaultValue = "No description available.") String description,
      @RequestParam(defaultValue = "") String imageUrl,
      @RequestParam(defaultValue = "") String notes,
      HttpSession session) {
    if (!isAuthenticated(session)) {
      return "redirect:/";
    }
    List<SavedPetEntry> savedPets = getSavedPets(session);
    String petKey = buildPetKey(name, type, breed, age);
    boolean alreadySaved = savedPets.stream().anyMatch(entry -> buildPetKey(entry).equals(petKey));
    if (!alreadySaved) {
      savedPets.add(
          new SavedPetEntry(
              name,
              type,
              breed,
              age,
              description,
              imageUrl,
              getProfileKeywords(session),
              notes,
              SavedPetStatus.INTRODUCED,
              LocalDate.now()));
    }
    session.setAttribute("savedPets", savedPets);
    return "redirect:/dashboard";
  }

  /**
   * Updates the notes and status of a saved pet entry.
   *
   * @param id the UUID of the saved pet entry
   * @param notes updated notes text
   * @param status updated adoption status
   * @param session the current HTTP session
   * @return a redirect to {@code /dashboard}
   */
  @PostMapping("/dashboard/update")
  public String updateSavedPet(
      @RequestParam String id,
      @RequestParam(defaultValue = "") String notes,
      @RequestParam(defaultValue = "INTRODUCED") SavedPetStatus status,
      HttpSession session) {
    if (!isAuthenticated(session)) {
      return "redirect:/";
    }
    List<SavedPetEntry> savedPets = getSavedPets(session);
    for (SavedPetEntry entry : savedPets) {
      if (entry.getId().equals(id)) {
        entry.setNotes(notes);
        entry.setStatus(status);
      }
    }
    session.setAttribute("savedPets", savedPets);
    return "redirect:/dashboard";
  }

  /**
   * Removes a saved pet entry from the dashboard.
   *
   * @param id the UUID of the saved pet entry to remove
   * @param session the current HTTP session
   * @return a redirect to {@code /dashboard}
   */
  @PostMapping("/dashboard/delete")
  public String deleteSavedPet(@RequestParam String id, HttpSession session) {
    if (!isAuthenticated(session)) {
      return "redirect:/";
    }
    List<SavedPetEntry> savedPets = getSavedPets(session);
    savedPets.removeIf(entry -> entry.getId().equals(id));
    session.setAttribute("savedPets", savedPets);
    return "redirect:/dashboard";
  }

  @SuppressWarnings("unchecked")
  private List<SavedPetEntry> getSavedPets(HttpSession session) {
    Object value = session.getAttribute("savedPets");
    if (value instanceof List<?>) {
      return (List<SavedPetEntry>) value;
    }
    List<SavedPetEntry> savedPets = new ArrayList<>();
    session.setAttribute("savedPets", savedPets);
    return savedPets;
  }

  @SuppressWarnings("checkstyle:Indentation")
  private Comparator<SavedPetEntry> buildComparator(String sort, String dir) {
    Comparator<SavedPetEntry> comparator =
        switch (sort) {
          case "name" ->
              Comparator.comparing(SavedPetEntry::getName, String.CASE_INSENSITIVE_ORDER);
          case "type" ->
              Comparator.comparing(SavedPetEntry::getType, String.CASE_INSENSITIVE_ORDER);
          case "breed" ->
              Comparator.comparing(SavedPetEntry::getBreed, String.CASE_INSENSITIVE_ORDER);
          case "age" -> Comparator.comparingInt(SavedPetEntry::getAge);
          case "keywords" ->
              Comparator.comparing(
                  entry -> entry.getKeywords() == null ? "" : entry.getKeywords(),
                  String.CASE_INSENSITIVE_ORDER);
          case "status" ->
              Comparator.comparing(
                  entry -> entry.getStatus().name(), String.CASE_INSENSITIVE_ORDER);
          case "notes" ->
              Comparator.comparing(SavedPetEntry::getNotes, String.CASE_INSENSITIVE_ORDER);
          case "savedAt" -> Comparator.comparing(SavedPetEntry::getSavedAt);
          default -> Comparator.comparing(SavedPetEntry::getSavedAt);
        };

    if ("desc".equalsIgnoreCase(dir)) {
      return comparator.reversed();
    }
    return comparator;
  }

  private String getProfileKeywords(HttpSession session) {
    return getSessionString(session, "profileKeywords");
  }

  private String getSessionString(HttpSession session, String attributeName) {
    if (session == null) {
      return "";
    }
    Object value = session.getAttribute(attributeName);
    if (value instanceof String text) {
      return text;
    }
    return "";
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

  private Set<String> buildSavedPetKeys(HttpSession session) {
    Set<String> savedPetKeys = new HashSet<>();
    if (session == null) {
      return savedPetKeys;
    }
    for (SavedPetEntry entry : getSavedPets(session)) {
      savedPetKeys.add(buildPetKey(entry));
    }
    return savedPetKeys;
  }

  private String buildPetKey(SavedPetEntry entry) {
    return buildPetKey(entry.getName(), entry.getType(), entry.getBreed(), entry.getAge());
  }

  private String buildPetKey(String name, String type, String breed, int age) {
    return String.join("|", name, type, breed, Integer.toString(age));
  }

  private String establishSessionAndRedirect(String email, String password, HttpSession session) {
    return authService
        .authenticate(email, password)
        .map(
            user -> {
              session.setAttribute("authUserId", user.getId());
              session.setAttribute("authUserEmail", user.getEmail());
              return "redirect:/search";
            })
        .orElse("redirect:/?authError=login");
  }

  private boolean isAuthenticated(HttpSession session) {
    return session != null && session.getAttribute("authUserId") != null;
  }

  private void loadPreferencesFromDb(HttpSession session) {
    Object raw = session.getAttribute("authUserId");
    if (raw == null) {
      return;
    }
    Long userId = ((Number) raw).longValue();
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
    logger.info(
        "savePreferencesToDb: saving for userId={} species={} gender={} weight={} breed={}",
        userId,
        species,
        gender,
        weight,
        breed);
    UserPreferences prefs =
        userPreferencesRepository.findByUserAccountId(userId).orElse(new UserPreferences(userId));
    prefs.setPreferredSpecies(emptyToNull(species));
    prefs.setPreferredGender(emptyToNull(gender));
    prefs.setPreferredWeightBand(emptyToNull(weight));
    prefs.setPreferredBreed(emptyToNull(breed));
    prefs.setPreferredKeywords(emptyToNull(keywords));
    userPreferencesRepository.save(prefs);
    logger.info("savePreferencesToDb: saved prefs id={} for userId={}", prefs.getId(), userId);
  }

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
  }

  private String emptyToNull(String value) {
    return (value == null || value.isBlank()) ? null : value;
  }
}
