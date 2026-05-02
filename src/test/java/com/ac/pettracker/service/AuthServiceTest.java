package com.ac.pettracker.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ac.pettracker.model.UserAccount;
import com.ac.pettracker.repository.UserAccountRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AuthServiceTest {

  @Autowired private AuthService authService;

  @Autowired private UserAccountRepository userAccountRepository;

  @BeforeEach
  void setup() {
    userAccountRepository.deleteAll();
  }

  @Test
  void registerStoresHashedPasswordNotPlaintext() {
    boolean registered = authService.register("user@example.com", "password123");

    assertThat(registered).isTrue();
    Optional<UserAccount> saved = userAccountRepository.findByEmail("user@example.com");
    assertThat(saved).isPresent();
    assertThat(saved.get().getPasswordHash()).isNotEqualTo("password123");
    assertThat(saved.get().getPasswordHash()).startsWith("$2");
  }

  @Test
  void authenticateSucceedsWithCorrectPassword() {
    authService.register("user@example.com", "password123");

    Optional<UserAccount> authResult = authService.authenticate("user@example.com", "password123");

    assertThat(authResult).isPresent();
    assertThat(authResult.get().getEmail()).isEqualTo("user@example.com");
  }

  @Test
  void authenticateFailsWithWrongPassword() {
    authService.register("user@example.com", "password123");

    Optional<UserAccount> authResult =
        authService.authenticate("user@example.com", "wrong-password");

    assertThat(authResult).isEmpty();
  }

  @Test
  void registerRejectsDuplicateEmail() {
    boolean first = authService.register("user@example.com", "password123");
    boolean second = authService.register("user@example.com", "another123");

    assertThat(first).isTrue();
    assertThat(second).isFalse();
    assertThat(userAccountRepository.count()).isEqualTo(1);
  }

  @Test
  void updatePasswordSucceedsWhenCurrentPasswordMatches() {
    authService.register("user@example.com", "password123");

    boolean updated =
        authService.updatePassword("user@example.com", "password123", "new-password123");

    assertThat(updated).isTrue();
    assertThat(authService.authenticate("user@example.com", "new-password123")).isPresent();
    assertThat(authService.authenticate("user@example.com", "password123")).isEmpty();
  }

  @Test
  void updatePasswordFailsWhenCurrentPasswordDoesNotMatch() {
    authService.register("user@example.com", "password123");

    boolean updated =
        authService.updatePassword("user@example.com", "wrong-password", "new-password123");

    assertThat(updated).isFalse();
    assertThat(authService.authenticate("user@example.com", "password123")).isPresent();
  }
}
