package amaneko.ml_and_fx.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class GenreTest {
    @Test
    void testGenreFields() {
        Genre g = new Genre(1, "Action");
        assertEquals(1, g.getId());
        assertEquals("Action", g.getName());
    }
}
