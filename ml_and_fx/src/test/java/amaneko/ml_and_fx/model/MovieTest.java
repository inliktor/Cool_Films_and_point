package amaneko.ml_and_fx.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class MovieTest {
    @Test
    void testMovieFields() {
        Movie m = new Movie(1, "Test", "desc", 7.5, 100.0, 2020, "poster.jpg", "Action,Comedy");
        assertEquals(1, m.getId());
        assertEquals("Test", m.getTitle());
        assertEquals("desc", m.getOverview());
        assertEquals(2020, m.getReleaseYear());
        assertEquals(7.5, m.getVoteAverage());
        assertEquals("poster.jpg", m.getPosterPath());
        assertEquals("Action,Comedy", m.getGenres());
        assertNotNull(m.toString());
    }
}
