package com.ac.pettracker.dto;

/** Immutable data transfer object representing a pet returned from a search. */
public record PetDto(
    String name,
    String type,
    String breed,
    int age,
    String description,
    String imageUrl,
    String gender) {}
