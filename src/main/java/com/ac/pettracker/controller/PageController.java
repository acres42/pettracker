package com.ac.pettracker.controller;

import com.ac.pettracker.service.PetService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {

  private final PetService petService;

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
      return "redirect:/search";
    }

    model.addAttribute("type", type);
    model.addAttribute("location", location);
    model.addAttribute("pets", petService.searchPets(type, location));

    return "results";
  }
}
