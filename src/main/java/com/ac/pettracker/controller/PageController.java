package com.ac.pettracker.controller;

import com.ac.pettracker.model.Pet;
import com.ac.pettracker.model.SavedPetEntry;
import com.ac.pettracker.model.SavedPetStatus;
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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {

  private final PetService petService;
  private final AuthService authService;
  private static final Logger logger = LoggerFactory.getLogger(PageController.class);

  public PageController(PetService petService, AuthService authService) {
    this.petService = petService;
    this.authService = authService;
  }

  @GetMapping("/")
  public String home() {
    return "index";
  }

  @GetMapping("/signup")
  public String signup() {
    return "signup";
  }

  @GetMapping("/search")
  public String search(
      @RequestParam(name = "q", required = false) String query, Model model, HttpSession session) {
    if (!isAuthenticated(session)) {
      return "redirect:/";
    }
    model.addAttribute("query", query);
    return "search";
  }

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

  @GetMapping("/profile")
  public String profile(Model model, HttpSession session) {
    if (!isAuthenticated(session)) {
      return "redirect:/";
    }
    model.addAttribute("profileFirstName", getSessionString(session, "profileFirstName"));
    model.addAttribute("profileLastName", getSessionString(session, "profileLastName"));
    model.addAttribute("profileEmail", getSessionString(session, "authUserEmail"));
    model.addAttribute("profileSpecies", getSessionString(session, "profileSpecies"));
    model.addAttribute("profileWeight", getSessionString(session, "profileWeight"));
    model.addAttribute("profileBreed", getSessionString(session, "profileBreed"));
    model.addAttribute("profileKeywords", getProfileKeywords(session));
    return "profile";
  }

  @PostMapping("/auth/register")
  public String register(
      @RequestParam String email, @RequestParam String password, HttpSession session) {
    boolean registered = authService.register(email, password);
    if (!registered) {
      return "redirect:/?authError=register";
    }
    return establishSessionAndRedirect(email, password, session);
  }

  @PostMapping("/auth/login")
  public String login(
      @RequestParam String email, @RequestParam String password, HttpSession session) {
    return establishSessionAndRedirect(email, password, session);
  }

  @PostMapping("/profile")
  public String updateProfile(
      @RequestParam(defaultValue = "") String species,
      @RequestParam(defaultValue = "") String weight,
      @RequestParam(defaultValue = "") String breed,
      @RequestParam(defaultValue = "") String keywords,
      HttpSession session) {
    return updateProfilePreferences(species, weight, breed, keywords, session);
  }

  @PostMapping("/profile/preferences")
  public String updateProfilePreferences(
      @RequestParam(defaultValue = "") String species,
      @RequestParam(defaultValue = "") String weight,
      @RequestParam(defaultValue = "") String breed,
      @RequestParam(defaultValue = "") String keywords,
      HttpSession session) {
    if (!isAuthenticated(session)) {
      return "redirect:/";
    }
    session.setAttribute("profileSpecies", normalizeSpecies(species));
    session.setAttribute("profileWeight", normalizeWeight(weight));
    session.setAttribute("profileBreed", normalizeTextField(breed));
    session.setAttribute("profileKeywords", normalizeTextField(keywords));
    return "redirect:/profile";
  }

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

  @GetMapping("/logout")
  public String logout(HttpSession session) {
    session.invalidate();
    return "redirect:/";
  }

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
}
