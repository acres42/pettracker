package com.ac.pettracker.controller;

import com.ac.pettracker.model.PetKeys;
import com.ac.pettracker.model.SavedPetEntry;
import com.ac.pettracker.repository.UserPreferencesRepository;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Abstract base controller providing shared session and preference utilities. */
abstract class BaseController {

  protected boolean isAuthenticated(HttpSession session) {
    return session != null && session.getAttribute("authUserId") != null;
  }

  protected String getSessionString(HttpSession session, String attributeName) {
    if (session == null) {
      return "";
    }
    Object value = session.getAttribute(attributeName);
    if (value instanceof String text) {
      return text;
    }
    return "";
  }

  @SuppressWarnings("unchecked")
  protected List<SavedPetEntry> getSavedPets(HttpSession session) {
    Object value = session.getAttribute("savedPets");
    if (value instanceof List<?>) {
      return (List<SavedPetEntry>) value;
    }
    List<SavedPetEntry> savedPets = new ArrayList<>();
    session.setAttribute("savedPets", savedPets);
    return savedPets;
  }

  protected Set<String> buildSavedPetKeys(HttpSession session) {
    if (session == null) {
      return new HashSet<>();
    }
    return getSavedPets(session).stream()
        .map(e -> PetKeys.of(e.getName(), e.getType(), e.getBreed(), e.getAge()))
        .collect(Collectors.toCollection(HashSet::new));
  }

  protected void loadPreferencesFromDb(HttpSession session, UserPreferencesRepository repo) {
    Object raw = session.getAttribute("authUserId");
    if (raw == null) {
      return;
    }
    Long userId = ((Number) raw).longValue();
    repo.findByUserAccountId(userId)
        .ifPresent(
            prefs -> {
              session.setAttribute("profileSpecies", nullToEmpty(prefs.getPreferredSpecies()));
              session.setAttribute("profileGender", nullToEmpty(prefs.getPreferredGender()));
              session.setAttribute("profileWeight", nullToEmpty(prefs.getPreferredWeightBand()));
              session.setAttribute("profileBreed", nullToEmpty(prefs.getPreferredBreed()));
              session.setAttribute("profileKeywords", nullToEmpty(prefs.getPreferredKeywords()));
            });
  }

  protected static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
