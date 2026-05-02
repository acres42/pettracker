package com.ac.pettracker.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.ac.pettracker.model.Pet;
import com.ac.pettracker.model.UserAccount;
import com.ac.pettracker.service.AuthService;
import com.ac.pettracker.service.PetService;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(PageController.class)
class PageControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private PetService petService;

  @MockitoBean private AuthService authService;

  @Test
  void homePageReturnsIndexView() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(view().name("index"))
        .andExpect(content().string(containsString("name=\"viewport\"")))
        .andExpect(content().string(containsString("name=\"description\"")))
        .andExpect(content().string(containsString("property=\"og:title\"")))
        .andExpect(content().string(containsString("rel=\"icon\"")))
        .andExpect(content().string(containsString("<form")))
        .andExpect(content().string(containsString("type=\"password\"")))
        .andExpect(content().string(containsString("Log In")))
        .andExpect(content().string(containsString("site-header")))
        .andExpect(content().string(containsString("/images/favicon.svg")))
        .andExpect(content().string(containsString("href=\"/dashboard\"")))
        .andExpect(content().string(containsString("href=\"/search\"")))
        .andExpect(content().string(containsString("href=\"/profile\"")))
        .andExpect(content().string(containsString(">Login</a>")));
  }

  @Test
  void signupPageReturnsSignupView() throws Exception {
    mockMvc
        .perform(get("/signup"))
        .andExpect(status().isOk())
        .andExpect(view().name("signup"))
        .andExpect(content().string(containsString("name=\"viewport\"")))
        .andExpect(content().string(containsString("name=\"description\"")))
        .andExpect(content().string(containsString("property=\"og:title\"")))
        .andExpect(content().string(containsString("rel=\"icon\"")))
        .andExpect(content().string(containsString("method=\"post\" action=\"/auth/register\"")))
        .andExpect(content().string(containsString("Create Account")))
        .andExpect(content().string(containsString("href=\"/\"")));
  }

  @Test
  void searchPageLoads() throws Exception {
    mockMvc
        .perform(get("/search").session(authenticatedSession()))
        .andExpect(status().isOk())
        .andExpect(view().name("search"))
        .andExpect(content().string(containsString("name=\"viewport\"")))
        .andExpect(content().string(containsString("name=\"description\"")))
        .andExpect(content().string(containsString("property=\"og:title\"")))
        .andExpect(content().string(containsString("rel=\"icon\"")))
        .andExpect(content().string(containsString("site-header")));
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
        .perform(
            get("/pets/results")
                .session(authenticatedSession())
                .param("type", "dog")
                .param("location", "46201"))
        .andExpect(status().isOk())
        .andExpect(view().name("results"))
        .andExpect(model().attributeExists("pets"))
        .andExpect(model().attribute("type", "dog"))
        .andExpect(model().attribute("location", "46201"))
        .andExpect(content().string(containsString("name=\"viewport\"")))
        .andExpect(content().string(containsString("name=\"description\"")))
        .andExpect(content().string(containsString("property=\"og:title\"")))
        .andExpect(content().string(containsString("rel=\"icon\"")))
        .andExpect(content().string(containsString("site-header")))
        .andExpect(content().string(containsString("/dashboard/save")))
        .andExpect(content().string(containsString("Save")));
  }

  @Test
  void searchResultsHeartReflectsWhetherPetIsAlreadySaved() throws Exception {
    given(petService.searchPets("dog", "46201"))
        .willReturn(
            List.of(
                new Pet(
                    "Buddy",
                    "dog",
                    "Golden Retriever",
                    4,
                    "Friendly dog",
                    "/images/pets/buddy.jpg"),
                new Pet("Milo", "dog", "Beagle", 2, "Curious dog", "/images/pets/milo.jpg")));

    MockHttpSession session = new MockHttpSession();
    session.setAttribute("authUserId", 1L);
    session.setAttribute("authUserEmail", "user@example.com");

    MvcResult beforeSave =
        mockMvc
            .perform(
                get("/pets/results")
                    .session(session)
                    .param("type", "dog")
                    .param("location", "46201"))
            .andExpect(status().isOk())
            .andReturn();

    String beforeHtml = beforeSave.getResponse().getContentAsString();
    assertThat(beforeHtml).contains("heart-save");
    assertThat(beforeHtml).contains("aria-pressed=\"false\"");
    assertThat(beforeHtml).doesNotContain("heart-save is-saved");

    mockMvc
        .perform(
            post("/dashboard/save")
                .session(session)
                .param("name", "Buddy")
                .param("type", "dog")
                .param("breed", "Golden Retriever")
                .param("age", "4")
                .param("description", "Friendly dog")
                .param("imageUrl", "/images/pets/buddy.jpg"))
        .andExpect(status().is3xxRedirection());

    MvcResult afterSave =
        mockMvc
            .perform(
                get("/pets/results")
                    .session(session)
                    .param("type", "dog")
                    .param("location", "46201"))
            .andExpect(status().isOk())
            .andReturn();

    String afterHtml = afterSave.getResponse().getContentAsString();
    assertThat(afterHtml).contains("heart-save is-saved");
    assertThat(afterHtml).contains("aria-pressed=\"true\"");
    assertThat(afterHtml).contains("Saved ♥");
    assertThat(afterHtml).contains("Save ♡");
  }

  @Test
  void searchResultsReturnsBadRequestWhenParamsAreMissing() throws Exception {
    mockMvc
        .perform(get("/pets/results").session(authenticatedSession()))
        .andExpect(status().isBadRequest())
        .andExpect(view().name("error"));
  }

  @Test
  void searchResultsPageShowsEmptyStateWhenNoPetsFound() throws Exception {
    given(petService.searchPets("bird", "46201")).willReturn(List.of());

    mockMvc
        .perform(
            get("/pets/results")
                .session(authenticatedSession())
                .param("type", "bird")
                .param("location", "46201"))
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
        .perform(
            get("/pets/results")
                .session(authenticatedSession())
                .param("type", "dog")
                .param("location", "46201"))
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
        .perform(
            get("/pets/results")
                .session(authenticatedSession())
                .param("type", "dog")
                .param("location", "46201"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No image available")));
  }

  @Test
  void loginRedirectsToSearchWhenCredentialsAreValid() throws Exception {
    UserAccount user = new UserAccount("user@example.com", "$2a$hash");
    given(authService.authenticate("user@example.com", "password123"))
        .willReturn(Optional.of(user));

    mockMvc
        .perform(
            post("/auth/login").param("email", "user@example.com").param("password", "password123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/search"));
  }

  @Test
  void registerRedirectsToSearchWhenPayloadIsValid() throws Exception {
    given(authService.register("new@example.com", "password123")).willReturn(true);
    UserAccount user = new UserAccount("new@example.com", "$2a$hash");
    given(authService.authenticate("new@example.com", "password123")).willReturn(Optional.of(user));

    mockMvc
        .perform(
            post("/auth/register")
                .param("email", "new@example.com")
                .param("password", "password123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/search"));
  }

  @Test
  void dashboardPageLoads() throws Exception {
    mockMvc
        .perform(get("/dashboard").session(authenticatedSession()))
        .andExpect(status().isOk())
        .andExpect(view().name("dashboard"))
        .andExpect(content().string(containsString("Saved Pets Dashboard")))
        .andExpect(content().string(containsString("site-header")));
  }

  @Test
  void profilePageLoads() throws Exception {
    mockMvc
        .perform(get("/profile").session(authenticatedSession()))
        .andExpect(status().isOk())
        .andExpect(view().name("profile"))
        .andExpect(content().string(containsString("Profile")))
        .andExpect(content().string(containsString("site-header")))
        .andExpect(content().string(containsString("First Name")))
        .andExpect(content().string(containsString("Last Name")))
        .andExpect(content().string(containsString("Email Address")))
        .andExpect(content().string(containsString("action=\"/profile/password\"")))
        .andExpect(content().string(containsString("action=\"/profile/preferences\"")));
  }

  @Test
  void profileKeywordsPopulateSavedPetDashboardColumn() throws Exception {
    MvcResult profileResult =
        mockMvc
            .perform(
                post("/profile/preferences")
                    .session(authenticatedSession())
                    .param("species", "dog")
                    .param("weight", "25-50lbs")
                    .param("breed", "beagle")
                    .param("keywords", "calm, fenced yard"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/profile"))
            .andReturn();

    MockHttpSession session = (MockHttpSession) profileResult.getRequest().getSession(false);

    mockMvc
        .perform(
            post("/dashboard/save")
                .session(session)
                .param("name", "Buddy")
                .param("type", "dog")
                .param("breed", "Golden Retriever")
                .param("age", "4")
                .param("description", "Friendly dog")
                .param("imageUrl", "/images/pets/buddy.jpg"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/dashboard"));

    MvcResult dashboardResult =
        mockMvc.perform(get("/dashboard").session(session)).andExpect(status().isOk()).andReturn();

    String html = dashboardResult.getResponse().getContentAsString();
    assertThat(html).contains("calm, fenced yard");
    assertThat(html).doesNotContain("name=\"keywords\"");
  }

  @Test
  void passwordUpdateRedirectsToSuccessWhenCurrentPasswordIsValid() throws Exception {
    given(authService.updatePassword("user@example.com", "old-password", "new-password123"))
        .willReturn(true);

    mockMvc
        .perform(
            post("/profile/password")
                .session(authenticatedSession())
                .param("currentPassword", "old-password")
                .param("newPassword", "new-password123")
                .param("confirmPassword", "new-password123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/profile?passwordUpdated=1"));
  }

  @Test
  void passwordUpdateRedirectsToMismatchWhenConfirmationFails() throws Exception {
    mockMvc
        .perform(
            post("/profile/password")
                .session(authenticatedSession())
                .param("currentPassword", "old-password")
                .param("newPassword", "new-password123")
                .param("confirmPassword", "different-pass123"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/profile?passwordError=mismatch"));
  }

  @Test
  void logoutRedirectsHome() throws Exception {
    mockMvc
        .perform(get("/logout"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/"));
  }

  @Test
  void savePetAddsEntryToDashboardSessionList() throws Exception {
    MvcResult saveResult =
        mockMvc
            .perform(
                post("/dashboard/save")
                    .session(authenticatedSession())
                    .param("name", "Buddy")
                    .param("type", "dog")
                    .param("breed", "Golden Retriever")
                    .param("age", "4")
                    .param("description", "Friendly dog")
                    .param("imageUrl", "/images/pets/buddy.jpg"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard"))
            .andReturn();

    MockHttpSession session = (MockHttpSession) saveResult.getRequest().getSession(false);

    mockMvc
        .perform(get("/dashboard").session(session))
        .andExpect(status().isOk())
        .andExpect(view().name("dashboard"))
        .andExpect(model().attributeExists("savedPets"))
        .andExpect(content().string(containsString("Buddy")))
        .andExpect(content().string(containsString("Golden Retriever")));
  }

  @Test
  void dashboardShowsSortableColumnsAndRequiredOrder() throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/dashboard").session(authenticatedSession()))
            .andExpect(status().isOk())
            .andExpect(view().name("dashboard"))
            .andExpect(content().string(containsString("sort=name")))
            .andExpect(content().string(containsString("sort=type")))
            .andExpect(content().string(containsString("sort=breed")))
            .andExpect(content().string(containsString("sort=age")))
            .andExpect(content().string(containsString("sort=savedAt")))
            .andExpect(content().string(containsString("sort=status")))
            .andExpect(content().string(containsString("sort=keywords")))
            .andExpect(content().string(containsString("sort=notes")))
            .andReturn();

    String html = result.getResponse().getContentAsString();
    assertThat(html.indexOf("Age")).isLessThan(html.indexOf("Keywords"));
    assertThat(html.indexOf("Keywords")).isLessThan(html.indexOf("Notes"));
  }

  @Test
  void dashboardShowsSavedAtHumanReadableAndStatusDropdownWithoutDate() throws Exception {
    MvcResult saveResult =
        mockMvc
            .perform(
                post("/dashboard/save")
                    .session(authenticatedSession())
                    .param("name", "Buddy")
                    .param("type", "dog")
                    .param("breed", "Golden Retriever")
                    .param("age", "4")
                    .param("description", "Friendly dog")
                    .param("imageUrl", "/images/pets/buddy.jpg")
                    .param("keywords", "calm, family"))
            .andReturn();

    MockHttpSession session = (MockHttpSession) saveResult.getRequest().getSession(false);
    MvcResult dashboardResult =
        mockMvc.perform(get("/dashboard").session(session)).andExpect(status().isOk()).andReturn();

    String html = dashboardResult.getResponse().getContentAsString();
    assertThat(html).contains("<select");
    assertThat(html).contains("name=" + '"' + "status" + '"');
    assertThat(html).contains("Introduced");
    assertThat(html).doesNotContain("Introduced (");
    assertThat(Pattern.compile("\\d{2}/\\d{2}/\\d{4}, \\d{2}:\\d{2}").matcher(html).find())
        .isTrue();
  }

  @Test
  void savePetStoresNullableKeywordsAndNotesTextareaLimitedTo500Chars() throws Exception {
    String longNotes = "x".repeat(550);

    MvcResult saveResult =
        mockMvc
            .perform(
                post("/dashboard/save")
                    .session(authenticatedSession())
                    .param("name", "Milo")
                    .param("type", "cat")
                    .param("breed", "Tabby")
                    .param("age", "2")
                    .param("description", "Curious cat")
                    .param("imageUrl", "")
                    .param("notes", longNotes))
            .andReturn();

    MockHttpSession session = (MockHttpSession) saveResult.getRequest().getSession(false);
    MvcResult dashboardResult =
        mockMvc.perform(get("/dashboard").session(session)).andExpect(status().isOk()).andReturn();

    String html = dashboardResult.getResponse().getContentAsString();
    assertThat(html).contains("Milo");
    assertThat(html).contains("Tabby");
    assertThat(html).contains("-");
    assertThat(html).contains("<textarea");
    assertThat(html).contains("maxlength=\"500\"");
    assertThat(html).doesNotContain(longNotes);
    assertThat(html).contains("x".repeat(500));
  }

  @Test
  void dashboardShowsUpdateAndDeleteActionsInLastColumn() throws Exception {
    MvcResult saveResult =
        mockMvc
            .perform(
                post("/dashboard/save")
                    .session(authenticatedSession())
                    .param("name", "Buddy")
                    .param("type", "dog")
                    .param("breed", "Golden Retriever")
                    .param("age", "4")
                    .param("description", "Friendly dog")
                    .param("imageUrl", "/images/pets/buddy.jpg"))
            .andReturn();

    MockHttpSession session = (MockHttpSession) saveResult.getRequest().getSession(false);
    MvcResult dashboardResult =
        mockMvc.perform(get("/dashboard").session(session)).andExpect(status().isOk()).andReturn();

    String html = dashboardResult.getResponse().getContentAsString();
    assertThat(html).contains("Actions");
    assertThat(html).contains("action=\"/dashboard/update\"");
    assertThat(html).contains("action=\"/dashboard/delete\"");
    assertThat(html).contains("action-button action-update");
    assertThat(html).contains("action-button action-delete");
  }

  @Test
  void deleteRemovesEntryFromDashboard() throws Exception {
    MvcResult saveResult =
        mockMvc
            .perform(
                post("/dashboard/save")
                    .session(authenticatedSession())
                    .param("name", "Buddy")
                    .param("type", "dog")
                    .param("breed", "Golden Retriever")
                    .param("age", "4")
                    .param("description", "Friendly dog")
                    .param("imageUrl", "/images/pets/buddy.jpg"))
            .andReturn();

    MockHttpSession session = (MockHttpSession) saveResult.getRequest().getSession(false);
    MvcResult beforeDelete =
        mockMvc.perform(get("/dashboard").session(session)).andExpect(status().isOk()).andReturn();
    String htmlBefore = beforeDelete.getResponse().getContentAsString();
    int idStart = htmlBefore.indexOf("name=\"id\" value=\"");
    assertThat(idStart).isGreaterThan(-1);
    String idPrefix = "name=\"id\" value=\"";
    String id =
        htmlBefore.substring(
            idStart + idPrefix.length(), htmlBefore.indexOf('"', idStart + idPrefix.length()));

    mockMvc
        .perform(post("/dashboard/delete").session(session).param("id", id))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/dashboard"));

    mockMvc
        .perform(get("/dashboard").session(session))
        .andExpect(status().isOk())
        .andExpect(content().string(org.hamcrest.Matchers.not(containsString("Buddy"))));
  }

  @Test
  void dashboardSortsByRequestedColumn() throws Exception {
    MvcResult firstSave =
        mockMvc
            .perform(
                post("/dashboard/save")
                    .session(authenticatedSession())
                    .param("name", "Zoe")
                    .param("type", "dog")
                    .param("breed", "Mix")
                    .param("age", "9")
                    .param("description", "Older dog")
                    .param("imageUrl", "")
                    .param("notes", "zzz"))
            .andReturn();

    MockHttpSession session = (MockHttpSession) firstSave.getRequest().getSession(false);

    mockMvc
        .perform(
            post("/dashboard/save")
                .session(session)
                .param("name", "Amy")
                .param("type", "dog")
                .param("breed", "Mix")
                .param("age", "1")
                .param("description", "Young dog")
                .param("imageUrl", "")
                .param("notes", "aaa"))
        .andReturn();

    MvcResult sortedByAge =
        mockMvc
            .perform(get("/dashboard").session(session).param("sort", "age").param("dir", "asc"))
            .andExpect(status().isOk())
            .andReturn();
    String ageHtml = sortedByAge.getResponse().getContentAsString();
    assertThat(ageHtml.indexOf("Amy")).isLessThan(ageHtml.indexOf("Zoe"));

    MvcResult sortedByNotes =
        mockMvc
            .perform(get("/dashboard").session(session).param("sort", "notes").param("dir", "asc"))
            .andExpect(status().isOk())
            .andReturn();
    String notesHtml = sortedByNotes.getResponse().getContentAsString();
    assertThat(notesHtml.indexOf("Amy")).isLessThan(notesHtml.indexOf("Zoe"));
  }

  private MockHttpSession authenticatedSession() {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("authUserId", 1L);
    session.setAttribute("authUserEmail", "user@example.com");
    return session;
  }
}
