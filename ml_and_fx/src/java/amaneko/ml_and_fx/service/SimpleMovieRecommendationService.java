package amaneko.ml_and_fx.service;

import java.sql.Connection;
import java.sql.DriverManager; // Added import
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException; // Added import
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import amaneko.ml_and_fx.ml.MLService;
import amaneko.ml_and_fx.ml.SimpleMLService;
import amaneko.ml_and_fx.model.Genre;
import amaneko.ml_and_fx.model.Movie;
import amaneko.ml_and_fx.model.UserPreferences;
import amaneko.ml_and_fx.model.UserRating;

/**
 * Простой и надежный сервис рекомендаций фильмов
 */
public class SimpleMovieRecommendationService {

    private final Connection dbConnection;
    private final MLService mlService; // Changed type to MLService
    private int currentUserId = 1; // ID текущего пользователя
    
    // Кэш предпочтений пользователей
    private final Map<Integer, UserPreferences> userPreferencesCache = new HashMap<>();
    
    // Отслеживание показанных фильмов (для текущей сессии, основной источник - DB)
    private final Map<Integer, Set<Integer>> shownMoviesCache = new HashMap<>();

    // Updated constructor
    public SimpleMovieRecommendationService() {
        this.dbConnection = connectToDatabase();
        this.mlService = new SimpleMLService(this); // Pass self for movie description filtering
        // Удалён вызов createTablesIfNotExist();
    }

    // New method to establish database connection
    private Connection connectToDatabase() {
        try {
            // Database connection details (consider making these configurable)
            String dbUrl = "jdbc:postgresql://localhost:15432/postgres";
            String dbUser = "zwloader";
            String dbPassword = "0010085070Pgsql";
            Connection connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
            System.out.println("✅ SimpleMovieRecommendationService: Подключение к БД установлено");
            return connection;
        } catch (SQLException e) {
            System.err.println("❌ SimpleMovieRecommendationService: Ошибка подключения к БД: " + e.getMessage());
            // stack trace removed for compliance
            throw new RuntimeException("Failed to connect to the database in SimpleMovieRecommendationService", e);
        }
    }
    
    // Getter for the database connection
    public Connection getDbConnection() {
        return dbConnection;
    }
    
    /**
     * Установить текущего пользователя
     */
    public void setCurrentUser(int userId) {
        this.currentUserId = userId;
    }
    
    /**
     * Получить ID текущего пользователя
     */
    public int getCurrentUserId() {
        return currentUserId;
    }
    
    /**
     * Главный метод получения рекомендаций (только ML, просто)
     * Адаптирован для последовательного получения уникальных рекомендаций:
     * - При первом вызове возвращает обычные рекомендации
     * - При последующих вызовах возвращает только новые рекомендации, не показанные ранее
     * - Если уникальных рекомендаций из ML нет, берет из БД
     */
    public List<Movie> getRecommendationsForUser(int userId, int count) {
        System.out.println("Получаем рекомендации для пользователя: " + userId);
        
        List<Movie> recommendations = new ArrayList<>();
        Set<Integer> recommendedIds = new HashSet<>(); // Для отслеживания уникальных ID
        
        // Получаем список уже рекомендованных в текущей сессии фильмов
        Set<Integer> previouslyRecommendedMovies = getSessionRecommendedMovies(userId);
        System.out.println("🔍 Уже рекомендовано в этой сессии: " + previouslyRecommendedMovies.size() + " фильмов");
        
        UserPreferences preferences = getUserPreferences(userId);
        boolean hasPreferredGenres = !preferences.getPreferredGenres().isEmpty();
        
        if (hasPreferredGenres) {
            System.out.println("📊 У пользователя есть предпочтительные жанры. Генерируем персональные рекомендации.");
            // 1. Пробуем получить ML рекомендации с учетом жанров
            if (mlService.isModelLoaded()) {
                try {
                    // Запрашиваем больше рекомендаций, так как часть может быть отфильтрована
                    List<Integer> movieIds = mlService.getRecommendations(userId, count * 8); 
                    Collections.shuffle(movieIds); 

                    List<String> preferredGenres = preferences.getPreferredGenres().stream()
                        .map(Genre::getName)
                        .toList();
                    double minRating = preferences.getMinRating();
                    boolean includeWatched = preferences.isIncludeWatchedMovies();
                    Set<Integer> watchedMovies = getShownMovies(userId); 

                    System.out.println("🤖 Получено " + movieIds.size() + " ID от ML. Применяем фильтры...");

                    for (Integer movieId : movieIds) {
                        if (recommendations.size() >= count) break;
                        if (recommendedIds.contains(movieId)) continue;
                        if (previouslyRecommendedMovies.contains(movieId)) continue; // Пропускаем ранее рекомендованные
                        if (!includeWatched && watchedMovies.contains(movieId)) continue; // Исключаем просмотренные

                        Movie movie = getMovieById(movieId);
                        if (movie != null) {
                            float predictedRating = mlService.getPredictedRating(userId, movieId);
                            movie.setPredictedRating(predictedRating);

                            boolean genreOk = preferredGenres.isEmpty() ||
                                (movie.getGenres() != null && preferredGenres.stream().anyMatch(g -> movie.getGenres().contains(g)));
                            boolean ratingOk = movie.getVoteAverage() >= minRating;

                            if (genreOk && ratingOk) {
                                recommendations.add(movie);
                                recommendedIds.add(movieId);
                            }
                        }
                    }
                    System.out.println("🤖 После ML и фильтров: " + recommendations.size());

                } catch (Exception e) {
                    System.err.println("Ошибка ML рекомендаций: " + e.getMessage());
                }
            }
            
            // 2. Если ML рекомендаций недостаточно или нет, добираем из персональных из БД
            if (recommendations.size() < count) {
                int neededCount = count - recommendations.size();
                System.out.println("📊 Добираем " + neededCount + " фильмов из персональных рекомендаций из БД...");

                List<Movie> personalizedFallback = getPersonalizedRecommendations(userId, neededCount * 3);

                for (Movie movie : personalizedFallback) {
                    if (recommendations.size() >= count) break;
                    if (!recommendedIds.contains(movie.getId()) && !previouslyRecommendedMovies.contains(movie.getId())) {
                        recommendations.add(movie);
                        recommendedIds.add(movie.getId());
                    }
                }
                System.out.println("📊 После добора из персональных (из БД): " + recommendations.size());
            }

        } else {
            System.out.println("🔥 У пользователя нет предпочтительных жанров. Генерируем общие рекомендации.");
            // 1. Пробуем получить ML рекомендации (общие)
            if (mlService.isModelLoaded()) {
                try {
                    List<Integer> movieIds = mlService.getRecommendations(userId, count * 8); 
                    Collections.shuffle(movieIds); 

                    UserPreferences userPrefs = getUserPreferences(userId);
                    boolean includeWatched = userPrefs.isIncludeWatchedMovies();
                    Set<Integer> watchedMovies = getShownMovies(userId); 

                    System.out.println("🤖 Получено " + movieIds.size() + " ID от ML. Применяем фильтры...");

                    for (Integer movieId : movieIds) {
                        if (recommendations.size() >= count) break;
                        if (recommendedIds.contains(movieId)) continue;
                        if (previouslyRecommendedMovies.contains(movieId)) continue; // Пропускаем ранее рекомендованные
                        if (!includeWatched && watchedMovies.contains(movieId)) continue; // Исключаем просмотренные

                        Movie movie = getMovieById(movieId);
                        if (movie != null) {
                            float predictedRating = mlService.getPredictedRating(userId, movieId);
                            movie.setPredictedRating(predictedRating);
                            recommendations.add(movie);
                            recommendedIds.add(movie.getId());
                        }
                    }
                    System.out.println("🤖 После ML (общих): " + recommendations.size());

                } catch (Exception e) {
                    System.err.println("Ошибка ML рекомендаций: " + e.getMessage());
                }
            }

            // 2. Если ML рекомендаций недостаточно, добираем из популярных
            if (recommendations.size() < count) {
                int neededCount = count - recommendations.size();
                System.out.println("🔥 Добираем " + neededCount + " фильмов из популярных...");
                List<Movie> popularFallback = getPopularMovies(neededCount * 3); 
                
                UserPreferences userPrefs = getUserPreferences(userId);
                boolean includeWatched = userPrefs.isIncludeWatchedMovies();
                Set<Integer> watchedMovies = getShownMovies(userId); 

                for (Movie movie : popularFallback) {
                    if (recommendations.size() >= count) break;
                    if (!recommendedIds.contains(movie.getId()) && 
                        !previouslyRecommendedMovies.contains(movie.getId()) && 
                        (includeWatched || !watchedMovies.contains(movie.getId()))) {
                        recommendations.add(movie);
                        recommendedIds.add(movie.getId());
                    }
                }
                System.out.println("🔥 После добора из популярных: " + recommendations.size());
            }
        }
        
        if (recommendations.isEmpty() && !previouslyRecommendedMovies.isEmpty()) {
            System.out.println("⚠️ Не найдено новых уникальных рекомендаций. Сбрасываем историю сессии и пробуем снова.");
            // Если не нашлось уникальных рекомендаций и была прошлая сессия - сбрасываем и ищем снова
            clearSessionRecommendations(userId);
            return getRecommendationsForUser(userId, count); // Рекурсивный вызов, но уже с чистой историей
        }
        
        // Отмечаем полученные фильмы как рекомендованные в этой сессии
        markMoviesAsRecommended(userId, recommendations);
        
        System.out.println("✅ Итого получено " + recommendations.size() + " новых рекомендаций");
        return recommendations;
    }
    
    /**
     * ML-основанные рекомендации (убраны, так как включены в getRecommendationsForUser)
     */
    // private List<Movie> getMLBasedRecommendations(int userId, int count) {
    //     ...
    // }
    
    /**
     * Персональные рекомендации на основе жанров и рейтингов
     */
    public List<Movie> getPersonalizedRecommendations(int userId, int count) {
        List<Movie> recommendations = new ArrayList<>();
        Set<Integer> watchedMovies = getShownMovies(userId); // Теперь из БД и кэша
        
        try {
            UserPreferences preferences = getUserPreferences(userId);
            
            if (preferences != null && !preferences.getPreferredGenres().isEmpty()) {
                StringBuilder queryBuilder = new StringBuilder();
                queryBuilder.append("""
                    WITH movie_data AS (
                        SELECT m.id, m.title, m.overview, m.vote_average, 
                               m.popularity, m.release_year, m.poster_path,
                               STRING_AGG(g.name, ', ') as genres,
                               COUNT(DISTINCT g.id) as genre_count
                        FROM movies m
                        LEFT JOIN movie_genres mg ON m.id = mg.movie_id
                        LEFT JOIN genres g ON mg.genre_id = g.id
                        WHERE m.vote_average >= ?
                """);
                
                if (!preferences.getPreferredGenres().isEmpty()) {
                    queryBuilder.append(" AND g.name IN (");
                    for (int i = 0; i < preferences.getPreferredGenres().size(); i++) {
                        if (i > 0) queryBuilder.append(", ");
                        queryBuilder.append("?");
                    }
                    queryBuilder.append(")");
                }
                
                // Исключаем уже просмотренные фильмы, если не включено их отображение
                if (!preferences.isIncludeWatchedMovies() && !watchedMovies.isEmpty()) {
                    queryBuilder.append(" AND m.id NOT IN (");
                    for (int i = 0; i < watchedMovies.size(); i++) {
                        if (i > 0) queryBuilder.append(", ");
                        queryBuilder.append("?");
                    }
                    queryBuilder.append(")");
                }
                
                queryBuilder.append("""
                    GROUP BY m.id, m.title, m.overview, m.vote_average, 
                             m.popularity, m.release_year, m.poster_path
                    )
                    SELECT id, title, overview, vote_average, 
                           popularity, release_year, poster_path, genres
                    FROM movie_data
                    ORDER BY genre_count DESC, vote_average DESC, popularity DESC
                    LIMIT ?
                """);
                
                try (PreparedStatement stmt = dbConnection.prepareStatement(queryBuilder.toString())) {
                    int paramIndex = 1;
                    
                    stmt.setDouble(paramIndex++, preferences.getMinRating());
                    
                    for (Genre genre : preferences.getPreferredGenres()) {
                        stmt.setString(paramIndex++, genre.getName());
                    }
                    
                    if (!preferences.isIncludeWatchedMovies()) { // Добавляем параметры только если фильтруем
                        for (Integer movieId : watchedMovies) {
                            stmt.setInt(paramIndex++, movieId);
                        }
                    }
                    
                    stmt.setInt(paramIndex++, count);
                    
                    recommendations = executeMovieQuery(stmt);
                    
                    if (!recommendations.isEmpty()) {
                        // Mark as shown in current session, DB persistence is through recordMovieView
                        // markMoviesAsShown(userId, recommendations); // Убрано, так как показано в getRecommendationsForUser
                    }
                    
                    System.out.println("📊 Персональные рекомендации: " + recommendations.size());
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Ошибка персональных рекомендаций: " + e.getMessage());
        }
        
        return recommendations;
    }
    
    /**
     * Популярные фильмы (fallback) - теперь учитывает предпочтения по просмотренным
     */
    public List<Movie> getPopularMovies(int count) {
        List<Movie> movies = new ArrayList<>();
        UserPreferences preferences = getUserPreferences(currentUserId);
        boolean includeWatched = preferences.isIncludeWatchedMovies();
        Set<Integer> watchedMovies = getShownMovies(currentUserId);

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("""
            SELECT m.id, m.title, m.overview, m.vote_average, 
                   m.popularity, m.release_year, m.poster_path,
                   STRING_AGG(g.name, ', ') as genres
            FROM movies m
            LEFT JOIN movie_genres mg ON m.id = mg.movie_id
            LEFT JOIN genres g ON mg.genre_id = g.id
            WHERE m.vote_average > 7.0 AND m.popularity > 50
        """);

        if (!includeWatched && !watchedMovies.isEmpty()) {
            queryBuilder.append(" AND m.id NOT IN (");
            for (int i = 0; i < watchedMovies.size(); i++) {
                if (i > 0) queryBuilder.append(", ");
                queryBuilder.append("?");
            }
            queryBuilder.append(")");
        }

        queryBuilder.append("""
            GROUP BY m.id, m.title, m.overview, m.vote_average, 
                     m.popularity, m.release_year, m.poster_path
            ORDER BY RANDOM(), m.popularity DESC, m.vote_average DESC
            LIMIT ?
            """);
        
        try (PreparedStatement stmt = dbConnection.prepareStatement(queryBuilder.toString())) {
            int paramIndex = 1;
            if (!includeWatched && !watchedMovies.isEmpty()) {
                for (Integer movieId : watchedMovies) {
                    stmt.setInt(paramIndex++, movieId);
                }
            }
            stmt.setInt(paramIndex++, count);
            movies = executeMovieQuery(stmt);
            System.out.println("🔥 Популярные фильмы: " + movies.size());
        } catch (SQLException e) {
            System.err.println("Ошибка получения популярных фильмов: " + e.getMessage());
        }
        
        return movies;
    }
    
    /**
     * Объединенный метод для всех действий с фильмом (просмотр, оценка, публикация)
     * Заменяет дублированную логику из saveUserRating, markMovieAsWatched, и контроллера
     */
    public boolean processMovieAction(int userId, int movieId, MovieAction action, Double rating, String reviewText) {
        System.out.println("🎬 Обрабатываем действие с фильмом: " + action + " (пользователь=" + userId + ", фильм=" + movieId + ")");
        
        try {
            return switch (action) {
                case RATE_ONLY -> saveRatingOnly(userId, movieId, rating, reviewText);
                case RATE_AND_PUBLISH -> saveRatingAndPublish(userId, movieId, rating, reviewText);
                case WATCH_ONLY -> markAsWatchedOnly(userId, movieId);
                default -> {
                    System.err.println("❌ Неизвестное действие: " + action);
                    yield false;
                }
            };
        } catch (Exception e) {
            System.err.println("❌ Ошибка при обработке действия с фильмом: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Enum для типов действий с фильмом
     */
    public enum MovieAction {
        RATE_ONLY,          // Только оценка (handleSave)
        RATE_AND_PUBLISH,   // Оценка + публикация (handlePublish)
        WATCH_ONLY          // Только просмотр (handleWatched)
    }
    
    /**
     * Приватный метод для сохранения только оценки
     */
    private boolean saveRatingOnly(int userId, int movieId, Double rating, String reviewText) {
        UserRating userRating = new UserRating(userId, movieId, rating != null ? rating : 0.0, reviewText);
        userRating.setPublished(false);
        
        boolean success = saveUserRatingInternal(userRating);
        if (success) {
            updateMovieMetadata(userId, movieId, rating);
        }
        return success;
    }
    
    /**
     * Приватный метод для сохранения оценки с публикацией
     */
    private boolean saveRatingAndPublish(int userId, int movieId, Double rating, String reviewText) {
        if (reviewText == null || reviewText.trim().isEmpty()) {
            System.err.println("❌ Для публикации необходим текст отзыва");
            return false;
        }
        
        UserRating userRating = new UserRating(userId, movieId, rating != null ? rating : 0.0, reviewText);
        userRating.setPublished(true);
        
        boolean success = saveUserRatingInternal(userRating);
        if (success) {
            updateMovieMetadata(userId, movieId, rating);
        }
        return success;
    }
    
    /**
     * Приватный метод для отметки только просмотра
     */
    private boolean markAsWatchedOnly(int userId, int movieId) {
        // Записываем в user_views (единая точка для просмотров)
        recordMovieView(userId, movieId);
        
        // Обновляем ML и предпочтения
        updateMovieMetadata(userId, movieId, null);
        
        return true;
    }
    
    /**
     * Единый метод обновления метаданных фильма (ML, предпочтения, кэши)
     */
    private void updateMovieMetadata(int userId, int movieId, @SuppressWarnings("unused") Double rating) {
        try {
            // Обновляем историю просмотров в ML
            mlService.updateViewingHistory(userId, movieId);
            
            // Обновляем предпочтения пользователя, если есть данные о фильме
            // Параметр rating резервируется для будущих ML-функций, которые могут
            // учитывать оценку при обновлении предпочтений
            Movie movie = getMovieById(movieId);
            if (movie != null) {
                updateUserPreferences(userId, movie);
            }
            
            // Очищаем кэши для пересчета
            userPreferencesCache.remove(userId);
            
            // Добавляем в кэш просмотренных
            addToShownMoviesCache(userId, movieId);
            
            System.out.println("🧠 Метаданные фильма обновлены");
            
        } catch (Exception e) {
            System.err.println("⚠️ Ошибка при обновлении метаданных фильма: " + e.getMessage());
        }
    }
    
    /**
     * Внутренний метод сохранения оценки (без дублирования логики)
     */
    private boolean saveUserRatingInternal(UserRating rating) {
        System.out.println("💾 Сохраняем оценку: пользователь=" + rating.getUserId() + 
                          ", фильм=" + rating.getMovieId() + 
                          ", рейтинг=" + rating.getRating() + 
                          ", опубликована=" + rating.isPublished());
        
        String checkQuery = "SELECT id FROM user_ratings WHERE user_id = ? AND movie_id = ?";
        String insertQuery = """
            INSERT INTO user_ratings (user_id, movie_id, rating, review_text, published, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """;
        String updateQuery = """
            UPDATE user_ratings 
            SET rating = ?, review_text = ?, published = ?, updated_at = CURRENT_TIMESTAMP
            WHERE user_id = ? AND movie_id = ?
            """;
        
        try {
            boolean exists;
            try (PreparedStatement checkStmt = dbConnection.prepareStatement(checkQuery)) {
                checkStmt.setInt(1, rating.getUserId());
                checkStmt.setInt(2, rating.getMovieId());
                ResultSet rs = checkStmt.executeQuery();
                exists = rs.next();
            }
            
            PreparedStatement stmt;
            if (exists) {
                stmt = dbConnection.prepareStatement(updateQuery);
                stmt.setDouble(1, rating.getRating());
                stmt.setString(2, rating.getReviewText());
                stmt.setBoolean(3, rating.isPublished());
                stmt.setInt(4, rating.getUserId());
                stmt.setInt(5, rating.getMovieId());
                System.out.println("🔄 Обновляем существующую оценку");
            } else {
                stmt = dbConnection.prepareStatement(insertQuery);
                stmt.setInt(1, rating.getUserId());
                stmt.setInt(2, rating.getMovieId());
                stmt.setDouble(3, rating.getRating());
                stmt.setString(4, rating.getReviewText());
                stmt.setBoolean(5, rating.isPublished());
                System.out.println("➕ Создаем новую оценку");
            }
            
            int rowsAffected = stmt.executeUpdate();
            stmt.close();
            
            if (rowsAffected > 0) {
                System.out.println("✅ Оценка сохранена успешно");
                // Также записываем просмотр
                recordMovieView(rating.getUserId(), rating.getMovieId()); 
                return true;
            } else {
                System.out.println("⚠️ Оценка не сохранена");
                return false;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Ошибка сохранения оценки: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Отмечает фильм как просмотренный без оценки
     * 
     * @param userId ID пользователя
     * @param movieId ID фильма
     * @return true если операция успешна
     */
    public boolean markMovieAsWatched(int userId, int movieId) {
        try {
            // Проверяем, есть ли уже запись об этом фильме
            String checkSql = "SELECT id FROM user_movie_ratings WHERE user_id = ? AND movie_id = ?";
            PreparedStatement checkStmt = dbConnection.prepareStatement(checkSql);
            checkStmt.setInt(1, userId);
            checkStmt.setInt(2, movieId);
            ResultSet resultSet = checkStmt.executeQuery();
            
            if (resultSet.next()) {
                // Запись уже существует, обновляем флаг просмотра
                int ratingId = resultSet.getInt("id");
                String updateSql = "UPDATE user_movie_ratings SET is_watched = 1 WHERE id = ?";
                PreparedStatement updateStmt = dbConnection.prepareStatement(updateSql);
                updateStmt.setInt(1, ratingId);
                updateStmt.executeUpdate();
                
                System.out.println("📝 Обновлена запись о просмотре фильма (ID: " + movieId + ") для пользователя (ID: " + userId + ")");
            } else {
                // Создаем новую запись о просмотре без оценки
                String insertSql = "INSERT INTO user_movie_ratings (user_id, movie_id, is_watched, watched_date) VALUES (?, ?, 1, CURRENT_TIMESTAMP)";
                PreparedStatement insertStmt = dbConnection.prepareStatement(insertSql);
                insertStmt.setInt(1, userId);
                insertStmt.setInt(2, movieId);
                insertStmt.executeUpdate();
                
                System.out.println("✅ Создана новая запись о просмотре фильма (ID: " + movieId + ") для пользователя (ID: " + userId + ")");
            }
            
            // Обновляем информацию для нейросети
            mlService.updateViewingHistory(userId, movieId);
            
            // Добавляем в кэш показанных фильмов
            if (!shownMoviesCache.containsKey(userId)) {
                shownMoviesCache.put(userId, new HashSet<>());
            }
            shownMoviesCache.get(userId).add(movieId);
            
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Ошибка при отметке фильма как просмотренного: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Добавляет фильм в кэш просмотренных фильмов для данного пользователя
     * 
     * @param userId ID пользователя
     * @param movieId ID фильма
     */
    private void addToShownMoviesCache(int userId, int movieId) {
        // Получаем или создаем множество просмотренных фильмов для пользователя
        Set<Integer> userShownMovies = shownMoviesCache.computeIfAbsent(userId, k -> new HashSet<>());
        
        // Добавляем ID фильма в кэш просмотренных
        userShownMovies.add(movieId);
        
        System.out.println("📝 Фильм ID:" + movieId + " добавлен в кэш просмотренных для пользователя ID:" + userId);
    }
    
    /**
     * Обновляет предпочтения пользователя для ML-модели на основе просмотренного фильма
     * 
     * @param userId ID пользователя
     * @param movie Фильм, который был просмотрен
     */
    public void updateUserPreferences(int userId, Movie movie) {
        try {
            if (movie == null) return;
            
            // 1. Извлекаем жанры фильма
            String genres = movie.getGenres();
            List<Genre> movieGenres = new ArrayList<>();
            
            if (genres != null && !genres.isEmpty()) {
                String[] genreNames = genres.split(", ");
                for (String genreName : genreNames) {
                    movieGenres.add(new Genre(0, genreName.trim()));
                }
            }
            
            if (movieGenres.isEmpty()) {
                System.out.println("ℹ️ У фильма нет жанров для обновления предпочтений");
                return;
            }
            
            // 2. Загружаем текущие предпочтения пользователя
            UserPreferences userPreferences = getUserPreferences(userId);
            if (userPreferences == null) {
                // Создаем новые предпочтения, если они еще не существуют
                userPreferences = new UserPreferences(userId);
            }
            
            // 3. Обновляем предпочтения по жанрам на основе просмотренного фильма
            for (Genre genre : movieGenres) {
                // Увеличиваем вес жанра в предпочтениях пользователя
                userPreferences.addPreferredGenre(genre);
                System.out.println("➕ Увеличен вес жанра '" + genre.getName() + "' в предпочтениях пользователя");
            }
            
            // 4. Сохраняем обновленные предпочтения в базе данных
            saveUserPreferences(userPreferences);
            System.out.println("💾 Предпочтения пользователя обновлены на основе просмотренного фильма");
            
            // 5. Информируем ML-модель об обновлении предпочтений
            mlService.updateUserPreferences(userId, userPreferences);
            
        } catch (Exception e) {
            System.err.println("❌ Ошибка при обновлении предпочтений пользователя: " + e.getMessage());
        }
    }
    
    /**
     * Получить предпочтения пользователя
     */
    public UserPreferences getUserPreferences(int userId) {
        // Проверяем кэш
        if (userPreferencesCache.containsKey(userId)) {
            return userPreferencesCache.get(userId);
        }
        
        UserPreferences preferences = new UserPreferences(userId);
        
        // Получаем любимые жанры на основе высоких оценок ТОЛЬКО если пользователь ещё не выбирал жанры вручную
        // Эта логика должна быть перенесена в PreferencesController или явно активирована там.
        // Здесь мы просто загружаем сохраненные предпочтения или создаем дефолтные.
        // ВАЖНО: Если жанры выбираются вручную, они должны сохраняться в БД в таблице user_preferences_genres
        // Пока этой таблицы нет, предпочитаемые жанры будут вычисляться на лету или браться из UI.
        // Для демонстрации, оставлю вычисление из оценок, если жанры не заданы.
        String genreQuery = """
            SELECT g.name, AVG(ur.rating) as avg_rating, COUNT(*) as count
            FROM user_ratings ur
            JOIN movie_genres mg ON ur.movie_id = mg.movie_id
            JOIN genres g ON mg.genre_id = g.id
            WHERE ur.user_id = ? AND ur.rating >= 4.0
            GROUP BY g.id, g.name
            HAVING COUNT(*) >= 1
            ORDER BY avg_rating DESC, count DESC
            LIMIT 5
            """;
        try (PreparedStatement stmt = dbConnection.prepareStatement(genreQuery)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            List<Genre> preferredGenres = new ArrayList<>();
            while (rs.next()) {
                preferredGenres.add(new Genre(0, rs.getString("name")));
            }
            preferences.setPreferredGenres(preferredGenres);
            System.out.println("📊 Найдено предпочтений: " + preferredGenres.size() + " жанров (из оценок)");
        } catch (SQLException e) {
            System.err.println("Ошибка получения предпочтений: " + e.getMessage());
        }
        
        // Настройки по умолчанию
        preferences.setMinRating(6.0);
        preferences.setIncludeWatchedMovies(false); // По умолчанию не включать просмотренные
        
        // Сохраняем в кэш
        userPreferencesCache.put(userId, preferences);
        
        return preferences;
    }
    
    /**
     * Сохранить предпочтения пользователя
     */
    public void saveUserPreferences(UserPreferences preferences) {
        // Здесь также нужно сохранять жанры в БД, если они были выбраны вручную
        // В текущей реализации UserPreferences не сохраняются в БД, только в кэш.
        // Для полной реализации нужно добавить таблицу user_preferences и логику сохранения/загрузки.
        userPreferencesCache.put(preferences.getUserId(), preferences);
        System.out.println("✅ Предпочтения сохранены в кэш");
    }
    
    /**
     * Получить все жанры
     */
    public List<Genre> getAllGenres() {
        List<Genre> genres = new ArrayList<>();
        
        String query = "SELECT id, name FROM genres ORDER BY name";
        
        try (PreparedStatement stmt = dbConnection.prepareStatement(query)) {
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                genres.add(new Genre(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            System.err.println("Ошибка получения жанров: " + e.getMessage());
        }
        
        return genres;
    }
    
    /**
     * Получить оценку пользователя для фильма
     */
    public UserRating getUserRating(int userId, int movieId) {
        String query = """
            SELECT rating, review_text, created_at, updated_at
            FROM user_ratings 
            WHERE user_id = ? AND movie_id = ?
            """;
        
        try (PreparedStatement stmt = dbConnection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, movieId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                UserRating rating = new UserRating(
                    userId, 
                    movieId, 
                    rs.getDouble("rating"), 
                    rs.getString("review_text")
                );
                return rating;
            }
        } catch (SQLException e) {
            System.err.println("Ошибка получения оценки: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Поиск фильмов по названию
     */
    public List<Movie> searchMovies(String searchTerm, int limit) {
        List<Movie> movies = new ArrayList<>();
        UserPreferences preferences = getUserPreferences(currentUserId);
        boolean includeWatched = preferences.isIncludeWatchedMovies();
        Set<Integer> watchedMovies = getShownMovies(currentUserId);

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("""
            SELECT m.id, m.title, m.overview, m.vote_average, 
                   m.popularity, m.release_year, m.poster_path,
                   STRING_AGG(g.name, ', ') as genres
            FROM movies m
            LEFT JOIN movie_genres mg ON m.id = mg.movie_id
            LEFT JOIN genres g ON mg.genre_id = g.id
            WHERE LOWER(m.title) LIKE LOWER(?) OR LOWER(m.overview) LIKE LOWER(?)
        """);

        if (!includeWatched && !watchedMovies.isEmpty()) {
            queryBuilder.append(" AND m.id NOT IN (");
            for (int i = 0; i < watchedMovies.size(); i++) {
                if (i > 0) queryBuilder.append(", ");
                queryBuilder.append("?");
            }
            queryBuilder.append(")");
        }

        queryBuilder.append("""
            GROUP BY m.id, m.title, m.overview, m.vote_average, 
                     m.popularity, m.release_year, m.poster_path
            ORDER BY m.popularity DESC
            LIMIT ?
            """);

        try (PreparedStatement stmt = dbConnection.prepareStatement(queryBuilder.toString())) {
            int paramIndex = 1;
            stmt.setString(paramIndex++, "%" + searchTerm + "%");
            stmt.setString(paramIndex++, "%" + searchTerm + "%");
            if (!includeWatched && !watchedMovies.isEmpty()) {
                for (Integer movieId : watchedMovies) {
                    stmt.setInt(paramIndex++, movieId);
                }
            }
            stmt.setInt(paramIndex++, limit);
            movies = executeMovieQuery(stmt);
        } catch (SQLException e) {
            System.err.println("Ошибка поиска: " + e.getMessage());
        }

        return movies;
    }
    
    /**
     * Получить фильм по ID
     */
    public Movie getMovieById(int movieId) {
        String query = """
            SELECT m.id, m.title, m.overview, m.vote_average, 
                   m.popularity, m.release_year, m.poster_path,
                   STRING_AGG(g.name, ', ') as genres
            FROM movies m
            LEFT JOIN movie_genres mg ON m.id = mg.movie_id
            LEFT JOIN genres g ON mg.genre_id = g.id
            WHERE m.id = ?
            GROUP BY m.id, m.title, m.overview, m.vote_average, 
                     m.popularity, m.release_year, m.poster_path
            """;

        try (PreparedStatement stmt = dbConnection.prepareStatement(query)) {
            stmt.setInt(1, movieId);
            List<Movie> movies = executeMovieQuery(stmt);
            return movies.isEmpty() ? null : movies.get(0);
        } catch (SQLException e) {
            System.err.println("Ошибка получения фильма: " + e.getMessage());
        }

        return null;
    }
    
    /**
     * Загрузить фильм с данными пользователя (теперь учитывает user_views)
     */
    public Movie loadMovieWithUserData(int movieId, int userId) {
        Movie movie = getMovieById(movieId);
        if (movie != null) {
            // Проверяем, есть ли оценка пользователя
            UserRating userRating = getUserRating(userId, movieId);
            if (userRating != null) {
                movie.setUserRating(userRating);
                // Фильм считается просмотренным, если есть оценка
                movie.setWatched(true); 
                if (userRating.getRating() >= 4.0) {
                    movie.setRewatch(true);
                }
            } else {
                // Если оценки нет, проверяем user_views
                if (isMovieWatchedByUser(userId, movieId)) {
                    movie.setWatched(true);
                }
            }
        }
        return movie;
    }
    
    /**
     * Фильмы для повторного просмотра
     */
    public List<Movie> getRecommendationsForRewatch(int userId, int count) {
        List<Movie> recommendations = new ArrayList<>();
        
        String query = """
            SELECT m.id, m.title, m.overview, m.vote_average, 
                   m.popularity, m.release_year, m.poster_path,
                   STRING_AGG(g.name, ', ') as genres,
                   ur.rating as user_rating
            FROM movies m
            LEFT JOIN movie_genres mg ON m.id = mg.movie_id
            LEFT JOIN genres g ON mg.genre_id = g.id
            INNER JOIN user_ratings ur ON m.id = ur.movie_id
            WHERE ur.user_id = ? AND ur.rating >= 4
            GROUP BY m.id, m.title, m.overview, m.vote_average, 
                     m.popularity, m.release_year, m.poster_path, ur.rating
            ORDER BY RANDOM(), ur.rating DESC, m.vote_average DESC
            LIMIT ?
            """;
        
        try (PreparedStatement stmt = dbConnection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, count);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Movie movie = new Movie(
                    rs.getInt("id"),
                    rs.getString("title"),
                    rs.getString("overview"),
                    rs.getDouble("vote_average"),
                    rs.getDouble("popularity"),
                    rs.getInt("release_year"),
                    rs.getString("poster_path"),
                    rs.getString("genres")
                );
                
                UserRating userRating = new UserRating(userId, movie.getId(), rs.getDouble("user_rating"), "");
                movie.setUserRating(userRating);
                movie.setWatched(true);
                movie.setRewatch(true);
                
                recommendations.add(movie);
            }
        } catch (SQLException e) {
            System.err.println("Ошибка получения фильмов для повтора: " + e.getMessage());
        }
        
        return recommendations;
    }
    
    /**
     * Получить все фильмы с ограничением количества - теперь учитывает user_views
     */
    public List<Movie> getAllMovies(int count, int userId) { // Добавлен userId
        List<Movie> movies = new ArrayList<>();
        UserPreferences preferences = getUserPreferences(userId); 
        boolean includeWatched = preferences.isIncludeWatchedMovies();
        Set<Integer> watchedMovies = getShownMovies(userId); 

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("""
            SELECT m.id, m.title, m.overview, m.vote_average, 
                   m.popularity, m.release_year, m.poster_path,
                   STRING_AGG(g.name, ', ') as genres
            FROM movies m
            LEFT JOIN movie_genres mg ON m.id = mg.movie_id
            LEFT JOIN genres g ON mg.genre_id = g.id
        """);

        if (!includeWatched && !watchedMovies.isEmpty()) {
            queryBuilder.append(" WHERE m.id NOT IN (");
            for (int i = 0; i < watchedMovies.size(); i++) {
                if (i > 0) queryBuilder.append(", ");
                queryBuilder.append("?");
            }
            queryBuilder.append(")");
        }

        queryBuilder.append("""
            GROUP BY m.id, m.title, m.overview, m.vote_average, 
                     m.popularity, m.release_year, m.poster_path
            ORDER BY m.popularity DESC, m.vote_average DESC
            LIMIT ?
            """);
        
        try (PreparedStatement stmt = dbConnection.prepareStatement(queryBuilder.toString())) {
            int paramIndex = 1;
            if (!includeWatched && !watchedMovies.isEmpty()) {
                for (Integer movieId : watchedMovies) {
                    stmt.setInt(paramIndex++, movieId);
                }
            }
            stmt.setInt(paramIndex++, count);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Movie movie = new Movie(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("overview"),
                        rs.getDouble("vote_average"),
                        rs.getDouble("popularity"),
                        rs.getInt("release_year"),
                        rs.getString("poster_path"),
                        ""
                    );
                    
                    String genres = rs.getString("genres");
                    movie.setGenres(genres != null ? genres : "");
                    
                    movies.add(movie);
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка получения всех фильмов: " + e.getMessage());
        }
        
        System.out.println("📊 Загружено " + movies.size() + " фильмов из getAllMovies");
        return movies;
    }
    
    /**
     * Получить фильмы, оцененные пользователем
     */
    public List<Movie> getRatedMovies(int userId) {
        List<Movie> movies = new ArrayList<>();
        
        String query = """
            SELECT m.id, m.title, m.overview, m.vote_average, 
                   m.popularity, m.release_year, m.poster_path,
                   STRING_AGG(g.name, ', ') as genres,
                   ur.rating, ur.review_text, ur.published
            FROM movies m
            LEFT JOIN movie_genres mg ON m.id = mg.movie_id
            LEFT JOIN genres g ON mg.genre_id = g.id
            INNER JOIN user_ratings ur ON m.id = ur.movie_id
            WHERE ur.user_id = ?
            GROUP BY m.id, m.title, m.overview, m.vote_average, 
                     m.popularity, m.release_year, m.poster_path,
                     ur.rating, ur.review_text, ur.published
            ORDER BY ur.rating DESC, m.vote_average DESC
            """;
        
        try (PreparedStatement stmt = dbConnection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Movie movie = new Movie(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("overview"),
                        rs.getDouble("vote_average"),
                        rs.getDouble("popularity"),
                        rs.getInt("release_year"),
                        rs.getString("poster_path"),
                        ""
                    );
                    
                    String genres = rs.getString("genres");
                    movie.setGenres(genres != null ? genres : "");
                    
                    // Добавляем пользовательскую оценку
                    UserRating userRating = new UserRating(
                        userId,
                        movie.getId(),
                        rs.getDouble("rating"),
                        rs.getString("review_text")
                    );
                    userRating.setPublished(rs.getBoolean("published"));
                    movie.setUserRating(userRating);
                    movie.setWatched(true); // Фильм считается просмотренным, если он оценен
                    
                    movies.add(movie);
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка получения оцененных фильмов: " + e.getMessage());
        }
        
        System.out.println("⭐ Загружено " + movies.size() + " оцененных фильмов");
        return movies;
    }

    /**
     * Получить избранные фильмы пользователя
     */
    public List<Movie> getFavoriteMovies(int userId) {
        List<Movie> movies = new ArrayList<>();
        String query = """
            SELECT m.id, m.title, m.overview, m.vote_average, 
                   m.popularity, m.release_year, m.poster_path,
                   STRING_AGG(g.name, ', ') as genres,
                   uf.added_at
            FROM user_favorites uf
            JOIN movies m ON uf.movie_id = m.id
            LEFT JOIN movie_genres mg ON m.id = mg.movie_id
            LEFT JOIN genres g ON mg.genre_id = g.id
            WHERE uf.user_id = ?
            GROUP BY m.id, m.title, m.overview, m.vote_average, 
                     m.popularity, m.release_year, m.poster_path, uf.added_at
            ORDER BY uf.added_at DESC
            """;
        try (PreparedStatement stmt = dbConnection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Movie movie = new Movie(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("overview"),
                        rs.getDouble("vote_average"),
                        rs.getDouble("popularity"),
                        rs.getInt("release_year"),
                        rs.getString("poster_path"),
                        rs.getString("genres")
                    );
                    // Можно добавить обработку rs.getTimestamp("added_at") если нужно
                    movies.add(movie);
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка получения избранных фильмов: " + e.getMessage());
        }
        return movies;
    }

    // Вспомогательные методы
    
    private List<Movie> executeMovieQuery(PreparedStatement stmt) throws SQLException {
        List<Movie> movies = new ArrayList<>();
        ResultSet rs = stmt.executeQuery();
        
        while (rs.next()) {
            movies.add(new Movie(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("overview"),
                rs.getDouble("vote_average"),
                rs.getDouble("popularity"),
                rs.getInt("release_year"),
                rs.getString("poster_path"),
                rs.getString("genres")
            ));
        }
        
        return movies;
    }
    
    /**
     * Записывает фильм как просмотренный в user_views
     */
    public void recordMovieView(int userId, int movieId) {
        String checkQuery = "SELECT id FROM user_views WHERE user_id = ? AND movie_id = ?";
        String insertQuery = "INSERT INTO user_views (user_id, movie_id, view_start_time, completed) VALUES (?, ?, CURRENT_TIMESTAMP, TRUE)";
        try {
            boolean exists;
            try (PreparedStatement checkStmt = dbConnection.prepareStatement(checkQuery)) {
                checkStmt.setInt(1, userId);
                checkStmt.setInt(2, movieId);
                ResultSet rs = checkStmt.executeQuery();
                exists = rs.next();
            }

            if (!exists) {
                try (PreparedStatement insertStmt = dbConnection.prepareStatement(insertQuery)) {
                    insertStmt.setInt(1, userId);
                    insertStmt.setInt(2, movieId);
                    insertStmt.executeUpdate();
                    System.out.println("✅ Фильм " + movieId + " отмечен как просмотренный для пользователя " + userId);
                }
            } else {
                System.out.println("ℹ️ Фильм " + movieId + " уже был отмечен как просмотренный для пользователя " + userId);
            }
        } catch (SQLException e) {
            System.err.println("❌ Ошибка при записи просмотра фильма: " + e.getMessage());
        }
    }

    /**
     * Проверяет, был ли фильм просмотрен пользователем (из user_views)
     */
    public boolean isMovieWatchedByUser(int userId, int movieId) {
        String query = "SELECT COUNT(*) FROM user_views WHERE user_id = ? AND movie_id = ?";
        try (PreparedStatement stmt = dbConnection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, movieId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при проверке просмотра фильма: " + e.getMessage());
        }
        return false;
    }
    
    /**
     * Проверяет, находится ли фильм в избранном у пользователя
     */
    public boolean isMovieFavorite(int userId, int movieId) {
        String query = "SELECT 1 FROM user_favorites WHERE user_id = ? AND movie_id = ?";
        try (PreparedStatement stmt = dbConnection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, movieId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Ошибка проверки избранного: " + e.getMessage());
            return false;
        }
    }

    /**
     * Добавляет фильм в избранное для пользователя
     */
    public boolean addMovieToFavorites(int userId, int movieId) {
        String query = "INSERT INTO user_favorites (user_id, movie_id, added_at) VALUES (?, ?, NOW()) ON CONFLICT DO NOTHING";
        try (PreparedStatement stmt = dbConnection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, movieId);
            int affected = stmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("Ошибка добавления в избранное: " + e.getMessage());
            return false;
        }
    }

    /**
     * Удаляет фильм из избранного для пользователя
     */
    public boolean removeMovieFromFavorites(int userId, int movieId) {
        String query = "DELETE FROM user_favorites WHERE user_id = ? AND movie_id = ?";
        try (PreparedStatement stmt = dbConnection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, movieId);
            int affected = stmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("Ошибка удаления из избранного: " + e.getMessage());
            return false;
        }
    }

    // Эти методы взаимодействуют с user_views и сессионным кэшем
    public void markMoviesAsShown(int userId, List<Movie> movies) {
        // Логика кэширования для текущей сессии
        Set<Integer> shownMovies = shownMoviesCache.computeIfAbsent(userId, k -> new HashSet<>());
        for (Movie movie : movies) {
            shownMovies.add(movie.getId());
            // Дополнительно записываем в БД
            recordMovieView(userId, movie.getId()); 
        }
        System.out.println("✅ Отмечены как показанные: " + movies.size() + " фильмов");
    }
    
    // Метод для отслеживания показанных рекомендаций в текущей сессии (не очищает БД)
    private final Map<Integer, Set<Integer>> currentSessionRecommendations = new HashMap<>();
    
    // Отметить фильмы как показанные в текущей сессии рекомендаций
    public void markMoviesAsRecommended(int userId, List<Movie> movies) {
        // Получаем или создаем список рекомендованных для текущей сессии
        Set<Integer> recommendedMovies = currentSessionRecommendations.computeIfAbsent(userId, k -> new HashSet<>());
        for (Movie movie : movies) {
            recommendedMovies.add(movie.getId());
        }
        System.out.println("📊 Отмечены как рекомендованные в текущей сессии: " + movies.size() + " фильмов");
        System.out.println("📊 Всего рекомендовано в текущей сессии: " + recommendedMovies.size() + " фильмов");
    }
    
    // Полностью сбросить отслеживание рекомендаций текущей сессии
    public void clearSessionRecommendations(int userId) {
        currentSessionRecommendations.remove(userId);
        System.out.println("🔄 Сброшен список рекомендаций текущей сессии для пользователя " + userId);
    }
    
    // Очистить только историю показов, но сохранить рекомендации сессии
    public void clearShownMoviesForUser(int userId) {
        shownMoviesCache.remove(userId);
        System.out.println("🔄 Сброшен кэш показанных фильмов для пользователя " + userId);
        // Очистка БД user_views не предусмотрена, т.к. это история просмотров
    }
    
    // Полностью сбросить все кэши для пользователя
    public void clearAllUserCaches(int userId) {
        clearShownMoviesForUser(userId);
        clearSessionRecommendations(userId);
        System.out.println("🔄 Полностью сброшены все кэши для пользователя " + userId);
    }
    
    // Этот метод теперь получает фильмы из user_views
    private Set<Integer> getShownMovies(int userId) {
        // Объединяем просмотренные из БД и из кэша текущей сессии
        Set<Integer> watchedFromDb = new HashSet<>();
        String query = "SELECT movie_id FROM user_views WHERE user_id = ?";
        try (PreparedStatement stmt = dbConnection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                watchedFromDb.add(rs.getInt("movie_id"));
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при получении просмотренных фильмов из БД: " + e.getMessage());
        }
        
        Set<Integer> allShown = new HashSet<>(shownMoviesCache.getOrDefault(userId, new HashSet<>()));
        allShown.addAll(watchedFromDb);
        return allShown;
    }
    
    // Получить список фильмов, рекомендованных в текущей сессии
    private Set<Integer> getSessionRecommendedMovies(int userId) {
        return currentSessionRecommendations.getOrDefault(userId, new HashSet<>());
    }
    
    public void shutdown() {
        try {
            if (mlService != null) {
                mlService.shutdown();
            }
            if (dbConnection != null) {
                dbConnection.close();
            }
            System.out.println("✅ Сервис завершен");
        } catch (SQLException e) {
            System.err.println("Ошибка при закрытии: " + e.getMessage());
        }
    }
    
    /**
     * Публичный метод сохранения оценки пользователя (обратная совместимость)
     * Использует внутренний метод saveUserRatingInternal
     */
    public boolean saveUserRating(UserRating rating) {
        return saveUserRatingInternal(rating);
    }

    public List<Movie> searchMovie(String searchQuery) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'searchMovie'");
    }
}
