package amaneko.ml_and_fx.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class UserPreferencesTest {
    @Test
    void testDefaultPreferences() {
        UserPreferences prefs = new UserPreferences(1);
        assertEquals(1, prefs.getUserId());
        assertTrue(prefs.getPreferredGenres().isEmpty());
        assertTrue(prefs.isIncludeWatchedMovies());
        assertTrue(prefs.getMinRating() >= 0);
    }

    @Test
    void testAddRemoveGenre() {
        UserPreferences prefs = new UserPreferences(1);
        Genre g = new Genre(1, "Action");
        prefs.addPreferredGenre(g);
        assertTrue(prefs.getPreferredGenres().contains(g));
        prefs.removePreferredGenre(g);
        assertFalse(prefs.getPreferredGenres().contains(g));
    }
}
