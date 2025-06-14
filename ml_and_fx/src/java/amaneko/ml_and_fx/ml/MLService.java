package amaneko.ml_and_fx.ml;

import java.util.List;

import amaneko.ml_and_fx.model.UserPreferences;

public interface MLService {
    List<Integer> getRecommendations(int userId, int count);
    float getPredictedRating(int userId, int movieId);
    void updateViewingHistory(int userId, int movieId);
    void updateUserPreferences(int userId, UserPreferences preferences);
    boolean isModelLoaded();
    void shutdown();
}
