package com.ac.pettracker.dto;

import com.ac.pettracker.model.Pet;
import java.util.List;

/**
 * Holds the outcome of a keyword-filtered pet search: the matching pets and any keywords that
 * produced no matches across the candidate set.
 *
 * @param pets the pets that matched at least one keyword (or all candidates when keywords is empty)
 * @param unmatchedKeywords keywords from the search for which no pet's description contained a
 *     match
 */
public record PetSearchResult(List<Pet> pets, List<String> unmatchedKeywords) {}
