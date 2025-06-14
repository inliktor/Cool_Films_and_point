package amaneko.ml_and_fx.controller;

import amaneko.ml_and_fx.model.*;
import amaneko.ml_and_fx.service.*;
import amaneko.ml_and_fx.ml.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;

/**
 * Простой контроллер для настройки фильтров
 */
public class SimpleFilterController {
    
    @FXML private FlowPane genreFlowPane;
    @FXML private Slider minRatingSlider;
    @FXML private Label minRatingLabel;
    @FXML private CheckBox includeWatchedCheckBox;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;
    
    private SimpleMovieRecommendationService recommendationService;
    private User currentUser;
    private UserPreferences currentPreferences;
    private List<Genre> availableGenres;
    private Set<ToggleButton> genreButtons;
    
    @FXML
    private void initialize() {
        // Настраиваем слайдер рейтинга
        minRatingSlider.setMin(1.0);
        minRatingSlider.setMax(10.0);
        minRatingSlider.setValue(6.0);
        minRatingSlider.setMajorTickUnit(1.0);
        minRatingSlider.setShowTickLabels(true);
        minRatingSlider.setShowTickMarks(true);
        
        // Обновляем подпись при изменении слайдера
        minRatingSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateRatingLabel(newVal.doubleValue());
        });
        
        updateRatingLabel(6.0);
        
        // Инициализируем списки
        availableGenres = new ArrayList<>();
        genreButtons = new HashSet<>();
    }
    
    public void setData(SimpleMovieRecommendationService service, User user) {
        this.recommendationService = service;
        this.currentUser = user;
        
        loadGenres();
        loadCurrentPreferences();
    }
    
    private void loadGenres() {
        if (recommendationService != null) {
            List<Genre> genres = recommendationService.getAllGenres();
            availableGenres.clear();
            availableGenres.addAll(genres);
            
            // Создаем кнопки для жанров
            createGenreButtons();
        }
    }
    
    private void createGenreButtons() {
        genreFlowPane.getChildren().clear();
        genreButtons.clear();
        
        for (Genre genre : availableGenres) {
            ToggleButton button = new ToggleButton(genre.getName());
            button.setUserData(genre);
            button.setStyle("-fx-padding: 5 10; -fx-margin: 2;");
            
            // Стиль для выбранной кнопки
            button.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                if (isSelected) {
                    button.setStyle("-fx-padding: 5 10; -fx-margin: 2; -fx-background-color: #4CAF50; -fx-text-fill: white;");
                } else {
                    button.setStyle("-fx-padding: 5 10; -fx-margin: 2;");
                }
            });
            
            genreButtons.add(button);
            genreFlowPane.getChildren().add(button);
        }
    }
    
    private void loadCurrentPreferences() {
        if (recommendationService != null && currentUser != null) {
            currentPreferences = recommendationService.getUserPreferences(currentUser.getId());
            
            if (currentPreferences != null) {
                // Устанавливаем минимальный рейтинг
                minRatingSlider.setValue(currentPreferences.getMinRating());
                updateRatingLabel(currentPreferences.getMinRating());
                
                // Устанавливаем настройку просмотренных фильмов
                includeWatchedCheckBox.setSelected(currentPreferences.isIncludeWatchedMovies());
                
                // Выбираем предпочитаемые жанры
                for (Genre preferredGenre : currentPreferences.getPreferredGenres()) {
                    for (ToggleButton button : genreButtons) {
                        Genre buttonGenre = (Genre) button.getUserData();
                        if (buttonGenre.getName().equals(preferredGenre.getName())) {
                            button.setSelected(true);
                            break;
                        }
                    }
                }
            }
        }
    }
    
    private void updateRatingLabel(double rating) {
        minRatingLabel.setText(String.format("Минимальный рейтинг: %.1f", rating));
    }
    
    @FXML
    private void handleSave() {
        if (currentPreferences == null) {
            currentPreferences = new UserPreferences(currentUser.getId());
        }
        
        // Сохраняем настройки
        currentPreferences.setMinRating(minRatingSlider.getValue());
        currentPreferences.setIncludeWatchedMovies(includeWatchedCheckBox.isSelected());
        
        // Сохраняем выбранные жанры
        List<Genre> selectedGenres = new ArrayList<>();
        for (ToggleButton button : genreButtons) {
            if (button.isSelected()) {
                selectedGenres.add((Genre) button.getUserData());
            }
        }
        currentPreferences.setPreferredGenres(selectedGenres);
        
        // Сохраняем в сервисе
        recommendationService.saveUserPreferences(currentPreferences);
        
        // Показываем подтверждение
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Успешно");
        alert.setHeaderText(null);
        alert.setContentText("Предпочтения сохранены! Теперь рекомендации будут учитывать ваши настройки.");
        alert.showAndWait();
        
        // Закрываем окно
        closeWindow();
    }
    
    @FXML
    private void handleCancel() {
        closeWindow();
    }
    
    private void closeWindow() {
        Stage stage = (Stage) saveButton.getScene().getWindow();
        stage.close();
    }
}
