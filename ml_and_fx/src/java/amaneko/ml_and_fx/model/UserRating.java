package amaneko.ml_and_fx.model;

import java.time.LocalDateTime;

/**
 * Класс для представления оценки пользователя
 */
public class UserRating {
    private int id;
    private int userId;
    private int movieId;
    private double rating; // от 1.0 до 5.0
    private String reviewText;
    private boolean published; // опубликована ли оценка
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean watchedOnly; // только просмотр, без оценки
    
    public UserRating() {}
    
    public UserRating(int userId, int movieId, double rating) {
        this.userId = userId;
        this.movieId = movieId;
        this.rating = rating;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public UserRating(int userId, int movieId, double rating, String reviewText) {
        this(userId, movieId, rating);
        this.reviewText = reviewText;
    }
    
    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public int getMovieId() { return movieId; }
    public void setMovieId(int movieId) { this.movieId = movieId; }
    
    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }
    
    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }
    
    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public boolean isWatchedOnly() { return watchedOnly; }
    public void setWatchedOnly(boolean watchedOnly) { this.watchedOnly = watchedOnly; }
    
    @Override
    public String toString() {
        return "UserRating{userId=" + userId + ", movieId=" + movieId + 
               ", rating=" + rating + ", review='" + reviewText + "'}";
    }
}
