package amaneko.ml_and_fx.ml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

import amaneko.ml_and_fx.model.UserPreferences;
import amaneko.ml_and_fx.service.SimpleMovieRecommendationService;

/**
 * Простой ML сервис для рекомендаций фильмов
 */
public class SimpleMLService implements MLService {

    private boolean modelLoaded = true; // Для демонстрации считаем модель загруженной

    private static final String USER_FACTORS_PATH = "models/movie_recommendation_model/userFactors";
    private static final String ITEM_FACTORS_PATH = "models/movie_recommendation_model/itemFactors";

    private transient SparkSession spark;
    private final Map<Integer, float[]> userFactors = new HashMap<>();
    private final Map<Integer, float[]> itemFactors = new HashMap<>();
    private boolean factorsLoaded = false;

    private SimpleMovieRecommendationService movieService;

    public SimpleMLService() {}
    public SimpleMLService(SimpleMovieRecommendationService movieService) {
        this.movieService = movieService;
    }
    public void setMovieService(SimpleMovieRecommendationService movieService) {
        this.movieService = movieService;
    }

    /**
     * Проверяет, загружена ли ML модель
     */
    @Override
    public boolean isModelLoaded() {
        return modelLoaded;
    }

    private void ensureSpark() {
        if (spark == null) {
            spark = SparkSession.builder()
                    .appName("MovieRecommendationService")
                    .master("local[*]")
                    .config("spark.driver.bindAddress", "127.0.0.1")
                    .getOrCreate();
        }
    }

    private void loadFactors() {
        if (factorsLoaded) return;
        ensureSpark();
        // userFactors
        Dataset<Row> userDF = spark.read().parquet(USER_FACTORS_PATH);
        for (Row row : userDF.collectAsList()) {
            int id = row.getInt(row.fieldIndex("id"));
            List<?> features = row.getList(row.fieldIndex("features"));
            float[] vec = new float[features.size()];
            for (int i = 0; i < features.size(); i++) vec[i] = ((Number)features.get(i)).floatValue();
            userFactors.put(id, vec);
        }
        // itemFactors
        Dataset<Row> itemDF = spark.read().parquet(ITEM_FACTORS_PATH);
        for (Row row : itemDF.collectAsList()) {
            int id = row.getInt(row.fieldIndex("id"));
            List<?> features = row.getList(row.fieldIndex("features"));
            float[] vec = new float[features.size()];
            for (int i = 0; i < features.size(); i++) vec[i] = ((Number)features.get(i)).floatValue();
            itemFactors.put(id, vec);
        }
        factorsLoaded = true;
        // Логируем id пользователей, покрытых моделью
        System.out.println("[ML] userFactors ids: " + userFactors.keySet());
    }

    private float dot(float[] a, float[] b) {
        float sum = 0f;
        for (int i = 0; i < a.length; i++) sum += a[i] * b[i];
        return sum;
    }

    /**
     * Получить рекомендации для пользователя
     * @param userId ID пользователя
     * @param count количество рекомендаций
     * @return список ID фильмов
     */
    @Override
    public List<Integer> getRecommendations(int userId, int count) {
        loadFactors();
        float[] userVec = userFactors.get(userId);
        if (userVec == null) {
            // Cold-start: возвращаем top-N itemFactors (например, просто первые N id)
            System.out.println("[ML] Пользователь " + userId + " не найден в userFactors. Cold-start fallback.");
            List<Integer> fallback = new ArrayList<>(itemFactors.keySet());
            java.util.Collections.shuffle(fallback);
            // Фильтруем фильмы без описания
            List<Integer> filtered = new ArrayList<>();
            int checked = 0;
            for (Integer movieId : fallback) {
                if (hasDescription(movieId)) {
                    filtered.add(movieId);
                } else {
                    // System.out.println("[ML] Фильм " + movieId + " не имеет описания, ищем другой...");
                }
                if (filtered.size() >= count) break;
                checked++;
                if (checked > fallback.size() * 2) break; // safety
            }
            return filtered;
        }
        // Считаем рейтинг для каждого фильма
        List<Map.Entry<Integer, Float>> scored = itemFactors.entrySet().stream()
                .map(e -> Map.entry(e.getKey(), dot(userVec, e.getValue())))
                .sorted((a, b) -> -Float.compare(a.getValue(), b.getValue()))
                .limit(Math.max(count * 3, count + 5)) // взять больше кандидатов
                .collect(Collectors.toList());
        // Перемешать топ-кандидатов для разнообразия
        List<Integer> result = new ArrayList<>();
        int checked = 0;
        for (Map.Entry<Integer, Float> entry : scored) {
            if (hasDescription(entry.getKey())) {
                result.add(entry.getKey());
            } else {
                // System.out.println("[ML] Фильм " + entry.getKey() + " не имеет описания, ищем другой...");
            }
            if (result.size() >= count) break;
            checked++;
            if (checked > scored.size() * 2) break; // safety
        }
        java.util.Collections.shuffle(result);
        return result.stream().limit(count).collect(Collectors.toList());
    }

    // Проверка наличия описания у фильма (заглушка, можно заменить на реальную проверку по БД или кэшу)
    private boolean hasDescription(int movieId) {
        if (movieService == null) return true; // fallback: allow all if not set
        var movie = movieService.getMovieById(movieId);
        return movie != null && movie.getOverview() != null && !movie.getOverview().trim().isEmpty();
    }

    /**
     * Получить предсказанный рейтинг для фильма
     * @param userId ID пользователя
     * @param movieId ID фильма
     * @return предсказанный рейтинг от 1.0 до 5.0
     */
    @Override
    public float getPredictedRating(int userId, int movieId) {
        loadFactors();
        float[] userVec = userFactors.get(userId);
        float[] itemVec = itemFactors.get(movieId);
        if (userVec == null || itemVec == null) return 0f;
        return dot(userVec, itemVec);
    }

    private void retrainModelFromExternalProject() {
        try {
            System.out.println("[ML] Запуск переобучения ALS через may_practic...");
            // Формируем команду с обязательными JVM-аргументами для Spark на Java 17+/21+
            List<String> command = new ArrayList<>();
            command.add("java");
            command.add("--add-exports");
            command.add("java.base/sun.nio.ch=ALL-UNNAMED");
            command.add("--add-opens");
            command.add("java.base/java.nio=ALL-UNNAMED");
            command.add("-cp");
            command.add("/home/zwloader/IdeaProjects/may_practic/target/classes:/home/zwloader/IdeaProjects/may_practic/target/dependency/*");
            command.add("org.example.nanomachineson");
            command.add("/home/zwloader/IdeaProjects/ml_and_fx/models/movie_recommendation_model");
            System.out.println("[ML] Команда запуска: " + String.join(" ", command));
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true); // Объединить stdout и stderr
            Process process = pb.start();
            // Логируем вывод процесса
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("[ALS] " + line);
                }
            }
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("[ML] Переобучение ALS завершено успешно");
                this.factorsLoaded = false;
                loadFactors();
            } else {
                System.err.println("[ML] Ошибка переобучения ALS, код: " + exitCode);
            }
        } catch (Exception e) {
            System.err.println("[ML] Ошибка запуска переобучения ALS: " + e.getMessage());
            // stack trace removed for compliance
        }
    }

    /**
     * Обновить историю просмотров для ML модели
     * @param userId ID пользователя
     * @param movieId ID фильма
     */
    @Override
    public void updateViewingHistory(int userId, int movieId) {
        System.out.println("🧠 ML: Обновлена история просмотров - пользователь " + userId + ", фильм " + movieId);
        retrainModelFromExternalProject();
    }

    /**
     * Обновить предпочтения пользователя для ML модели
     * @param userId ID пользователя
     * @param preferences предпочтения пользователя
     */
    @Override
    public void updateUserPreferences(int userId, UserPreferences preferences) {
        System.out.println("🧠 ML: Обновлены предпочтения пользователя " + userId +
                          " (жанров: " + preferences.getPreferredGenres().size() + ")");
        retrainModelFromExternalProject();
    }

    /**
     * Завершить работу ML сервиса
     */
    @Override
    public void shutdown() {
        System.out.println("🧠 ML сервис завершил работу");
        modelLoaded = false;
    }
}
