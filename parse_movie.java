package org.example;


import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class parse_movie {
    static final String API = "no";
    // Updated URL without placeholders for formatting
    static final String URL = "https://api.themoviedb.org/3/discover/movie?include_adult=false&include_video=false&language=ru-RU&sort_by=popularity.desc";

    // db connection
    private static final String DB_URL = "jdbc:postgresql://localhost:15432/postgres";
    private static final String DB_USER = "zwloader";
    private static final String DB_PASSWORD = "0010085070Pgsql";

    // для чего тут эти 2 строки ниже объясни:
    private static Connection dbConnection;
    private static OkHttpClient httpClient;
    
    // Кэш жанров для избежания повторных запросов к API
    private static Map<Integer, String> genreCache = new HashMap<>();
    
    // Список для накопления информации о жанрах фильмов перед batch сохранением
    private static List<MovieGenreInfo> pendingMovieGenres = new ArrayList<>();
    
    // Класс для хранения информации о жанрах фильма
    private static class MovieGenreInfo {
        int movieId;
        JsonArray genreIds;
        
        MovieGenreInfo(int movieId, JsonArray genreIds) {
            this.movieId = movieId;
            this.genreIds = genreIds;
        }
    }

    // Загружаем список жанров из TMDB API
    private static void loadGenres() throws Exception {
        String genresUrl = "https://api.themoviedb.org/3/genre/movie/list?language=ru-RU";
        
        Request request = new Request.Builder()
                .url(genresUrl)
                .get()
                .addHeader("accept", "application/json")
                .addHeader("Authorization", "Bearer " + API)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JsonObject responseObj = JsonParser.parseString(responseBody).getAsJsonObject();
                JsonArray genres = responseObj.getAsJsonArray("genres");
                
                // Загружаем жанры в кэш и в базу данных
                for (JsonElement genreElement : genres) {
                    JsonObject genre = genreElement.getAsJsonObject();
                    int genreId = genre.get("id").getAsInt();
                    String genreName = genre.get("name").getAsString();
                    
                    genreCache.put(genreId, genreName);
                    insertGenreIfNotExists(genreId, genreName);
                }
                
                System.out.println("Загружено " + genres.size() + " жанров");
            } else {
                System.err.println("Ошибка при загрузке жанров: " + response.code());
            }
        }
    }
    
    // Вставляем жанр в базу данных, если его там нет
    private static void insertGenreIfNotExists(int genreId, String genreName) throws SQLException {
        String insertGenreSQL = "INSERT INTO genres (id, name) VALUES (?, ?) ON CONFLICT (id) DO NOTHING";
        try (PreparedStatement stmt = dbConnection.prepareStatement(insertGenreSQL)) {
            stmt.setInt(1, genreId);
            stmt.setString(2, genreName);
            stmt.executeUpdate();
        }
    }
    
    // Сохраняем связи между фильмом и жанрами
    private static void saveMovieGenres(int movieId, JsonArray genreIds) throws SQLException {
        // Сначала удаляем старые связи
        String deleteSQL = "DELETE FROM movie_genres WHERE movie_id = ?";
        try (PreparedStatement deleteStmt = dbConnection.prepareStatement(deleteSQL)) {
            deleteStmt.setInt(1, movieId);
            deleteStmt.executeUpdate();
        }
        
        // Затем добавляем новые связи
        String insertSQL = "INSERT INTO movie_genres (movie_id, genre_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement insertStmt = dbConnection.prepareStatement(insertSQL)) {
            for (JsonElement genreIdElement : genreIds) {
                int genreId = genreIdElement.getAsInt();
                insertStmt.setInt(1, movieId);
                insertStmt.setInt(2, genreId);
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();
        }
    }
    
    // Сохраняем все накопленные жанры после того, как фильмы уже в БД
    private static void savePendingMovieGenres() throws SQLException {
        if (pendingMovieGenres.isEmpty()) {
            return;
        }
        
        String insertSQL = "INSERT INTO movie_genres (movie_id, genre_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement insertStmt = dbConnection.prepareStatement(insertSQL)) {
            for (MovieGenreInfo movieGenre : pendingMovieGenres) {
                for (JsonElement genreIdElement : movieGenre.genreIds) {
                    int genreId = genreIdElement.getAsInt();
                    insertStmt.setInt(1, movieGenre.movieId);
                    insertStmt.setInt(2, genreId);
                    insertStmt.addBatch();
                }
            }
            insertStmt.executeBatch();
        }
        
        // Очищаем список после сохранения
        int savedCount = pendingMovieGenres.size();
        pendingMovieGenres.clear();
        System.out.println("Сохранены жанры для " + savedCount + " фильмов");
    }


    protected static void parse_movie() throws Exception {
        // Получаем соединение с базой данных из класса initdb
        dbConnection = initdb.getDbConnection();
        // Получаем HTTP клиент из класса initdb
        httpClient = initdb.getHttpClient();
        
        // Загружаем жанры из TMDB API
        System.out.println("Загрузка жанров из TMDB...");
        loadGenres();
        
        int totalMoviesFound = 0;

        String insertSQL = """
            INSERT INTO movies (id, title, original_title, overview, release_date, 
                              vote_average, vote_count, poster_path, popularity, release_year)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (id) DO UPDATE SET
                title = EXCLUDED.title,
                vote_average = EXCLUDED.vote_average,
                vote_count = EXCLUDED.vote_count,
                popularity = EXCLUDED.popularity
        """;
        try (PreparedStatement batchStmt = dbConnection.prepareStatement(insertSQL)) {
            try {
                for (int year = 1990; year <= 2025; year++) {
                    System.out.println("=== Обрабатываем " + year + " год ===");

                    int moviesForYear = processYear(year, batchStmt);
                    totalMoviesFound += moviesForYear;

                    // Выполняем batch каждые 1000 записей для оптимизации
                    if (totalMoviesFound % 1000 == 0 && totalMoviesFound > 0) {
                        int[] batchResults = batchStmt.executeBatch();
                        dbConnection.commit();
                        
                        // Сохраняем жанры после сохранения фильмов
                        savePendingMovieGenres();
                        dbConnection.commit();
                        
                        System.out.println("Сохранено " + totalMoviesFound + " фильмов, результат: " + batchResults.length + " операций");
                        // Очищаем пакет после сохранения
                        batchStmt.clearBatch();
                    }
                    
                    // Сохраняем фильмы после каждого года
                    if (moviesForYear > 0) {
                        int[] batchResults = batchStmt.executeBatch();
                        dbConnection.commit();
                        
                        // Сохраняем жанры после сохранения фильмов
                        savePendingMovieGenres();
                        dbConnection.commit();
                        
                        System.out.println("Сохранены фильмы за " + year + " год: " + moviesForYear + " фильмов");
                        // Очищаем пакет после сохранения
                        batchStmt.clearBatch();
                    }

                    System.out.println("Год " + year + ": " + moviesForYear + " фильмов");
                    Thread.sleep(1000); // Пауза между годами
                }

                // Выполняем оставшиеся записи
                if (totalMoviesFound % 1000 != 0) {
                    int[] batchResults = batchStmt.executeBatch();
                    dbConnection.commit();
                    
                    // Сохраняем жанры после сохранения фильмов
                    savePendingMovieGenres();
                    dbConnection.commit();
                    
                    System.out.println("Сохранены оставшиеся записи: " + batchResults.length + " операций");
                }

                System.out.println("=== ИТОГО: " + totalMoviesFound + " ФИЛЬМОВ СОХРАНЕНО В БД ===");
            } catch (SQLException e) {
                if (dbConnection != null) {
                    dbConnection.rollback();
                }
                System.err.println("Ошибка при сохранении данных в БД: " + e.getMessage());
                throw e;
            }
        }
    }

    private static int processYear(int year, PreparedStatement batchStmt) throws Exception {
        int moviesCount = 0;
        int currentPage = 1;
        int totalPages = 1;

        do {
            // Construct URL with year parameter
            String url = URL + "&primary_release_year=" + year + "&page=" + currentPage;
            System.out.println("Запрос к URL: " + url);

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("accept", "application/json")
                    .addHeader("Authorization", "Bearer " + API)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    System.out.println("Получен ответ длиной: " + responseBody.length() + " символов");
                    JsonObject responseObj = JsonParser.parseString(responseBody).getAsJsonObject();

                    totalPages = responseObj.get("total_pages").getAsInt();
                    JsonArray movies = responseObj.getAsJsonArray("results");
                    System.out.println("Получено фильмов на странице " + currentPage + ": " + movies.size());

                    for (JsonElement movieElement : movies) {
                        addMovieToBatch(movieElement.getAsJsonObject(), year, batchStmt);
                        moviesCount++;
                    }

                    if (currentPage == 1) {
                        int totalResults = responseObj.get("total_results").getAsInt();
                        System.out.println("Найдено " + totalResults + " фильмов на " +
                                Math.min(totalPages, 500) + " страницах");
                    }

                    currentPage++;
                    Thread.sleep(300); // Соблюдаем лимиты API

                } else {
                    System.out.println("Ошибка HTTP: " + response.code());
                    break;
                }
            }

        } while (currentPage <= totalPages && currentPage <= 500);

        return moviesCount;
    }
    private static void addMovieToBatch(JsonObject movie, int year, PreparedStatement stmt)
            throws SQLException {

        int movieId = movie.get("id").getAsInt();
        String title = movie.get("title").getAsString();
        String originalTitle = movie.get("original_title").getAsString();
        String overview = movie.has("overview") && !movie.get("overview").isJsonNull()
                ? movie.get("overview").getAsString() : "";
        String releaseDate = movie.has("release_date") && !movie.get("release_date").isJsonNull()
                ? movie.get("release_date").getAsString() : null;
        double voteAverage = movie.get("vote_average").getAsDouble();
        int voteCount = movie.get("vote_count").getAsInt();
        String posterPath = movie.has("poster_path") && !movie.get("poster_path").isJsonNull()
                ? movie.get("poster_path").getAsString() : "";
        double popularity = movie.get("popularity").getAsDouble();

        try {
            stmt.setInt(1, movieId);
            stmt.setString(2, title);
            stmt.setString(3, originalTitle);
            stmt.setString(4, overview);
            if (releaseDate != null && !releaseDate.isEmpty()) {
                stmt.setDate(5, Date.valueOf(releaseDate));
            } else {
                stmt.setNull(5, Types.DATE);
            }
            stmt.setDouble(6, voteAverage);
            stmt.setInt(7, voteCount);
            stmt.setString(8, posterPath);
            stmt.setDouble(9, popularity);
            stmt.setInt(10, year);

            stmt.addBatch();
            
            // Добавляем информацию о жанрах в список ожидания
            if (movie.has("genre_ids") && !movie.get("genre_ids").isJsonNull()) {
                JsonArray genreIds = movie.getAsJsonArray("genre_ids");
                if (genreIds.size() > 0) {
                    pendingMovieGenres.add(new MovieGenreInfo(movieId, genreIds));
                }
            }
            
            // Раскомментируйте для отладки конкретных фильмов
            // System.out.println("Добавлен фильм в batch: ID=" + movieId + ", Название: " + title);
        } catch (SQLException e) {
            System.err.println("Ошибка при добавлении фильма в batch: " + e.getMessage());
            System.err.println("Фильм ID: " + movieId + ", Название: " + title);
            throw e;
        }
    }
    protected static void closeConnections() throws SQLException {
        if (dbConnection != null && !dbConnection.isClosed()) {
            dbConnection.close();
        }
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
        }
        System.out.println("Соединения закрыты");
    }

    }

