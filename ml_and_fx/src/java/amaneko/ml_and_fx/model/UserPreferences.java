package amaneko.ml_and_fx.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс для хранения предпочтений пользователя
 */
public class UserPreferences {
    private int userId;
    private List<Genre> preferredGenres;
    private boolean includeWatchedMovies; // показывать ли просмотренные фильмы как "к пересмотру"
    private double minRating; // минимальный рейтинг для рекомендаций
    
    public UserPreferences(int userId) {
        this.userId = userId;
        this.preferredGenres = new ArrayList<>();
        this.includeWatchedMovies = true;
        this.minRating = 5.0; // по умолчанию фильмы с рейтингом выше 6.0
    }
    
    // Геттеры и сеттеры
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public List<Genre> getPreferredGenres() { return preferredGenres; }
    public void setPreferredGenres(List<Genre> preferredGenres) { this.preferredGenres = preferredGenres; }
    
    public boolean isIncludeWatchedMovies() { return includeWatchedMovies; }
    public void setIncludeWatchedMovies(boolean includeWatchedMovies) { this.includeWatchedMovies = includeWatchedMovies; }
    
    public double getMinRating() { return minRating; }
    public void setMinRating(double minRating) { this.minRating = minRating; }
    
    // Утилитарные методы
    public void addPreferredGenre(Genre genre) {
        if (!preferredGenres.contains(genre)) {
            preferredGenres.add(genre);
        }
    }
    
    public void removePreferredGenre(Genre genre) {
        preferredGenres.remove(genre);
    }
    
    public boolean hasPreferredGenres() {
        return !preferredGenres.isEmpty();
    }
    
    public List<Integer> getPreferredGenreIds() {
        List<Integer> ids = new ArrayList<>();
        for (Genre genre : preferredGenres) {
            ids.add(genre.getId());
        }
        return ids;
    }
}
