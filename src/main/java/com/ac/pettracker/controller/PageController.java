package com.ac.pettracker.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

  @GetMapping("/")
  public String home() {
    return "index";
  }

  @GetMapping("/search")
  String search() {
    return "search";
  }
}
