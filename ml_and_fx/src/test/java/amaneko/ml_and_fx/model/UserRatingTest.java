package amaneko.ml_and_fx.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class UserRatingTest {
    @Test
    void testUserRatingFields() {
        UserRating r = new UserRating(1, 2, 4.5, "Отличный фильм");
        r.setPublished(true);
        assertEquals(1, r.getUserId());
        assertEquals(2, r.getMovieId());
        assertEquals(4.5, r.getRating());
        assertEquals("Отличный фильм", r.getReviewText());
        assertTrue(r.isPublished());
    }
}
