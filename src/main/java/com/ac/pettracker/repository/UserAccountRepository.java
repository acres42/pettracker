package com.ac.pettracker.repository;

import com.ac.pettracker.model.UserAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for user account persistence and lookup. */
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

  Optional<UserAccount> findByEmail(String email);

  boolean existsByEmail(String email);
}
