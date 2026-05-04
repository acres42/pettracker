ALTER TABLE user_preferences
  ADD COLUMN preferred_gender ENUM('male', 'female') NULL AFTER preferred_species;
