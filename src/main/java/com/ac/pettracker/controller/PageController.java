package com.ac.pettracker.controller;

import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {

  @GetMapping("/")
  public String home() {
    return "index";
  }

  @GetMapping("/search")
  public String search() {
    return "search";
  }

  @GetMapping("/pets/results")
  public String results(@RequestParam String type, @RequestParam String location, Model model) {
    model.addAttribute("type", type);
    model.addAttribute("location", location);

    model.addAttribute(
        "pets",
        List.of(
            type + " 1 - Friendly and adoptable",
            type + " 2 - Loves people",
            type + " 3 - Very energetic"));

    return "results";
  }
}
