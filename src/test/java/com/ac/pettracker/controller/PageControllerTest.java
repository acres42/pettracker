package com.ac.pettracker.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
        .willReturn(
            List.of(
                new Pet(
                    "Buddy",
                    "dog",
                    "Golden Retriever",
                    4,
                    "Friendly dog",
                    "/images/pets/buddy.jpg")));

    mockMvc
        .perform(get("/pets/results").param("type", "dog").param("location", "46201"))
        .andExpect(status().isOk())
        .andExpect(view().name("results"))
        .andExpect(model().attributeExists("pets"))
        .andExpect(model().attribute("type", "dog"))
        .andExpect(model().attribute("location", "46201"));
  }

  @Test
  void searchResultsReturnsBadRequestWhenParamsAreMissing() throws Exception {
    mockMvc
        .perform(get("/pets/results"))
        .andExpect(status().isBadRequest())
        .andExpect(view().name("error"));
  }

  @Test
  void searchResultsPageShowsEmptyStateWhenNoPetsFound() throws Exception {
    given(petService.searchPets("bird", "46201")).willReturn(List.of());

    mockMvc
        .perform(get("/pets/results").param("type", "bird").param("location", "46201"))
        .andExpect(status().isOk())
        .andExpect(view().name("results"))
        .andExpect(model().attributeExists("pets"))
        .andExpect(model().attribute("type", "bird"))
        .andExpect(model().attribute("location", "46201"))
        .andExpect(content().string(containsString("No pets found")));
  }

  @Test
  void searchResultsPageRendersPetImageWhenImageUrlExists() throws Exception {
    when(petService.searchPets("dog", "46201"))
        .thenReturn(
            List.of(
                new Pet(
                    "Buddy",
                    "dog",
                    "Golden Retriever",
                    4,
                    "Friendly dog",
                    "/images/pets/buddy.jpg")));

    mockMvc
        .perform(get("/pets/results").param("type", "dog").param("location", "46201"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("<img")))
        .andExpect(content().string(containsString("/images/pets/buddy.jpg")))
        .andExpect(content().string(containsString("Buddy")));
  }

  @Test
  void searchResultsPageRendersFallbackWhenImageUrlIsMissing() throws Exception {
    when(petService.searchPets("dog", "46201"))
        .thenReturn(List.of(new Pet("Buddy", "dog", "Golden Retriever", 4, "Friendly dog", "")));

    mockMvc
        .perform(get("/pets/results").param("type", "dog").param("location", "46201"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No image available")));
  }
}
