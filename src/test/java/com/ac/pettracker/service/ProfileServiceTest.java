package com.ac.pettracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import com.ac.pettracker.dto.ProfilePreferences;
import com.ac.pettracker.model.UserPreferences;
import com.ac.pettracker.repository.UserPreferencesRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

  @Mock private UserPreferencesRepository userPreferencesRepository;

  @InjectMocks private ProfileService profileService;

  private static final long USER_ID = 42L;

  @BeforeEach
  void setUp() {
    lenient().when(userPreferencesRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  // -------------------------------------------------------------------------
  // savePreferences — normalization
  // -------------------------------------------------------------------------

  @Test
  void savePreferencesNormalizesSpeciesToLowercase() {
    given(userPreferencesRepository.findByUserAccountId(USER_ID)).willReturn(Optional.empty());

    ProfilePreferences result = profileService.savePreferences(USER_ID, "Dog", "", "", "", "");

    assertThat(result.species()).isEqualTo("dog");
  }

  @Test
  void savePreferencesRejectsUnknownSpecies() {
    given(userPreferencesRepository.findByUserAccountId(USER_ID)).willReturn(Optional.empty());

    ProfilePreferences result = profileService.savePreferences(USER_ID, "hamster", "", "", "", "");

    assertThat(result.species()).isEmpty();
  }

  @Test
  void savePreferencesNormalizesGender() {
    given(userPreferencesRepository.findByUserAccountId(USER_ID)).willReturn(Optional.empty());

    assertThat(profileService.savePreferences(USER_ID, "", "male", "", "", "").gender())
        .isEqualTo("male");
    assertThat(profileService.savePreferences(USER_ID, "", "FEMALE", "", "", "").gender())
        .isEqualTo("female");
    assertThat(profileService.savePreferences(USER_ID, "", "other", "", "", "").gender()).isEmpty();
  }

  @Test
  void savePreferencesAcceptsValidWeightBands() {
    given(userPreferencesRepository.findByUserAccountId(USER_ID)).willReturn(Optional.empty());

    assertThat(profileService.savePreferences(USER_ID, "", "", "25-50lbs", "", "").weight())
        .isEqualTo("25-50lbs");
  }

  @Test
  void savePreferencesRejectsInvalidWeightBand() {
    given(userPreferencesRepository.findByUserAccountId(USER_ID)).willReturn(Optional.empty());

    assertThat(profileService.savePreferences(USER_ID, "", "", "heavyweight", "", "").weight())
        .isEmpty();
  }

  @Test
  void savePreferencesTruncatesBreedAt120Characters() {
    given(userPreferencesRepository.findByUserAccountId(USER_ID)).willReturn(Optional.empty());

    String longBreed = "x".repeat(200);
    ProfilePreferences result = profileService.savePreferences(USER_ID, "", "", "", longBreed, "");

    assertThat(result.breed()).hasSize(120);
  }

  @Test
  void savePreferencesTruncatesKeywordsAt500Characters() {
    given(userPreferencesRepository.findByUserAccountId(USER_ID)).willReturn(Optional.empty());

    String longKeywords = "x".repeat(600);
    ProfilePreferences result =
        profileService.savePreferences(USER_ID, "", "", "", "", longKeywords);

    assertThat(result.keywords()).hasSize(500);
  }

  @Test
  void savePreferencesTrimsWhitespace() {
    given(userPreferencesRepository.findByUserAccountId(USER_ID)).willReturn(Optional.empty());

    ProfilePreferences result =
        profileService.savePreferences(USER_ID, "  dog  ", "", "", "  beagle  ", "");

    assertThat(result.species()).isEqualTo("dog");
    assertThat(result.breed()).isEqualTo("beagle");
  }

  // -------------------------------------------------------------------------
  // savePreferences — persistence
  // -------------------------------------------------------------------------

  @Test
  void savePreferencesCreatesNewRecordWhenNoneExists() {
    given(userPreferencesRepository.findByUserAccountId(USER_ID)).willReturn(Optional.empty());

    profileService.savePreferences(USER_ID, "cat", "female", "<25 lbs", "tabby", "calm");

    verify(userPreferencesRepository).save(any(UserPreferences.class));
  }

  @Test
  void savePreferencesUpdatesExistingRecord() {
    UserPreferences existing = new UserPreferences(USER_ID);
    given(userPreferencesRepository.findByUserAccountId(USER_ID)).willReturn(Optional.of(existing));

    profileService.savePreferences(USER_ID, "dog", "male", "25-50lbs", "lab", "active");

    verify(userPreferencesRepository).save(existing);
  }

  @Test
  void savePreferencesSetsNullForBlankFields() {
    given(userPreferencesRepository.findByUserAccountId(USER_ID)).willReturn(Optional.empty());
    UserPreferences[] saved = new UserPreferences[1];
    given(userPreferencesRepository.save(any()))
        .willAnswer(
            inv -> {
              saved[0] = inv.getArgument(0);
              return saved[0];
            });

    profileService.savePreferences(USER_ID, "", "", "", "", "");

    assertThat(saved[0].getPreferredSpecies()).isNull();
    assertThat(saved[0].getPreferredGender()).isNull();
    assertThat(saved[0].getPreferredBreed()).isNull();
  }

  // -------------------------------------------------------------------------
  // populateSessionPreferences
  // -------------------------------------------------------------------------

  @Test
  void populateSessionPreferencesSetsSessionAttributesFromDb() {
    UserPreferences prefs = new UserPreferences(USER_ID);
    prefs.setPreferredSpecies("dog");
    prefs.setPreferredGender("male");
    prefs.setPreferredBreed("beagle");
    given(userPreferencesRepository.findByUserAccountId(USER_ID)).willReturn(Optional.of(prefs));

    MockHttpSession session = new MockHttpSession();
    profileService.populateSessionPreferences(USER_ID, session);

    assertThat(session.getAttribute("profileSpecies")).isEqualTo("dog");
    assertThat(session.getAttribute("profileGender")).isEqualTo("male");
    assertThat(session.getAttribute("profileBreed")).isEqualTo("beagle");
  }

  @Test
  void populateSessionPreferencesConvertsNullDbValuesToEmptyString() {
    UserPreferences prefs = new UserPreferences(USER_ID);
    // all fields null
    given(userPreferencesRepository.findByUserAccountId(USER_ID)).willReturn(Optional.of(prefs));

    MockHttpSession session = new MockHttpSession();
    profileService.populateSessionPreferences(USER_ID, session);

    assertThat(session.getAttribute("profileSpecies")).isEqualTo("");
  }

  @Test
  void populateSessionPreferencesDoesNothingWhenNoPreferencesExist() {
    given(userPreferencesRepository.findByUserAccountId(USER_ID)).willReturn(Optional.empty());

    MockHttpSession session = new MockHttpSession();
    profileService.populateSessionPreferences(USER_ID, session);

    assertThat(session.getAttribute("profileSpecies")).isNull();
  }
}
