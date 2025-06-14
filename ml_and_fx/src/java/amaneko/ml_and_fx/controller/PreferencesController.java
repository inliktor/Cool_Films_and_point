package amaneko.ml_and_fx.controller;

import amaneko.ml_and_fx.model.*;
import amaneko.ml_and_fx.service.*;
import amaneko.ml_and_fx.ml.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;

/**
 * Контроллер для экрана настройки предпочтений пользователя
 */
public class PreferencesController implements Initializable {
    
    @FXML private FlowPane genreFlowPane;
    @FXML private ScrollPane genreScrollPane;
    @FXML private Slider minRatingSlider;
    @FXML private Label minRatingLabel;
    @FXML private CheckBox includeWatchedCheckBox;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    @FXML private Label statusLabel;
    
    private SimpleMovieRecommendationService recommendationService;
    private List<Genre> allGenres = new ArrayList<>();
    private Map<String, ToggleButton> genreButtons = new HashMap<>();
    private UserPreferences userPreferences;
    private User currentUser;
    private MovieRecommendationApp app;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Сервис будет установлен через setApp()
        
        // Настраиваем слайдер рейтинга
        minRatingSlider.setMin(1.0);
        minRatingSlider.setMax(10.0);
        minRatingSlider.setValue(6.0);
        minRatingSlider.setShowTickLabels(true);
        minRatingSlider.setShowTickMarks(true);
        minRatingSlider.setMajorTickUnit(1.0);
        
        // Обновляем текст при изменении слайдера
        minRatingSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            minRatingLabel.setText("⭐ Минимальный рейтинг: " + String.format("%.1f", newVal.doubleValue()));
        });
        minRatingLabel.setText("⭐ Минимальный рейтинг: 6.0");
        
        // Устанавливаем значения по умолчанию
        includeWatchedCheckBox.setSelected(true);
        
        // Настраиваем FlowPane для жанров
        genreFlowPane.setHgap(10);
        genreFlowPane.setVgap(10);
        
        // CSS классы применятся автоматически через FXML
        // Убедимся, что стили применяются
        genreScrollPane.getStyleClass().add("genre-scroll-pane");
        genreFlowPane.getStyleClass().add("genre-flow-pane");
    }
    
    /**
     * Загружает все доступные жанры и создает кнопки-переключатели
     */
    private void loadGenres() {
        try {
            allGenres = recommendationService.getAllGenres();
            genreFlowPane.getChildren().clear();
            genreButtons.clear();
            
            for (Genre genre : allGenres) {
                ToggleButton genreButton = new ToggleButton(genre.getName());
                
                // Применяем CSS класс вместо прямого стилизования
                genreButton.getStyleClass().add("genre-toggle-button");
                
                genreButton.setUserData(genre);
                genreButtons.put(genre.getName(), genreButton);
                genreFlowPane.getChildren().add(genreButton);
                
                // Нет необходимости в обработчиках стилей - CSS сделает всё сам
            }
            
            statusLabel.setText("Загружено " + allGenres.size() + " жанров");
        } catch (Exception e) {
            statusLabel.setText("Ошибка загрузки жанров: " + e.getMessage());
            e.printStackTrace(); // Для отладки
        }
    }
    
    /**
     * Устанавливает текущего пользователя и загружает его предпочтения
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            System.out.println("👤 Загружаем предпочтения для пользователя: " + user.getUsername());
            
            // Загружаем сохраненные предпочтения из БД
            userPreferences = recommendationService.getUserPreferences(user.getId());
            if (userPreferences == null) {
                System.out.println("📝 Создаем новые предпочтения для пользователя");
                userPreferences = new UserPreferences(user.getId());
            } else {
                System.out.println("✅ Загружены предпочтения из кэша/базы");
            }
            
            updateUI();
        }
    }
    
    /**
     * Устанавливает ссылку на главное приложение
     */
    public void setApp(MovieRecommendationApp app) {
        this.app = app;
        // Получаем общий сервис рекомендаций
        this.recommendationService = app.getRecommendationService();
        // Загружаем жанры теперь, когда сервис доступен
        loadGenres();
    }
    
    /**
     * Обновляет интерфейс в соответствии с текущими предпочтениями
     */
    private void updateUI() {
        if (userPreferences != null) {
            System.out.println("🔄 Обновляем UI с предпочтениями пользователя");
            
            // Устанавливаем выбранные жанры
            for (Genre preferredGenre : userPreferences.getPreferredGenres()) {
                String genreName = preferredGenre.getName();
                if (genreButtons.containsKey(genreName)) {
                    genreButtons.get(genreName).setSelected(true);
                    System.out.println("✅ Жанр " + genreName + " отмечен как предпочтительный");
                }
            }
            
            // Обновляем слайдер и чекбокс
            minRatingSlider.setValue(userPreferences.getMinRating());
            includeWatchedCheckBox.setSelected(userPreferences.isIncludeWatchedMovies());
            
            System.out.println("📊 UI обновлен: минРейтинг=" + userPreferences.getMinRating() + 
                             ", включатьПросмотренные=" + userPreferences.isIncludeWatchedMovies());
        }
    }
    
    /**
     * Обработчик кнопки сохранения
     */
    @FXML
    private void handleSave() {
        try {
            System.out.println("💾 Сохраняем предпочтения пользователя...");
            
            // Собираем выбранные жанры
            userPreferences.getPreferredGenres().clear();
            for (ToggleButton button : genreButtons.values()) {
                if (button.isSelected()) {
                    Genre genre = (Genre) button.getUserData();
                    userPreferences.addPreferredGenre(genre);
                    System.out.println("✅ Выбран жанр: " + genre.getName());
                }
            }
            
            // Устанавливаем остальные настройки
            userPreferences.setMinRating(minRatingSlider.getValue());
            userPreferences.setIncludeWatchedMovies(includeWatchedCheckBox.isSelected());
            
            System.out.println("⭐ Минимальный рейтинг: " + userPreferences.getMinRating());
            System.out.println("👁️ Включать просмотренные: " + userPreferences.isIncludeWatchedMovies());
            
            // Сохраняем предпочтения через сервис
            recommendationService.saveUserPreferences(userPreferences);
            
            // Обновляем статус
            statusLabel.setText("✅ Предпочтения сохранены!");
            
            // Создаем модальный alert с Netflix стилем
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Предпочтения сохранены");
            alert.setHeaderText(null);
            
            // Настраиваем alert с темным стилем
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStylesheets().add(getClass().getResource("/amaneko/ml_and_fx/netflix-styles.css").toExternalForm());
            dialogPane.getStyleClass().add("netflix-alert");
            
            String message = "Ваши предпочтения сохранены:\n\n";
            message += "• Выбрано жанров: " + userPreferences.getPreferredGenres().size() + "\n";
            message += "• Минимальный рейтинг: " + String.format("%.1f", userPreferences.getMinRating()) + "\n";
            message += "• Включать просмотренные: " + (userPreferences.isIncludeWatchedMovies() ? "Да" : "Нет");
            
            alert.setContentText(message);
            alert.showAndWait();
            
        } catch (Exception e) {
            statusLabel.setText("❌ Ошибка сохранения: " + e.getMessage());
            e.printStackTrace(); // Для отладки
        }
    }
    
    /**
     * Обработчик кнопки отмены
     */
    @FXML
    private void handleCancel() {
        try {
            // Возвращаемся на главный экран
            if (app != null) {
                app.showMainScreen();
            }
        } catch (Exception e) {
            statusLabel.setText("❌ Ошибка возврата: " + e.getMessage());
        }
    }
    
    /**
     * Возвращает текущие предпочтения пользователя
     */
    public UserPreferences getUserPreferences() {
        return userPreferences;
    }
    
    /**
     * Закрытие ресурсов
     */
    public void shutdown() {
        if (recommendationService != null) {
            recommendationService.shutdown();
        }
    }
}
