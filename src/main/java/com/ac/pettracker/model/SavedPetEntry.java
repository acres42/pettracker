package com.ac.pettracker.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

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
  }

  public LocalDate getStatusDate() {
    return statusDate;
  }

  public void setStatusDate(LocalDate statusDate) {
    this.statusDate = statusDate;
  }

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
