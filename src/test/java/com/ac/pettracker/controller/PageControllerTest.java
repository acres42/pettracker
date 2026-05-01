package com.ac.pettracker.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PageController.class)
class PageControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void homePageReturnsIndexView() throws Exception {
    mockMvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("index"));
  }

  @Test
  void searchPageLoads() throws Exception {
    mockMvc.perform(get("/search")).andExpect(status().isOk()).andExpect(view().name("search"));
  }

  @Test
  void searchResultsPageLoadsWithFakePets() throws Exception {
    mockMvc
        .perform(get("/pets/results").param("type", "dog").param("location", "46201"))
        .andExpect(status().isOk())
        .andExpect(view().name("results"))
        .andExpect(model().attributeExists("pets"));
  }
}
