package com.ac.pettracker.model;

/** Utility for building the stable pipe-delimited key that uniquely identifies a pet. */
public final class PetKeys {

  private PetKeys() {}

  /**
   * Returns a key in the form {@code name|type|breed|age}. {@code null} fields are treated as empty
   * string so the key is always well-formed.
   *
   * @param name pet name
   * @param type pet species
   * @param breed pet breed
   * @param age pet age in years
   * @return stable pet key
   */
  public static String of(String name, String type, String breed, int age) {
    return String.join(
        "|",
        name != null ? name : "",
        type != null ? type : "",
        breed != null ? breed : "",
        Integer.toString(age));
  }
}
