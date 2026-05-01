package com.ac.pettracker.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.ac.pettracker.model.Pet;
import com.ac.pettracker.service.PetService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PageController.class)
class PageControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PetService petService;

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
    given(petService.searchPets("dog", "46201"))
        .willReturn(List.of(new Pet("Buddy", "dog", "Friendly and adoptable")));

    mockMvc
        .perform(get("/pets/results").param("type", "dog").param("location", "46201"))
        .andExpect(status().isOk())
        .andExpect(view().name("results"))
        .andExpect(model().attributeExists("pets"))
        .andExpect(model().attribute("type", "dog"))
        .andExpect(model().attribute("location", "46201"));
  }
}
