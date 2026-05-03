package com.ac.pettracker.repository;

import com.ac.pettracker.model.Pet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for database-backed pet queries. */
public interface PetJpaRepository extends JpaRepository<Pet, Long> {

  List<Pet> findByTypeIgnoreCase(String type);
}
