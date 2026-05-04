package com.ac.pettracker.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ac.pettracker.repository.UserPreferencesRepository;
import com.ac.pettracker.service.AuthService;
import com.ac.pettracker.service.PetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({
  AuthController.class,
  ProfileController.class,
  DashboardController.class,
  SearchController.class
})
class AuthGuardControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PetService petService;

  @MockitoBean private AuthService authService;

  @MockitoBean private UserPreferencesRepository userPreferencesRepository;

  @Test
  void unauthenticatedSearchRedirectsHome() throws Exception {
    mockMvc
        .perform(get("/search"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }

  @Test
  void unauthenticatedResultsRedirectsHome() throws Exception {
    mockMvc
        .perform(get("/pets/results").param("type", "dog").param("location", "46201"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }

  @Test
  void unauthenticatedDashboardRedirectsHome() throws Exception {
    mockMvc
        .perform(get("/dashboard"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }

  @Test
  void unauthenticatedProfileRedirectsHome() throws Exception {
    mockMvc
        .perform(get("/profile"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }

  @Test
  void unauthenticatedProfilePreferencesUpdateRedirectsHome() throws Exception {
    mockMvc
        .perform(
            post("/profile/preferences")
                .with(csrf())
                .param("species", "dog")
                .param("weight", "25-50lbs")
                .param("breed", "beagle")
                .param("keywords", "calm"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }

  @Test
  void unauthenticatedPasswordUpdateRedirectsHome() throws Exception {
    mockMvc
        .perform(
            post("/profile/password")
                .with(csrf())
                .param("currentPassword", "old-password")
                .param("newPassword", "new-password123")
                .param("confirmPassword", "new-password123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }

  @Test
  void unauthenticatedSaveRedirectsHome() throws Exception {
    mockMvc
        .perform(
            post("/dashboard/save")
                .with(csrf())
                .param("name", "Buddy")
                .param("type", "dog")
                .param("breed", "Golden Retriever")
                .param("age", "4")
                .param("description", "Friendly dog")
                .param("imageUrl", "/images/pets/buddy.jpg"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }

  @Test
  void unauthenticatedUpdateRedirectsHome() throws Exception {
    mockMvc
        .perform(
            post("/dashboard/update")
                .with(csrf())
                .param("id", "abc-123")
                .param("notes", "check")
                .param("status", "INTRODUCED"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }

  @Test
  void unauthenticatedDeleteRedirectsHome() throws Exception {
    mockMvc
        .perform(post("/dashboard/delete").with(csrf()).param("id", "abc-123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }
}
