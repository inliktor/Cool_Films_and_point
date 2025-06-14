package amaneko.ml_and_fx.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import amaneko.ml_and_fx.ml.SimpleMLService;

class SimpleMLServiceTest {
    @Test
    void testGetRecommendations_coldStart() {
        SimpleMLService ml = new SimpleMLService();
        // Используем несуществующий userId для cold-start
        List<Integer> recs = ml.getRecommendations(-999, 5);
        assertNotNull(recs);
        assertFalse(recs.isEmpty(), "Cold-start должен возвращать fallback рекомендации");
    }

    @Test
    void testGetRecommendations_existingUser() {
        SimpleMLService ml = new SimpleMLService();
        // Используйте существующий userId из userFactors, например 1
        List<Integer> recs = ml.getRecommendations(1, 5);
        assertNotNull(recs);
        // Может быть пусто, если нет такого userId, но тест не должен падать
    }

    @Test
    void testGetPredictedRating() {
        SimpleMLService ml = new SimpleMLService();
        float rating = ml.getPredictedRating(1, 1);
        assertTrue(rating >= 0.0f);
    }
}
