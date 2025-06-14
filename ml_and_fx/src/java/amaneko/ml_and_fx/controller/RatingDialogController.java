package amaneko.ml_and_fx.controller;

import java.net.URL;
import java.util.ResourceBundle;

import amaneko.ml_and_fx.model.Movie;
import amaneko.ml_and_fx.model.User;
import amaneko.ml_and_fx.model.UserRating;
import amaneko.ml_and_fx.service.SimpleMovieRecommendationService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

/**
 * Контроллер для диалога оценки фильма с звёздным рейтингом
 */
public class RatingDialogController implements Initializable {
    
    @FXML private Label movieTitleLabel;
    @FXML private Label movieInfoLabel;
    @FXML private HBox starRatingBox; // Required for FXML binding
    @FXML private Label star1, star2, star3, star4, star5;
    @FXML private Slider ratingSlider;
    @FXML private Label ratingValueLabel;
    @FXML private TextArea reviewTextArea;
    @FXML private Button saveButton; // Required for FXML binding
    @FXML private Button publishButton; // Required for FXML binding
    @FXML private Button cancelButton; // Required for FXML binding
    
    private Movie movie;
    private User currentUser;
    private UserRating userRating;
    private boolean saved = false;
    private Stage dialogStage;
    private SimpleMovieRecommendationService recommendationService;
    private int currentStarRating = 3;
    private Label[] stars;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Инициализируем массив звёзд
        stars = new Label[]{star1, star2, star3, star4, star5};
        
        // Настраиваем слайдер оценки
        setupRatingSlider();
        
        // Настраиваем звёздный рейтинг
        setupStarRating();
        
        // Устанавливаем начальное значение
        setStarRating(3);
        updateRatingDisplay();
        
        // Настраиваем стили для текстовой области отзыва
        setupReviewTextArea();
        
        // Проверяем доступность кнопок
        if (saveButton != null) {
            System.out.println("💾 Кнопка сохранения инициализирована");
        }
        if (publishButton != null) {
            System.out.println("📢 Кнопка публикации инициализирована");
        }
    }
    
    /**
     * Настраивает слайдер оценки
     */
    private void setupRatingSlider() {
        ratingSlider.setMin(1.0);
        ratingSlider.setMax(5.0);
        ratingSlider.setValue(3.0);
        ratingSlider.setShowTickLabels(true);
        ratingSlider.setShowTickMarks(true);
        ratingSlider.setMajorTickUnit(1.0);
        ratingSlider.setMinorTickCount(0);
        ratingSlider.setSnapToTicks(true);
        
        // Добавляем стиль слайдера
        ratingSlider.getStyleClass().add("rating-slider");
        
        // Синхронизируем слайдер со звёздами
        ratingSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            int rating = (int) Math.round(newVal.doubleValue());
            if (rating != currentStarRating) {
                setStarRating(rating);
            }
        });
    }
    
    /**
     * Настраивает интерактивность звёзд
     */
    private void setupStarRating() {
        for (int i = 0; i < stars.length; i++) {
            final int starIndex = i + 1;
            Label star = stars[i];
            
            // Клик по звезде
            star.setOnMouseClicked(event -> {
                setStarRating(starIndex);
                ratingSlider.setValue(starIndex);
            });
            
            // Hover эффект
            star.setOnMouseEntered(event -> {
                highlightStars(starIndex);
            });
            
            star.setOnMouseExited(event -> {
                highlightStars(currentStarRating);
            });
        }
    }
    
    /**
     * Обработчик клика по звезде (вызывается из FXML)
     */
    @FXML
    private void handleStarClick(MouseEvent event) {
        Label clickedStar = (Label) event.getSource();
        for (int i = 0; i < stars.length; i++) {
            if (stars[i] == clickedStar) {
                int rating = i + 1;
                setStarRating(rating);
                ratingSlider.setValue(rating);
                break;
            }
        }
    }
    
    /**
     * Устанавливает рейтинг в звёздах
     */
    private void setStarRating(int rating) {
        currentStarRating = Math.max(1, Math.min(5, rating));
        updateStarDisplay();
        updateRatingDisplay();
        // Убираем автоматическое подтверждение - пользователь сам нажмет "Сохранить"
    }
    
    /**
     * Обновляет отображение звёзд
     */
    private void updateStarDisplay() {
        highlightStars(currentStarRating);
    }
    
    /**
     * Подсвечивает звёзды в стиле Netflix
     */
    private void highlightStars(int rating) {
        for (int i = 0; i < stars.length; i++) {
            Label star = stars[i];
            
            // Удаляем все стили кроме базового star-button
            star.getStyleClass().removeAll("filled");
            
            if (i < rating) {
                star.setText("★");
                star.getStyleClass().add("filled");
            } else {
                star.setText("☆");
            }
        }
    }
    
    /**
     * Обновляет текст оценки
     */
    private void updateRatingDisplay() {
        String ratingText = currentStarRating + " из 5 звёзд";
        String emoji = getRatingEmoji(currentStarRating);
        ratingValueLabel.setText(emoji + " " + ratingText);
    }
    
    /**
     * Возвращает эмодзи для оценки в стиле Netflix
     */
    private String getRatingEmoji(int rating) {
        return switch (rating) {
            case 1 -> "👎"; // Плохо (палец вниз)
            case 2 -> "😕"; // Не очень
            case 3 -> "👌"; // Нормально
            case 4 -> "👍"; // Хорошо (палец вверх)
            case 5 -> "🔥"; // Отлично (огонь)
            default -> "👌";
        };
    }
    
    /**
     * Устанавливает фильм для оценки
     */
    public void setMovie(Movie movie) {
        this.movie = movie;
        if (movie != null) {
            movieTitleLabel.setText(movie.getTitle());
            String info = movie.getReleaseYear() + " • " + movie.getGenres();
            movieInfoLabel.setText(info);
            
            // Если у фильма уже есть оценка пользователя, показываем её
            if (movie.hasUserRating()) {
                UserRating existing = movie.getUserRating();
                int rating = (int) Math.round(existing.getRating());
                setStarRating(rating);
                ratingSlider.setValue(rating);
                if (existing.getReviewText() != null) {
                    reviewTextArea.setText(existing.getReviewText());
                }
            }
        }
    }
    
    /**
     * Устанавливает текущего пользователя
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
    }
    
    /**
     * Устанавливает Stage диалога
     */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
        
        // Принудительно устанавливаем размер диалога
        if (stage != null) {
            stage.setMinWidth(600);
            stage.setMinHeight(550);
            stage.setWidth(600);
            stage.setHeight(550);
            stage.setResizable(false);
        }
    }
    
    /**
     * Устанавливает сервис рекомендаций
     */
    public void setRecommendationService(SimpleMovieRecommendationService service) {
        this.recommendationService = service;
    }
    
    /**
     * Обработчик кнопки публикации оценки (используется в FXML)
     */
    @FXML
    private void handlePublish() {
        handleMovieAction(ActionType.PUBLISH_RATING);
    }

    /**
     * Обработчик кнопки отмены (используется в FXML)
     */
    @FXML
    private void handleCancel() {
        saved = false;
        closeDialog();
    }
    
    /**
     * Закрывает диалог
     */
    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }
    
    /**
     * Показывает сообщение об успехе в стиле Netflix
     */
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Успех");
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Применяем улучшенный Netflix стиль программно
        DialogPane dialogPane = alert.getDialogPane();
        
        // Устанавливаем базовые стили
        dialogPane.setStyle("-fx-background-color: #141414;");
        
        // Стилизуем кнопки
        alert.getDialogPane().getButtonTypes().forEach(buttonType -> {
            Button button = (Button) alert.getDialogPane().lookupButton(buttonType);
            button.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-font-weight: bold;");
        });
        
        // Стилизуем текст сообщения
        Label contentLabel = (Label) dialogPane.lookup(".content.label");
        if (contentLabel != null) {
            contentLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        }
        
        alert.showAndWait();
    }
    
    /**
     * Показывает ошибку в стиле Netflix
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Применяем улучшенный Netflix стиль программно
        DialogPane dialogPane = alert.getDialogPane();
        
        // Устанавливаем базовые стили
        dialogPane.setStyle("-fx-background-color: #141414;");
        
        // Стилизуем кнопки
        alert.getDialogPane().getButtonTypes().forEach(buttonType -> {
            Button button = (Button) alert.getDialogPane().lookupButton(buttonType);
            button.setStyle("-fx-background-color: #E50914; -fx-text-fill: white; -fx-font-weight: bold;");
        });
        
        // Стилизуем текст сообщения
        Label contentLabel = (Label) dialogPane.lookup(".content.label");
        if (contentLabel != null) {
            contentLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");
        }
        
        alert.showAndWait();
    }
    
    /**
     * Возвращает true если оценка была сохранена
     */
    public boolean isSaved() {
        return saved;
    }
    
    /**
     * Возвращает созданную/обновленную оценку
     */
    public UserRating getUserRating() {
        return userRating;
    }
    
    /**
     * Настраивает стили для текстовой области отзыва
     */
    private void setupReviewTextArea() {
        if (reviewTextArea != null) {
            // Программно устанавливаем стили для гарантии видимости текста
            reviewTextArea.setStyle(
                "-fx-background-color: #141414 !important;" +
                "-fx-text-fill: white !important;" +
                "-fx-prompt-text-fill: rgba(255, 255, 255, 0.5) !important;" +
                "-fx-highlight-fill: #E50914;" +
                "-fx-highlight-text-fill: white;" +
                "-fx-control-inner-background: #141414 !important;" + // Важный параметр для внутреннего контента
                "-fx-font-size: 14px;"
            );
            
            // Устанавливаем стиль для всех вложенных элементов текстовой области
            if (reviewTextArea.lookup(".content") != null) {
                reviewTextArea.lookup(".content").setStyle("-fx-background-color: #141414 !important;");
            }
            if (reviewTextArea.lookup(".text") != null) {
                reviewTextArea.lookup(".text").setStyle("-fx-fill: white !important;");
            }
            if (reviewTextArea.lookup(".viewport") != null) {
                reviewTextArea.lookup(".viewport").setStyle("-fx-background-color: #141414 !important;");
            }
            
            // Добавляем слушатель для текста, чтобы отслеживать изменения
            reviewTextArea.textProperty().addListener((observable, oldValue, newValue) -> {
                System.out.println("✏️ Текст отзыва обновлен: " + newValue);
                // Перезапускаем применение стилей после каждого изменения, чтобы гарантировать видимость
                Platform.runLater(() -> {
                    reviewTextArea.setStyle(
                        "-fx-background-color: #141414 !important;" +
                        "-fx-text-fill: white !important;" +
                        "-fx-prompt-text-fill: rgba(255, 255, 255, 0.5) !important;" +
                        "-fx-control-inner-background: #141414 !important;" +
                        "-fx-font-size: 14px;"
                    );
                });
            });

            System.out.println("🎨 Расширенные стили для текстовой области настроены");
        } else {
            System.out.println("⚠️ Текстовая область отзыва не найдена");
        }
    }
    
    /**
     * Enum для типов действий с фильмом
     */
    public enum ActionType {
        SAVE_RATING,     // Сохранить оценку
        PUBLISH_RATING,  // Опубликовать оценку
        WATCH_ONLY       // Отметить только как просмотренное
    }
    
    /**
     * Объединенный метод обработки всех действий с фильмом
     * Устраняет дублирование логики между handleSave, handlePublish и handleWatched
     */
    private void handleMovieAction(ActionType actionType) {
        System.out.println("🎬 Обрабатываем действие: " + actionType);
        
        if (movie == null || currentUser == null) {
            System.out.println("❌ Недостаточно данных: movie=" + movie + ", user=" + currentUser);
            showError("Ошибка данных");
            return;
        }
        
        try {
            double rating = 0.0;
            String reviewText = null;
            boolean isPublish = false;
            String actionDescription = "";
            String successMessage = "";
            final int delayMs;
            
            // Настройки в зависимости от типа действия
            switch (actionType) {
                case SAVE_RATING -> {
                    rating = currentStarRating;
                    reviewText = reviewTextArea.getText().trim();
                    if (reviewText.isEmpty()) reviewText = null;
                    actionDescription = "сохранение оценки";
                    successMessage = getRatingEmoji(currentStarRating) + " Ваша оценка " + currentStarRating + " из 5 звёзд успешно сохранена!";
                    delayMs = 1000;
                }
                case PUBLISH_RATING -> {
                    reviewText = reviewTextArea.getText().trim();
                    if (reviewText.isEmpty()) {
                        showError("Для публикации необходимо написать отзыв о фильме");
                        return;
                    }
                    rating = currentStarRating;
                    isPublish = true;
                    actionDescription = "публикацию оценки";
                    successMessage = getRatingEmoji(currentStarRating) + " Ваша оценка " + currentStarRating + " из 5 звёзд и отзыв успешно опубликованы!\n\nТеперь другие пользователи смогут видеть вашу рецензию.";
                    delayMs = 1500;
                }
                case WATCH_ONLY -> {
                    rating = 0.0; // 0.0 означает "без оценки"
                    reviewText = null;
                    actionDescription = "отметку просмотра";
                    successMessage = "Фильм \"" + movie.getTitle() + "\" добавлен в список просмотренных.\nЭто поможет улучшить рекомендации для вас.";
                    delayMs = 1000;
                }
                default -> throw new IllegalArgumentException("Неизвестный тип действия: " + actionType);
            }
            
            System.out.println("⭐ Рейтинг: " + rating);
            System.out.println("📝 Отзыв: " + (reviewText != null ? reviewText : "отсутствует"));
            System.out.println("📢 Опубликовано: " + isPublish);
            
            boolean success;
            
            if (actionType == ActionType.WATCH_ONLY) {
                // Для просмотра без оценки используем объединенный метод
                success = recommendationService.processMovieAction(
                    currentUser.getId(), 
                    movie.getId(), 
                    SimpleMovieRecommendationService.MovieAction.WATCH_ONLY, 
                    null, 
                    null
                );
            } else {
                // Для оценок используем объединенный метод
                SimpleMovieRecommendationService.MovieAction movieAction = 
                    isPublish ? SimpleMovieRecommendationService.MovieAction.RATE_AND_PUBLISH 
                              : SimpleMovieRecommendationService.MovieAction.RATE_ONLY;
                
                if (recommendationService != null) {
                    success = recommendationService.processMovieAction(
                        currentUser.getId(), 
                        movie.getId(), 
                        movieAction, 
                        rating, 
                        reviewText
                    );
                    
                    // Создаем UserRating для локального использования
                    if (success) {
                        userRating = new UserRating(currentUser.getId(), movie.getId(), rating, reviewText);
                        userRating.setPublished(isPublish);
                        System.out.println("🎯 Создан объект UserRating: " + userRating);
                    }
                } else {
                    showError("Сервис недоступен");
                    return;
                }
            }
            
            if (success) {
                System.out.println("✅ " + actionDescription + " выполнена успешно");
                showSuccess(successMessage);
                
                // Обновляем состояние фильма
                if (actionType != ActionType.WATCH_ONLY) {
                    movie.setUserRating(userRating);
                }
                movie.setWatched(true);
                
                // Проверяем рекомендацию для повторного просмотра
                if (rating >= 4.0) {
                    movie.setRewatch(true);
                    System.out.println("🔄 Фильм рекомендован для повторного просмотра");
                }
                
                saved = true;
                System.out.println("🎉 Процесс " + actionDescription + " завершен успешно");
                
                // Закрываем диалог с задержкой
                new Thread(() -> {
                    try {
                        Thread.sleep(delayMs);
                        javafx.application.Platform.runLater(this::closeDialog);
                    } catch (InterruptedException e) {
                        javafx.application.Platform.runLater(this::closeDialog);
                    }
                }).start();
                
            } else {
                System.out.println("❌ Ошибка " + actionDescription);
                showError("Не удалось выполнить " + actionDescription);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Ошибка при обработке действия: " + e.getMessage());
            showError("Ошибка: " + e.getMessage());
        }
    }
}
