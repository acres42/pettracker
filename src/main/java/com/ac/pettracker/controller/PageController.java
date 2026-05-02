package com.ac.pettracker.controller;

import com.ac.pettracker.model.Pet;
import com.ac.pettracker.service.PetService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {

  private final PetService petService;
  private static final Logger logger = LoggerFactory.getLogger(PageController.class);

  public PageController(PetService petService) {
    this.petService = petService;
  }

  @GetMapping("/")
  public String home() {
    return "index";
  }

  @GetMapping("/search")
  public String search(@RequestParam(name = "q", required = false) String query, Model model) {
    model.addAttribute("query", query);
    return "search";
  }

  @GetMapping("/pets/results")
  public String results(
      @RequestParam(required = false) String type,
      @RequestParam(required = false) String location,
      Model model) {
    if (type == null || type.isBlank() || location == null || location.isBlank()) {
      throw new IllegalArgumentException("Missing search parameters");
    }
    logger.info("Searching pets with type={} location={}", type, location);

    List<Pet> pets = petService.searchPets(type, location);

    model.addAttribute("type", type);
    model.addAttribute("location", location);
    model.addAttribute("pets", pets);
    return "results";
  }
}
