package org.example;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import okhttp3.OkHttpClient;

public class createMoviesTable {
    // db connection
    private static final String DB_URL = "jdbc:postgresql://localhost:15432/postgres";
    private static final String DB_USER = "zwloader";
    private static final String DB_PASSWORD = "0010085070Pgsql";

    // для чего тут эти 2 строки ниже объясни:
    private static Connection dbConnection;
    private static OkHttpClient httpClient;
    static String createMoviesTable() throws SQLException {
        // Получаем соединение с базой данных из класса initdb
        dbConnection = initdb.getDbConnection();
        
        String createTableSQL = """
        CREATE TABLE IF NOT EXISTS movies (
            id INTEGER PRIMARY KEY,
            title VARCHAR(500) NOT NULL,
            original_title VARCHAR(500),
            overview TEXT,
            release_date DATE,
            vote_average DECIMAL(3,1),
            vote_count INTEGER,
            poster_path VARCHAR(200),
            popularity DECIMAL(10,3),
            release_year INTEGER,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        
        -- Таблица жанров
        CREATE TABLE IF NOT EXISTS genres (
            id INTEGER PRIMARY KEY,
            name VARCHAR(100) NOT NULL UNIQUE,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        
        -- Связующая таблица фильмы-жанры (многие ко многим)
        CREATE TABLE IF NOT EXISTS movie_genres (
            movie_id INTEGER NOT NULL,
            genre_id INTEGER NOT NULL,
            PRIMARY KEY (movie_id, genre_id),
            FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
            FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE
        );
        
        -- Таблица пользователей
        CREATE TABLE IF NOT EXISTS users (
            id SERIAL PRIMARY KEY,
            username VARCHAR(100) NOT NULL UNIQUE,
            password_hash VARCHAR(255),
            age INTEGER,
            gender VARCHAR(10),
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        
        -- Таблица оценок пользователей (критически важно для рекомендаций)
        CREATE TABLE IF NOT EXISTS user_ratings (
            id SERIAL PRIMARY KEY,
            user_id INTEGER NOT NULL,
            movie_id INTEGER NOT NULL,
            rating DECIMAL(2,1) NOT NULL CHECK (rating >= 0.5 AND rating <= 5.0),
            review_text TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
            UNIQUE(user_id, movie_id)
        );
        ALTER TABLE user_ratings ADD COLUMN IF NOT EXISTS published BOOLEAN DEFAULT TRUE;
        
        -- История просмотров (поведенческие данные для ML)
        CREATE TABLE IF NOT EXISTS user_views (
            id SERIAL PRIMARY KEY,
            user_id INTEGER NOT NULL,
            movie_id INTEGER NOT NULL,
            view_start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            view_duration INTEGER,
            completed BOOLEAN DEFAULT FALSE,
            watch_percentage DECIMAL(5,2),
            device_type VARCHAR(50),
            session_id VARCHAR(100),
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
        );
        
        -- Избранные фильмы пользователей
        CREATE TABLE IF NOT EXISTS user_favorites (
            user_id INTEGER NOT NULL,
            movie_id INTEGER NOT NULL,
            added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            PRIMARY KEY (user_id, movie_id),
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE
        );
                    
        -- Таблица предпочтений пользователей
        CREATE TABLE IF NOT EXISTS user_preferences (
            user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
            preferred_genres TEXT,
            min_rating REAL DEFAULT 0.0,
            include_watched_movies BOOLEAN DEFAULT FALSE,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
        
        -- Индексы для оптимизации запросов
        CREATE INDEX IF NOT EXISTS idx_movies_year ON movies(release_year);
        CREATE INDEX IF NOT EXISTS idx_movies_rating ON movies(vote_average);
        CREATE INDEX IF NOT EXISTS idx_movies_popularity ON movies(popularity);
        CREATE INDEX IF NOT EXISTS idx_movie_genres_movie_id ON movie_genres(movie_id);
        CREATE INDEX IF NOT EXISTS idx_movie_genres_genre_id ON movie_genres(genre_id);
        CREATE INDEX IF NOT EXISTS idx_genres_name ON genres(name);
        CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
        CREATE INDEX IF NOT EXISTS idx_user_ratings_user_id ON user_ratings(user_id);
        CREATE INDEX IF NOT EXISTS idx_user_ratings_movie_id ON user_ratings(movie_id);
        CREATE INDEX IF NOT EXISTS idx_user_ratings_rating ON user_ratings(rating);
        CREATE INDEX IF NOT EXISTS idx_user_views_user_id ON user_views(user_id);
        CREATE INDEX IF NOT EXISTS idx_user_views_movie_id ON user_views(movie_id);
        CREATE INDEX IF NOT EXISTS idx_user_views_timestamp ON user_views(view_start_time);
        CREATE INDEX IF NOT EXISTS idx_user_favorites_user_id ON user_favorites(user_id);
        CREATE INDEX IF NOT EXISTS idx_user_favorites_movie_id ON user_favorites(movie_id);
        """;

        try (Statement stmt = dbConnection.createStatement()) {
            // Выполнение SQL команды
            stmt.execute(createTableSQL);
            // Подтверждение транзакции
            dbConnection.commit();
            System.out.println("Все таблицы созданы или уже существуют: movies, genres, movie_genres, users, user_ratings, categories, movie_categories, user_views, user_favorites, user_movie_tags");
            return "succses";
        } catch (SQLException e) {
            dbConnection.rollback();
            throw new SQLException("Ошибка при создании таблицы movies", e);
        }

    }
}
