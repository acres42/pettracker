package com.ac.pettracker.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Session-scoped model for a pet saved to the user's dashboard, including notes and status. */
public class SavedPetEntry {

  private static final DateTimeFormatter SAVED_AT_FORMATTER =
      DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm");
  private final String id;
  private final String name;
  private final String type;
  private final String breed;
  private final int age;
  private final String description;
  private final String imageUrl;
  private final LocalDateTime savedAt;

  private String keywords;
  private String notes;
  private SavedPetStatus status;
  private LocalDate statusDate;

  /**
   * Creates a new saved pet entry with the given pet details and tracking state.
   *
   * @param name pet's name
   * @param type pet species (e.g., dog, cat)
   * @param breed pet breed
   * @param age pet age in years
   * @param description short description of the pet
   * @param imageUrl URL or path to the pet's image
   * @param keywords profile keywords copied from the user's preferences at save time
   * @param notes user-supplied notes (truncated to 500 characters)
   * @param status initial adoption status
   * @param statusDate date the status was last set
   */
  public SavedPetEntry(
      String name,
      String type,
      String breed,
      int age,
      String description,
      String imageUrl,
      String keywords,
      String notes,
      SavedPetStatus status,
      LocalDate statusDate) {
    this.id = UUID.randomUUID().toString();
    this.name = name;
    this.type = type;
    this.breed = breed;
    this.age = age;
    this.description = description;
    this.imageUrl = imageUrl;
    this.savedAt = LocalDateTime.now();
    this.keywords = keywords;
    setNotes(notes);
    this.status = status;
    this.statusDate = statusDate;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getBreed() {
    return breed;
  }

  public int getAge() {
    return age;
  }

  public String getDescription() {
    return description;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public LocalDateTime getSavedAt() {
    return savedAt;
  }

  /**
   * Returns the saved-at timestamp formatted as {@code dd/MM/yyyy, HH:mm}.
   *
   * @return formatted save timestamp
   */
  public String getSavedAtDisplay() {
    return savedAt.format(SAVED_AT_FORMATTER);
  }

  public String getKeywords() {
    return keywords;
  }

  public void setKeywords(String keywords) {
    this.keywords = keywords;
  }

  public String getKeywordsDisplay() {
    if (keywords == null || keywords.isBlank()) {
      return "-";
    }
    return keywords;
  }

  public String getNotes() {
    return notes;
  }

  /**
   * Sets the user's notes for this pet, truncating to 500 characters if necessary.
   *
   * @param notes the notes text; {@code null} is treated as an empty string
   */
  public void setNotes(String notes) {
    if (notes == null) {
      this.notes = "";
      return;
    }
    this.notes = notes.length() > 500 ? notes.substring(0, 500) : notes;
  }

  public SavedPetStatus getStatus() {
    return status;
  }

  public void setStatus(SavedPetStatus status) {
    this.status = status;
    this.statusDate = LocalDate.now();
  }

  public LocalDate getStatusDate() {
    return statusDate;
  }

  public void setStatusDate(LocalDate statusDate) {
    this.statusDate = statusDate;
  }

  /**
   * Returns a human-readable label for the current adoption status.
   *
   * @return display string such as "Adopted" or "Introduced"
   */
  public String getStatusDisplay() {
    return switch (status) {
      case ADOPTED -> "Adopted";
      case REJECTED -> "Rejected";
      case INTRODUCED -> "Introduced";
      case APPLIED -> "Applied";
      case ACCEPTED -> "Accepted";
    };
  }
}
