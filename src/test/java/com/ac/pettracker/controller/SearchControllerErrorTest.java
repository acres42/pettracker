package com.ac.pettracker.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.ac.pettracker.service.AuthService;
import com.ac.pettracker.service.PetService;
import com.ac.pettracker.service.ProfileService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SearchController.class)
class SearchControllerErrorTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PetService petService;

  @MockitoBean private AuthService authService;

  @MockitoBean private ProfileService profileService;

  @Test
  void missingTypeShowsErrorPage() throws Exception {
    MockHttpSession session = authenticatedSession();
    mockMvc
        .perform(get("/pets/results").session(session))
        .andExpect(status().isBadRequest())
        .andExpect(view().name("error"))
        .andExpect(content().string(containsString("Invalid search request")))
        .andExpect(content().string(containsString("name=\"viewport\"")))
        .andExpect(content().string(containsString("name=\"description\"")))
        .andExpect(content().string(containsString("property=\"og:title\"")))
        .andExpect(content().string(containsString("rel=\"icon\"")));
  }

  @Test
  void serviceExceptionShowsErrorPage() throws Exception {
    when(petService.searchPets("dog", "", "", "")).thenThrow(new RuntimeException("boom"));
    MockHttpSession session = authenticatedSession();

    mockMvc
        .perform(get("/pets/results").session(session).param("type", "dog"))
        .andExpect(status().isInternalServerError())
        .andExpect(view().name("error"))
        .andExpect(content().string(containsString("Something went wrong")));
  }

  private MockHttpSession authenticatedSession() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("authUserId", 1L);
    session.setAttribute("authUserEmail", "user@example.com");
    return session;
  }
}
