package com.ac.pettracker.service;

import com.ac.pettracker.model.UserAccount;
import com.ac.pettracker.repository.UserAccountRepository;
import java.util.Optional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final UserAccountRepository userAccountRepository;
  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  public AuthService(UserAccountRepository userAccountRepository) {
    this.userAccountRepository = userAccountRepository;
  }

  public boolean register(String email, String rawPassword) {
    String normalizedEmail = normalizeEmail(email);
    if (normalizedEmail == null || rawPassword == null || rawPassword.length() < 8) {
      return false;
    }
    if (userAccountRepository.existsByEmail(normalizedEmail)) {
      return false;
    }

    String passwordHash = passwordEncoder.encode(rawPassword);
    userAccountRepository.save(new UserAccount(normalizedEmail, passwordHash));
    return true;
  }

  public Optional<UserAccount> authenticate(String email, String rawPassword) {
    String normalizedEmail = normalizeEmail(email);
    if (normalizedEmail == null || rawPassword == null) {
      return Optional.empty();
    }

    return userAccountRepository
        .findByEmail(normalizedEmail)
        .filter(user -> passwordEncoder.matches(rawPassword, user.getPasswordHash()));
  }

  public boolean updatePassword(String email, String currentRawPassword, String newRawPassword) {
    String normalizedEmail = normalizeEmail(email);
    if (normalizedEmail == null
        || currentRawPassword == null
        || newRawPassword == null
        || newRawPassword.length() < 8) {
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
