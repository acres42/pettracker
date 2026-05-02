package com.ac.pettracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_accounts")
public class UserAccount {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false)
  private String passwordHash;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  protected UserAccount() {
    // JPA constructor
  }

  public UserAccount(String email, String passwordHash) {
    this.email = email;
    this.passwordHash = passwordHash;
  }

  @PrePersist
  void onCreate() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }

  public Long getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
