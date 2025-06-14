package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.evaluation.RegressionEvaluator;
import org.apache.spark.ml.feature.HashingTF;
import org.apache.spark.ml.feature.IDF;
import org.apache.spark.ml.feature.StopWordsRemover;
import org.apache.spark.ml.feature.Tokenizer;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.recommendation.ALS;
import org.apache.spark.ml.recommendation.ALSModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class nanomachineson {

    private static final String DB_URL = "jdbc:postgresql://localhost:15432/postgres";
    private static final String DB_USER = "zwloader";
    private static final String DB_PASSWORD = "0010085070Pgsql";

    // Для поддержки передачи пути через аргумент
    public static String modelPathArg = null;

    public static void main(String[] args) {
        if (args.length > 0) {
            modelPathArg = args[0];
            System.out.println("[ALS] Используем путь для модели: " + modelPathArg);
        }
        System.out.println("Запуск системы рекомендаций фильмов...");

        // Создание Spark сессии
        SparkSession spark = SparkSession.builder()
                .appName("Система рекомендаций фильмов")
                .master("local[*]")
                .config("spark.sql.adaptive.enabled", "true")
                .config("spark.sql.adaptive.coalescePartitions.enabled", "true")
                .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .getOrCreate();

        // Настройки подключения к PostgreSQL
        Properties connectionProperties = new Properties();
        connectionProperties.put("user", DB_USER);
        connectionProperties.put("password", DB_PASSWORD);
        connectionProperties.put("driver", "org.postgresql.Driver");

        try {
            // 1. Загружаем данные о фильмах
            System.out.println("Загружаем данные о фильмах...");
            Dataset<Row> moviesDF = loadMoviesData(spark, connectionProperties);

            // 2. Создаем тестовых пользователей и оценки
            System.out.println("Создаем тестовых пользователей...");
            createTestData();

            // 3. Загружаем пользовательские оценки
            System.out.println("Загружаем оценки пользователей...");
            Dataset<Row> ratingsDF = loadRatingsData(spark, connectionProperties);

            if (ratingsDF.count() > 0) {
                // 4. Обучаем модель машинного обучения
                System.out.println("Обучаем модель рекомендаций...");
                trainRecommendationModel(spark, ratingsDF);
            }

            // 5. Создаем content-based рекомендации
            System.out.println("Создаем рекомендации на основе контента...");
            createContentBasedRecommendations(spark, moviesDF);

        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
        } finally {
            spark.stop();
            System.out.println("Работа завершена!");
        }
    }

    // Загрузка данных о фильмах
    private static Dataset<Row> loadMoviesData(SparkSession spark, Properties props) {
        String movieQuery = """
            SELECT 
                m.id,
                m.title,
                m.overview,
                m.vote_average,
                m.vote_count,
                m.popularity,
                m.release_year,
                STRING_AGG(g.name, ' ') as genres_text
            FROM movies m
            LEFT JOIN movie_genres mg ON m.id = mg.movie_id
            LEFT JOIN genres g ON mg.genre_id = g.id
            WHERE m.overview IS NOT NULL 
              AND m.vote_count > 10
            GROUP BY m.id, m.title, m.overview, m.vote_average, 
                     m.vote_count, m.popularity, m.release_year
            """;
//        LIMIT 10000
        Dataset<Row> moviesDF = spark.read()
                .jdbc(DB_URL, "(" + movieQuery + ") as movies_data", props);

        System.out.println("Загружено фильмов: " + moviesDF.count());
        moviesDF.show(5, false);
        return moviesDF.cache(); // Кэшируем для ускорения
    }

    // Создание тестовых данных
    private static void createTestData() {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement stmt = conn.createStatement();

            // Создаем тестовых пользователей
            String createUsers = """
                INSERT INTO users (username, age, gender) 
                SELECT 
                    'testuser_' || generate_series(1, 500),
                    (random() * 50 + 18)::int,
                    CASE WHEN random() > 0.5 THEN 'M' ELSE 'F' END
                ON CONFLICT DO NOTHING
                """;
            stmt.executeUpdate(createUsers);

            // Создаем оценки фильмов с более реалистичным распределением
            String createRatings = """
                INSERT INTO user_ratings (user_id, movie_id, rating, created_at)
                SELECT 
                    u.id,
                    m.id,
                    CASE 
                        WHEN m.vote_average >= 8.5 THEN 
                            CASE WHEN random() < 0.7 THEN (random() * 1 + 4)::numeric(2,1) 
                                 ELSE (random() * 2 + 3)::numeric(2,1) END
                        WHEN m.vote_average >= 7.5 THEN 
                            CASE WHEN random() < 0.6 THEN (random() * 1.5 + 3.5)::numeric(2,1) 
                                 ELSE (random() * 2.5 + 2.5)::numeric(2,1) END
                        WHEN m.vote_average >= 6.5 THEN 
                            CASE WHEN random() < 0.5 THEN (random() * 1.5 + 3)::numeric(2,1) 
                                 ELSE (random() * 3 + 2)::numeric(2,1) END
                        WHEN m.vote_average >= 5.5 THEN (random() * 2.5 + 2)::numeric(2,1)
                        ELSE (random() * 3 + 1)::numeric(2,1)
                    END,
                    NOW()
                FROM users u
                CROSS JOIN (
                    SELECT id, vote_average 
                    FROM movies 
                    WHERE vote_average IS NOT NULL AND vote_count > 50
                    ORDER BY random() 
                    LIMIT 500
                ) m
                WHERE random() < 0.15  -- Больше спарсности данных
                ON CONFLICT DO NOTHING
                """;
            stmt.executeUpdate(createRatings);

            conn.close();
            System.out.println("Тестовые данные созданы");

        } catch (Exception e) {
            System.err.println("Ошибка создания тестовых данных: " + e.getMessage());
        }
    }

    // Загрузка оценок пользователей
    private static Dataset<Row> loadRatingsData(SparkSession spark, Properties props) {
        Dataset<Row> ratingsDF = spark.read()
                .jdbc(DB_URL, "user_ratings", props)
                .select("user_id", "movie_id", "rating");

        System.out.println("Загружено оценок: " + ratingsDF.count());
        
        // Фильтруем пользователей и фильмы с минимальным количеством оценок
        Dataset<Row> filteredRatings = ratingsDF
                .groupBy("user_id").count().filter("count >= 5")
                .join(ratingsDF, "user_id")
                .drop("count")
                .groupBy("movie_id").count().filter("count >= 3")
                .join(ratingsDF, "movie_id")
                .drop("count")
                .select("user_id", "movie_id", "rating");
        
        System.out.println("После фильтрации оценок: " + filteredRatings.count());
        if (filteredRatings.count() > 0) {
            filteredRatings.show(10);
        }
        return filteredRatings.cache();
    }

    // Обучение модели коллаборативной фильтрации
    private static void trainRecommendationModel(SparkSession spark, Dataset<Row> ratingsDF) {
        // Разделяем данные на обучение и тест
        Dataset<Row>[] splits = ratingsDF.randomSplit(new double[]{0.8, 0.2}, 42);
        Dataset<Row> training = splits[0].cache();
        Dataset<Row> test = splits[1].cache();

        System.out.println("Обучающих примеров: " + training.count());
        System.out.println("Тестовых примеров: " + test.count());

        // Простой grid search для лучших параметров
        double bestRMSE = Double.MAX_VALUE;
        ALSModel bestModel = null;
        
        // Параметры для поиска
        int[] ranks = {20, 50, 100};
        double[] regParams = {0.01, 0.1, 0.2};
        
        System.out.println("Ищем лучшие параметры...");
        
        for (int rank : ranks) {
            for (double regParam : regParams) {
                System.out.printf("Тестируем: rank=%d, regParam=%.2f...%n", rank, regParam);
                
                ALS als = new ALS()
                        .setMaxIter(20)
                        .setRegParam(regParam)
                        .setRank(rank)
                        .setUserCol("user_id")
                        .setItemCol("movie_id")
                        .setRatingCol("rating")
                        .setColdStartStrategy("drop");

                ALSModel model = als.fit(training);
                Dataset<Row> predictions = model.transform(test);

                RegressionEvaluator evaluator = new RegressionEvaluator()
                        .setMetricName("rmse")
                        .setLabelCol("rating")
                        .setPredictionCol("prediction");

                double rmse = evaluator.evaluate(
                        predictions.filter("prediction IS NOT NULL AND NOT isnan(prediction)")
                );
                
                System.out.printf("RMSE: %.3f%n", rmse);
                
                if (rmse < bestRMSE) {
                    bestRMSE = rmse;
                    bestModel = model;
                    System.out.printf("✅ Новый лучший результат: %.3f (rank=%d, regParam=%.2f)%n", 
                                    rmse, rank, regParam);
                }
            }
        }
        
        System.out.printf("🎯 Лучшая ошибка модели (RMSE): %.3f%n", bestRMSE);
        System.out.println("Чем меньше это число, тем лучше модель!");

        // Получаем рекомендации
        Dataset<Row> userRecs = bestModel.recommendForAllUsers(10);
        System.out.println("Топ рекомендации для пользователей:");
        userRecs.show(5, false);

        // Сохраняем модель
        try {
            // Получаем путь из аргументов или по умолчанию
            String modelPath = System.getProperty("modelPath");
            if (modelPath == null || modelPath.isEmpty()) {
                modelPath = (org.example.nanomachineson.modelPathArg != null) ? org.example.nanomachineson.modelPathArg : "./models/movie_recommendation_model";
            }
            bestModel.save(modelPath);
            System.out.println("Лучшая модель сохранена в: " + modelPath);
        } catch (Exception e) {
            System.out.println("Не удалось сохранить модель: " + e.getMessage());
        }
    }

    // Content-based рекомендации на основе описаний и жанров
    private static void createContentBasedRecommendations(SparkSession spark, Dataset<Row> moviesDF) {
        System.out.println("Создаем рекомендации на основе содержания фильмов...");

        // 1. Обработка текста описаний (TF-IDF)
        Tokenizer tokenizer = new Tokenizer()
                .setInputCol("overview")
                .setOutputCol("words");

        StopWordsRemover remover = new StopWordsRemover()
                .setInputCol("words")
                .setOutputCol("filtered_words");

        HashingTF hashingTF = new HashingTF()
                .setInputCol("filtered_words")
                .setOutputCol("raw_features")
                .setNumFeatures(1000);

        IDF idf = new IDF()
                .setInputCol("raw_features")
                .setOutputCol("overview_features");

        // 2. Обработка жанров
        Tokenizer genreTokenizer = new Tokenizer()
                .setInputCol("genres_text")
                .setOutputCol("genre_words");

        HashingTF genreHashingTF = new HashingTF()
                .setInputCol("genre_words")
                .setOutputCol("genre_features")
                .setNumFeatures(50);

        // 3. Объединение признаков
        VectorAssembler assembler = new VectorAssembler()
                .setInputCols(new String[]{"overview_features", "genre_features"})
                .setOutputCol("features");

        // Создаем пайплайн
        Pipeline pipeline = new Pipeline()
                .setStages(new PipelineStage[]{
                        tokenizer, remover, hashingTF, idf,
                        genreTokenizer, genreHashingTF, assembler
                });

        // Обучаем пайплайн
        PipelineModel pipelineModel = pipeline.fit(moviesDF);
        Dataset<Row> featuresDF = pipelineModel.transform(moviesDF);

        System.out.println("Признаки созданы для " + featuresDF.count() + " фильмов");
        featuresDF.select("id", "title", "features").show(5, false);

        // Сохраняем пайплайн
        try {
            pipelineModel.save("models/content_based_pipeline");
            System.out.println("Пайплайн content-based рекомендаций сохранен!");
        } catch (Exception e) {
            System.out.println("Не удалось сохранить пайплайн: " + e.getMessage());
        }
    }
}
