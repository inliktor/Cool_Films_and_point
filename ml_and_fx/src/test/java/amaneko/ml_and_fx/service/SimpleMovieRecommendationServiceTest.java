package amaneko.ml_and_fx.service;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import amaneko.ml_and_fx.model.Movie;

public class SimpleMovieRecommendationServiceTest {

    @Test
    void testGetRecommendations() {
        SimpleMovieRecommendationService service = new SimpleMovieRecommendationService();
        // Assuming user ID 1 and requesting 5 recommendations
        List<Movie> recommendations = service.getRecommendationsForUser(1, 5);
        assertNotNull(recommendations, "Рекомендации не должны быть null");
        assertFalse(recommendations.isEmpty(), "Список рекомендаций не должен быть пустым");
        // Optionally, you can print the recommendations to see what was returned
        // recommendations.forEach(movie -> System.out.println(movie.getTitle()));
    }
}
