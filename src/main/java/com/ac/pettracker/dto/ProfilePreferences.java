package com.ac.pettracker.dto;

/**
 * Immutable record of a user's normalized pet-preference settings, returned by {@link
 * com.ac.pettracker.service.ProfileService} after a preferences save so the calling controller can
 * update the session without re-querying the database.
 */
public record ProfilePreferences(
    String species, String gender, String weight, String breed, String keywords) {}
