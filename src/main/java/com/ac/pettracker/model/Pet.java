package com.ac.pettracker.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** JPA entity representing an adoptable pet stored in the {@code pets} table. */
@Entity
@Table(name = "pets")
public class Pet {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 50)
  private String type;

  @Column(nullable = false, length = 10)
  private String gender;

  @Column(length = 100)
  private String breed;

  private Integer age;

  @Column(length = 500)
  private String description;

  @Column(name = "image_url", length = 255)
  private String imageUrl;

  @Column(name = "weight_lbs")
  private Integer weightLbs;

  /** No-arg constructor required by JPA. */
  protected Pet() {}

  public Pet(
      String name, String type, String breed, Integer age, String description, String imageUrl) {
    this.name = name;
    this.type = type;
    this.breed = breed;
    this.age = age;
    this.description = description;
    this.imageUrl = imageUrl;
  }

  /** Full constructor including gender and weight. */
  public Pet(
      String name,
      String type,
      String breed,
      Integer age,
      String description,
      String imageUrl,
      String gender,
      Integer weightLbs) {
    this(name, type, breed, age, description, imageUrl);
    this.gender = gender;
    this.weightLbs = weightLbs;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getGender() {
    return gender;
  }

  public String getBreed() {
    return breed;
  }

  public Integer getAge() {
    return age;
  }

  public String getDescription() {
    return description;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public Integer getWeightLbs() {
    return weightLbs;
  }
}
