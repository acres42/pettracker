package com.ac.pettracker.controller;

import com.ac.pettracker.model.PetKeys;
import com.ac.pettracker.model.SavedPetEntry;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Package-private utility class providing stateless session helpers shared across MVC controllers.
 *
 * <p>Using a utility class (static methods, no inheritance) rather than a base class keeps
 * controller hierarchies flat and makes dependencies explicit.
 */
final class SessionHelper {

  private SessionHelper() {}

  /**
   * Returns the {@code Long} user ID stored in the session under {@code "authUserId"}, casting
   * safely from any {@link Number} subtype.
   *
   * @param session the current HTTP session
   * @return the authenticated user's ID
   * @throws IllegalStateException if no user ID is present (should not happen on authenticated
   *     routes guarded by Spring Security)
   */
  static Long getUserId(HttpSession session) {
    Object raw = session.getAttribute("authUserId");
    if (raw == null) {
      throw new IllegalStateException("authUserId missing from session on authenticated route");
    }
    return ((Number) raw).longValue();
  }

  /**
   * Returns the session attribute with the given name as a {@link String}, or an empty string if
   * the attribute is absent or not a {@code String}.
   *
   * @param session the current HTTP session
   * @param attributeName the session attribute key
   * @return the attribute value, or {@code ""} if absent
   */
  static String getSessionString(HttpSession session, String attributeName) {
    if (session == null) {
      return "";
    }
    Object value = session.getAttribute(attributeName);
    return (value instanceof String text) ? text : "";
  }

  /**
   * Returns the list of saved pet entries stored in the session, creating an empty list if none
   * exists.
   *
   * @param session the current HTTP session
   * @return the mutable saved-pet list (never {@code null})
   */
  @SuppressWarnings("unchecked")
  static List<SavedPetEntry> getSavedPets(HttpSession session) {
    Object value = session.getAttribute("savedPets");
    if (value instanceof List<?>) {
      return (List<SavedPetEntry>) value;
    }
    List<SavedPetEntry> savedPets = new ArrayList<>();
    session.setAttribute("savedPets", savedPets);
    return savedPets;
  }

  /**
   * Builds the set of composite keys ({@code name|type|breed|age}) for all pets currently saved in
   * the session.
   *
   * @param session the current HTTP session; returns an empty set if {@code null}
   * @return a mutable set of saved-pet key strings
   */
  static Set<String> buildSavedPetKeys(HttpSession session) {
    if (session == null) {
      return new HashSet<>();
    }
    return getSavedPets(session).stream()
        .map(e -> PetKeys.of(e.getName(), e.getType(), e.getBreed(), e.getAge()))
        .collect(Collectors.toCollection(HashSet::new));
  }
}
