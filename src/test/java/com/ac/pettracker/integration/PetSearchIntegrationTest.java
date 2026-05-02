package com.ac.pettracker.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class PetSearchIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  public void testPetSearch() throws Exception {
    mockMvc.perform(get("/search")).andExpect(status().isOk());
  }
}
