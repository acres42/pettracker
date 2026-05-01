package com.ac.pettracker.model;

public class Pet {
  private String name;
  private String type;
  private String description;

  public Pet(String name, String type, String description) {
    this.name = name;
    this.type = type;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }
}
