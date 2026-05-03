ALTER TABLE pets
  ADD COLUMN gender ENUM('male', 'female') NULL AFTER type;
