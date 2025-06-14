package amaneko.ml_and_fx.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import amaneko.ml_and_fx.model.Movie;

class SimpleMovieRecommendationServiceSmokeTest {
    @Test
    void testGetRecommendationsForUser() {
        SimpleMovieRecommendationService service = new SimpleMovieRecommendationService();
        List<Movie> recs = service.getRecommendationsForUser(1, 3);
        assertNotNull(recs);
        assertFalse(recs.isEmpty(), "Должны быть рекомендации для пользователя 1");
    }

    @Test
    void testGetRecommendationsForUnknownUser() {
        SimpleMovieRecommendationService service = new SimpleMovieRecommendationService();
        // -99999 гарантированно несуществующий пользователь
        List<Movie> recs = service.getRecommendationsForUser(-99999, 5);
        assertNotNull(recs);
        assertFalse(recs.isEmpty(), "Должны быть fallback-рекомендации для cold-start пользователя");
    }

    @Test
    void testGetRecommendationsDifferentCounts() {
        SimpleMovieRecommendationService service = new SimpleMovieRecommendationService();
        List<Movie> recs3 = service.getRecommendationsForUser(1, 3);
        List<Movie> recs7 = service.getRecommendationsForUser(1, 7);
        assertNotNull(recs3);
        assertNotNull(recs7);
        assertTrue(recs3.size() <= 3);
        assertTrue(recs7.size() <= 7);
    }

    @Test
    void testGetRecommendationsNoPreferences() {
        SimpleMovieRecommendationService service = new SimpleMovieRecommendationService();
        // Предположим, что userId=2 не имеет предпочтений (или используйте подходящий id)
        List<Movie> recs = service.getRecommendationsForUser(2, 5);
        assertNotNull(recs);
        assertFalse(recs.isEmpty());
    }

    @Test
    void testGetPersonalizedRecommendations() {
        SimpleMovieRecommendationService service = new SimpleMovieRecommendationService();
        List<Movie> recs = service.getPersonalizedRecommendations(1, 5);
        assertNotNull(recs);
        // Может быть пусто, если нет предпочтений, но не должно падать
    }

    @Test
    void testRecommendationsAreUnique() {
        SimpleMovieRecommendationService service = new SimpleMovieRecommendationService();
        List<Movie> recs = service.getRecommendationsForUser(1, 20);
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (Movie m : recs) {
            assertTrue(ids.add(m.getId()), "В рекомендациях не должно быть дубликатов");
        }
    }

    @Test
    void testNoNegativeMovieIds() {
        SimpleMovieRecommendationService service = new SimpleMovieRecommendationService();
        List<Movie> recs = service.getRecommendationsForUser(1, 10);
        for (Movie m : recs) {
            assertTrue(m.getId() > 0, "ID фильма должен быть положительным");
        }
    }
}
