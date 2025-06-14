package amaneko.ml_and_fx.model;

import java.util.ArrayList;
import java.util.List;

public class Movie {
    private int id;
    private String title;
    private String overview;
    private double voteAverage;
    private double popularity;
    private int releaseYear;
    private String posterPath;
    private String genres;
    private float predictedRating;
    private UserRating userRating;
    private boolean isWatched;
    private boolean isRewatch;

    public Movie() {}

    public Movie(int id, String title, String overview, double voteAverage,
                 double popularity, int releaseYear, String posterPath, String genres) {
        this.id = id;
        this.title = title;
        this.overview = overview;
        this.voteAverage = voteAverage;
        this.popularity = popularity;
        this.releaseYear = releaseYear;
        this.posterPath = posterPath;
        this.genres = genres;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    public double getVoteAverage() { return voteAverage; }
    public void setVoteAverage(double voteAverage) { this.voteAverage = voteAverage; }
    public double getPopularity() { return popularity; }
    public void setPopularity(double popularity) { this.popularity = popularity; }
    public int getReleaseYear() { return releaseYear; }
    public void setReleaseYear(int releaseYear) { this.releaseYear = releaseYear; }
    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
    public String getGenres() { return genres; }
    public void setGenres(String genres) { this.genres = genres; }
    public float getPredictedRating() { return predictedRating; }
    public void setPredictedRating(float predictedRating) { this.predictedRating = predictedRating; }
    public UserRating getUserRating() { return userRating; }
    public void setUserRating(UserRating userRating) { this.userRating = userRating; }
    public boolean isWatched() { return isWatched; }
    public void setWatched(boolean watched) { isWatched = watched; }
    public boolean isRewatch() { return isRewatch; }
    public void setRewatch(boolean rewatch) { isRewatch = rewatch; }

    public boolean hasUserRating() {
        return userRating != null;
    }

    public double getUserRatingValue() {
        return userRating != null ? userRating.getRating() : 0.0;
    }

    public String getFullPosterUrl() {
        if (posterPath == null || posterPath.isEmpty()) {
            return null;
        }
        return "https://image.tmdb.org/t/p/w300_and_h450_bestv2" + posterPath;
    }

    public List<Genre> getGenresList() {
        if (genres == null || genres.isEmpty()) {
            return new ArrayList<>();
        }
        List<Genre> genreList = new ArrayList<>();
        String[] genreNames = genres.split(", ");
        for (String genreName : genreNames) {
            genreList.add(new Genre(0, genreName.trim()));
        }
        return genreList;
    }

    @Override
    public String toString() {
        String status = "";
        if (isWatched && hasUserRating()) {
            status = " ✅ " + String.format("%.1f", getUserRatingValue());
        } else if (isRewatch) {
            status = " 🔄 К пересмотру";
        } else if (predictedRating > 0) {
            status = " ⭐ " + String.format("%.1f", predictedRating);
        } else {
            status = " ⭐ " + String.format("%.1f", voteAverage);
        }
        return title + " (" + releaseYear + ")" + status;
    }
}
