package com.ac.pettracker.model;

public class Pet {
  private final String name;
  private final String type;
  private final String breed;
  private final int age;
  private String description;
  private final String imageUrl;

  public Pet(
      String name, String type, String breed, Integer age, String description, String imageUrl) {
    this.name = name;
    this.type = type;
    this.breed = breed;
    this.age = age;
    this.description = description;
    this.imageUrl = imageUrl;
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
}
