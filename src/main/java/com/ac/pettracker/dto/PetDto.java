package com.ac.pettracker.dto;

public record PetDto(
    String name, String type, String breed, int age, String description, String imageUrl) {}
