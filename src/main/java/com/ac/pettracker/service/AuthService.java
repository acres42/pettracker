package com.ac.pettracker.service;

import com.ac.pettracker.model.UserAccount;
import com.ac.pettracker.repository.UserAccountRepository;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Service handling user registration, authentication, and password management. */
@Service
public class AuthService {

  private final UserAccountRepository userAccountRepository;
  private final PasswordEncoder passwordEncoder;

  public AuthService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
    this.userAccountRepository = userAccountRepository;
    this.passwordEncoder = passwordEncoder;
  }

  /**
   * Registers a new user account with the given email and password.
   *
   * @param email the user's email address (normalized to lowercase, trimmed)
   * @param rawPassword the plain-text password (minimum 8 characters)
   * @return {@code true} if registration succeeded; {@code false} if validation failed or the email
   *     is already in use
   */
  public boolean register(String email, String rawPassword) {
    String normalizedEmail = normalizeEmail(email);
    if (normalizedEmail == null || !isValidPassword(rawPassword)) {
      return false;
    }
    if (userAccountRepository.existsByEmail(normalizedEmail)) {
      return false;
    }

    String passwordHash = passwordEncoder.encode(rawPassword);
    userAccountRepository.save(new UserAccount(normalizedEmail, passwordHash));
    return true;
  }

  /**
   * Authenticates a user by verifying the given password against the stored hash.
   *
   * @param email the user's email address
   * @param rawPassword the plain-text password to verify
   * @return an {@link java.util.Optional} containing the account if credentials are valid, or empty
   *     if authentication fails
   */
  public Optional<UserAccount> authenticate(String email, String rawPassword) {
    String normalizedEmail = normalizeEmail(email);
    if (normalizedEmail == null || rawPassword == null) {
      return Optional.empty();
    }

    return userAccountRepository
        .findByEmail(normalizedEmail)
        .filter(user -> passwordEncoder.matches(rawPassword, user.getPasswordHash()));
  }

  /**
   * Updates the password for the authenticated user.
   *
   * @param email the user's email address
   * @param currentRawPassword the current plain-text password (used to verify identity)
   * @param newRawPassword the new plain-text password (minimum 8 characters)
   * @return {@code true} if the password was updated; {@code false} if validation or authentication
   *     failed
   */
  public boolean updatePassword(String email, String currentRawPassword, String newRawPassword) {
    String normalizedEmail = normalizeEmail(email);
    if (normalizedEmail == null || currentRawPassword == null || !isValidPassword(newRawPassword)) {
      return false;
    }

    Optional<UserAccount> userOptional = userAccountRepository.findByEmail(normalizedEmail);
    if (userOptional.isEmpty()) {
      return false;
    }

    UserAccount user = userOptional.get();
    if (!passwordEncoder.matches(currentRawPassword, user.getPasswordHash())) {
      return false;
    }

    user.setPasswordHash(passwordEncoder.encode(newRawPassword));
    userAccountRepository.save(user);
    return true;
  }

  /**
   * Returns {@code true} if the password meets minimum security requirements: at least 10
   * characters, at least one letter, and at least one digit.
   *
   * @param password the plain-text password to validate
   * @return {@code true} if valid
   */
  private boolean isValidPassword(String password) {
    if (password == null || password.length() < 10) {
      return false;
    }
    return password.chars().anyMatch(Character::isLetter)
        && password.chars().anyMatch(Character::isDigit);
  }

  private String normalizeEmail(String email) {
    if (email == null) {
      return null;
    }
    String normalized = email.trim().toLowerCase();
    if (normalized.isBlank() || !normalized.contains("@")) {
      return null;
    }
    return normalized;
  }
}
