package com.ac.pettracker.controller;

import com.ac.pettracker.model.PetKeys;
import com.ac.pettracker.model.SavedPetEntry;
import com.ac.pettracker.model.SavedPetStatus;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** MVC controller handling the saved-pets dashboard: viewing, saving, updating, and deleting. */
@Controller
public class DashboardController {

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
    List<SavedPetEntry> savedPets = SessionHelper.getSavedPets(session);
    savedPets.sort(buildComparator(sort, dir));
    model.addAttribute("savedPets", savedPets);
    model.addAttribute("sort", sort);
    model.addAttribute("dir", dir);
    return "dashboard";
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
    List<SavedPetEntry> savedPets = SessionHelper.getSavedPets(session);
    String petKey = PetKeys.of(name, type, breed, age);
    boolean alreadySaved =
        savedPets.stream()
            .anyMatch(
                e -> PetKeys.of(e.getName(), e.getType(), e.getBreed(), e.getAge()).equals(petKey));
    if (!alreadySaved) {
      savedPets.add(
          new SavedPetEntry(
              name,
              type,
              breed,
              age,
              description,
              imageUrl,
              SessionHelper.getSessionString(session, "profileKeywords"),
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
    List<SavedPetEntry> savedPets = SessionHelper.getSavedPets(session);
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
    List<SavedPetEntry> savedPets = SessionHelper.getSavedPets(session);
    savedPets.removeIf(entry -> entry.getId().equals(id));
    session.setAttribute("savedPets", savedPets);
    return "redirect:/dashboard";
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
          default -> Comparator.comparing(SavedPetEntry::getSavedAt);
        };
    if ("desc".equalsIgnoreCase(dir)) {
      return comparator.reversed();
    }
    return comparator;
  }
}
