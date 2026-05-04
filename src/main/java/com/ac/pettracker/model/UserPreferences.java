package com.ac.pettracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * JPA entity representing a user's pet preferences stored in the {@code user_preferences} table.
 */
@Entity
@Table(name = "user_preferences")
public class UserPreferences {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_account_id", nullable = false)
  private Long userAccountId;

  @Column(name = "preferred_species")
  private String preferredSpecies;

  @Column(name = "preferred_gender")
  private String preferredGender;

  @Column(name = "preferred_weight_band")
  private String preferredWeightBand;

  @Column(name = "preferred_breed")
  private String preferredBreed;

  @Column(name = "preferred_keywords")
  private String preferredKeywords;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  /** No-arg constructor required by JPA. */
  protected UserPreferences() {}

  /**
   * Creates a new {@code UserPreferences} for the given user account.
   *
   * @param userAccountId the ID of the owning user account
   */
  public UserPreferences(Long userAccountId) {
    this.userAccountId = userAccountId;
  }

  /** Sets {@code createdAt} and {@code updatedAt} before initial persist. */
  @PrePersist
  void onCreate() {
    LocalDateTime now = LocalDateTime.now();
    if (createdAt == null) {
      createdAt = now;
    }
    if (updatedAt == null) {
      updatedAt = now;
    }
  }

  /** Refreshes {@code updatedAt} before every update. */
  @PreUpdate
  void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  /**
   * Returns the primary key.
   *
   * @return the record ID
   */
  public Long getId() {
    return id;
  }

  /**
   * Returns the owning user account ID.
   *
   * @return the user account ID
   */
  public Long getUserAccountId() {
    return userAccountId;
  }

  /**
   * Returns the preferred species (e.g. {@code "dog"}, {@code "cat"}), or {@code null} if none.
   *
   * @return the preferred species
   */
  public String getPreferredSpecies() {
    return preferredSpecies;
  }

  /**
   * Sets the preferred species.
   *
   * @param preferredSpecies the species value to store
   */
  public void setPreferredSpecies(String preferredSpecies) {
    this.preferredSpecies = preferredSpecies;
  }

  /**
   * Returns the preferred gender ({@code "male"}, {@code "female"}), or {@code null} if none.
   *
   * @return the preferred gender
   */
  public String getPreferredGender() {
    return preferredGender;
  }

  /**
   * Sets the preferred gender. Must be {@code "male"}, {@code "female"}, or {@code null}.
   *
   * @param preferredGender the gender value to store
   */
  public void setPreferredGender(String preferredGender) {
    this.preferredGender = preferredGender;
  }

  /**
   * Returns the preferred weight band (e.g. {@code "25-50lbs"}), or {@code null} if none.
   *
   * @return the preferred weight band
   */
  public String getPreferredWeightBand() {
    return preferredWeightBand;
  }

  /**
   * Sets the preferred weight band.
   *
   * @param preferredWeightBand the weight band value to store
   */
  public void setPreferredWeightBand(String preferredWeightBand) {
    this.preferredWeightBand = preferredWeightBand;
  }

  /**
   * Returns the preferred breed, or {@code null} if none.
   *
   * @return the preferred breed
   */
  public String getPreferredBreed() {
    return preferredBreed;
  }

  /**
   * Sets the preferred breed.
   *
   * @param preferredBreed the breed value to store
   */
  public void setPreferredBreed(String preferredBreed) {
    this.preferredBreed = preferredBreed;
  }

  /**
   * Returns the preferred keywords, or {@code null} if none.
   *
   * @return the preferred keywords
   */
  public String getPreferredKeywords() {
    return preferredKeywords;
  }

  /**
   * Sets the preferred keywords.
   *
   * @param preferredKeywords the keywords value to store
   */
  public void setPreferredKeywords(String preferredKeywords) {
    this.preferredKeywords = preferredKeywords;
  }
}
