package com.ac.pettracker.repository;

import com.ac.pettracker.model.UserPreferences;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for user preference persistence and lookup. */
public interface UserPreferencesRepository extends JpaRepository<UserPreferences, Long> {

  /**
   * Finds the preferences record for the given user account, if one exists.
   *
   * @param userAccountId the owning user account ID
   * @return an {@link Optional} containing the preferences, or empty if none saved
   */
  Optional<UserPreferences> findByUserAccountId(Long userAccountId);
}
