package com.ac.pettracker.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.ac.pettracker.service.PetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PageController.class)
class AppErrorControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PetService petService;

  @Test
  void missingTypeShowsErrorPage() throws Exception {
    mockMvc
        .perform(get("/pets/results").param("location", "46201"))
        .andExpect(status().isBadRequest())
        .andExpect(view().name("error"))
        .andExpect(content().string(containsString("Invalid search request")))
        .andExpect(content().string(containsString("name=\"viewport\"")))
        .andExpect(content().string(containsString("name=\"description\"")))
        .andExpect(content().string(containsString("property=\"og:title\"")))
        .andExpect(content().string(containsString("rel=\"icon\"")));
  }

  @Test
  void missingLocationShowsErrorPage() throws Exception {
    mockMvc
        .perform(get("/pets/results").param("type", "dog"))
        .andExpect(status().isBadRequest())
        .andExpect(view().name("error"))
        .andExpect(content().string(containsString("Invalid search request")));
  }

  @Test
  void serviceExceptionShowsErrorPage() throws Exception {
    when(petService.searchPets("dog", "46201")).thenThrow(new RuntimeException("boom"));

    mockMvc
        .perform(get("/pets/results").param("type", "dog").param("location", "46201"))
        .andExpect(status().isInternalServerError())
        .andExpect(view().name("error"))
        .andExpect(content().string(containsString("Something went wrong")));
  }
}
