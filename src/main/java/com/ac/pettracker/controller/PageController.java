package com.ac.pettracker.controller;

import com.ac.pettracker.model.Pet;
import com.ac.pettracker.model.SavedPetEntry;
import com.ac.pettracker.model.SavedPetStatus;
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
  private static final Logger logger = LoggerFactory.getLogger(PageController.class);

  public PageController(PetService petService) {
    this.petService = petService;
  }

  @GetMapping("/")
  public String home() {
    return "index";
  }

  @GetMapping("/search")
  public String search(@RequestParam(name = "q", required = false) String query, Model model) {
    model.addAttribute("query", query);
    return "search";
  }

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
    List<SavedPetEntry> savedPets = getSavedPets(session);
    savedPets.sort(buildComparator(sort, dir));

    model.addAttribute("savedPets", savedPets);
    model.addAttribute("sort", sort);
    model.addAttribute("dir", dir);
    return "dashboard";
  }

  @GetMapping("/profile")
  public String profile(Model model, HttpSession session) {
    model.addAttribute("profileKeywords", getProfileKeywords(session));
    return "profile";
  }

  @PostMapping("/profile")
  public String updateProfileKeywords(
      @RequestParam(defaultValue = "") String keywords, HttpSession session) {
    session.setAttribute("profileKeywords", keywords == null ? "" : keywords.trim());
    return "redirect:/profile";
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
    Object value = session.getAttribute("profileKeywords");
    if (value instanceof String keywords) {
      return keywords;
    }
    return "";
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
}
