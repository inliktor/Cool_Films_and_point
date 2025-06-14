package amaneko.ml_and_fx.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class ControllersSmokeTest {
    @Test
    void testMainControllerCreation() {
        MainController ctrl = new MainController();
        assertNotNull(ctrl);
    }

    @Test
    void testPreferencesControllerCreation() {
        PreferencesController ctrl = new PreferencesController();
        assertNotNull(ctrl);
    }

    @Test
    void testSimpleFilterControllerCreation() {
        SimpleFilterController ctrl = new SimpleFilterController();
        assertNotNull(ctrl);
    }

    @Test
    void testRatingDialogControllerCreation() {
        RatingDialogController ctrl = new RatingDialogController();
        assertNotNull(ctrl);
    }

    @Test
    void testLoginControllerCreation() {
        LoginController ctrl = new LoginController();
        assertNotNull(ctrl);
    }

    @Test
    void testMovieRecommendationAppCreation() {
        MovieRecommendationApp app = new MovieRecommendationApp();
        assertNotNull(app);
    }
}
