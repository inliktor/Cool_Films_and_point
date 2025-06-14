package amaneko.ml_and_fx.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import amaneko.ml_and_fx.model.Movie;
import amaneko.ml_and_fx.model.User;
import amaneko.ml_and_fx.model.UserPreferences;
import amaneko.ml_and_fx.model.UserRating;
import amaneko.ml_and_fx.service.SimpleMovieRecommendationService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

// Главный контроллер для экрана с фильмами
public class MainController implements Initializable {
    
    // Элементы из FXML файла
    @FXML private TextField searchField;
    @FXML private Button searchButton;
    @FXML private Button recommendButton;
    @FXML private Button clearRecommendationsButton;
    @FXML private Button preferencesButton;
    @FXML private Button rateMovieButton;
    @FXML private Button markWatchedButton;
    @FXML private Button logoutButton;
    @FXML private ListView<Movie> movieListView;
    // @FXML private ComboBox<String> movieCategoryCombo; // FXID NOT FOUND
    @FXML private TextArea movieDetailsArea;
    @FXML private ImageView moviePosterView;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label userInfoLabel;
    @FXML private Label statusLabel;
    
    // Переменные для работы с данными
    private SimpleMovieRecommendationService recommendationService;
    private ObservableList<Movie> movieList;
    private User currentUser;
    private MovieRecommendationApp app;
    
    // Этот метод запускается когда открывается экран
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Сервис будет установлен через setApp()
        
        // Создаем список для фильмов
        movieList = FXCollections.observableArrayList();
        movieListView.setItems(movieList);
        
        // Настраиваем комбо-бокс категорий
        // movieCategoryCombo.setItems(FXCollections.observableArrayList( // FXID NOT FOUND
        //     "Все фильмы", "Поиск", "Общие рекомендации", 
        //     "Персональные рекомендации", "Для повторного просмотра", "Оцененные фильмы"
        // ));
        // movieCategoryCombo.setValue("Все фильмы"); // FXID NOT FOUND
        
        // Добавляем обработчик изменения категории
        // movieCategoryCombo.setOnAction(event -> handleCategoryChange()); // FXID NOT FOUND
        
        // Настраиваем комбо-бокс типов рекомендаций
        // recommendationTypeCombo.setItems(FXCollections.observableArrayList(
        //     "Общие рекомендации", "Персональные", "Пересмотреть"
        // ));
        // recommendationTypeCombo.setValue("Общие рекомендации");
        
        // Добавляем событие когда пользователь кликает на фильм
        movieListView.setOnMouseClicked(event -> {
            Movie selectedMovie = movieListView.getSelectionModel().getSelectedItem();
            if (selectedMovie != null) {
                showMovieDetails(selectedMovie);
            }
        });
        
        // Прячем загрузку
        loadingIndicator.setVisible(false);
    }
    
    // Устанавливаем текущего пользователя
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            userInfoLabel.setText("👤 " + user.getUsername());
            
            // Устанавливаем пользователя в сервисе рекомендаций
            if (recommendationService != null) {
                recommendationService.setCurrentUser(user.getId());
            }
            
            // Показываем приветствие
            String welcomeMessage = "Добро пожаловать, " + user.getUsername() + "!\n\n";
            welcomeMessage = welcomeMessage + "🔍 Используйте поиск для поиска фильмов\n";
            welcomeMessage = welcomeMessage + "⭐ Получите персональные рекомендации\n";
            welcomeMessage = welcomeMessage + "🎬 Просматривайте информацию о фильмах\n\n";
            welcomeMessage = welcomeMessage + "Удачного просмотра!";
            movieDetailsArea.setText(welcomeMessage);
            statusLabel.setText("Пользователь: " + user.getUsername());
        }
    }
    
    // Устанавливаем главное приложение
    public void setApp(MovieRecommendationApp app) {
        this.app = app;
        // Получаем общий сервис рекомендаций
        this.recommendationService = app.getRecommendationService();
    }
    
    // Обработчик кнопки выхода
    @FXML
    private void handleLogout() {
        if (app != null) {
            try {
                // Очищаем данные пользователя
                currentUser = null;
                // Возвращаемся к экрану входа
                app.showLoginScreen();
            } catch (Exception e) {
                System.err.println("Ошибка при выходе: " + e.getMessage());
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка");
                alert.setHeaderText(null);
                alert.setContentText("Не удалось вернуться к экрану входа: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }
    
    // Обработчик кнопки поиска
    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText();
        
        // Проверяем что ввели текст для поиска
        if (searchTerm == null || searchTerm.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Ошибка");
            alert.setHeaderText(null);
            alert.setContentText("Введите название фильма для поиска");
            alert.showAndWait();
            return;
        }
        
        // Показываем что идет поиск
        loadingIndicator.setVisible(true);
        statusLabel.setText("Поиск фильмов...");
        
        // Ищем фильмы
        List<Movie> foundMovies = recommendationService.searchMovies(searchTerm, 20);
        
        // Очищаем старый список и добавляем новые фильмы
        movieList.clear();
        for (Movie movie : foundMovies) {
            movieList.add(movie);
        }
        
        // Прячем загрузку
        loadingIndicator.setVisible(false);
        
        // Показываем результат
        if (foundMovies.isEmpty()) {
            statusLabel.setText("Фильмы не найдены");
        } else {
            statusLabel.setText("Найдено " + foundMovies.size() + " фильмов");
        }
    }
    
    // Счетчик запросов рекомендаций в текущей сессии
    private int recommendationRequestCount = 0;
    
    // Объединённый обработчик рекомендаций
    @FXML
    private void handleRecommendButton() {
        // Проверяем что пользователь залогинился
        if (currentUser == null) {
            showError("Пользователь не авторизован");
            return;
        }
        
        // Увеличиваем счетчик запросов рекомендаций
        recommendationRequestCount++;
        
        // Получаем количество рекомендаций
        int count = 10;
        
        // Показываем что идет загрузка
        loadingIndicator.setVisible(true);
        
        List<Movie> recommendations;
        String statusMessage;
        
        if (recommendationRequestCount == 1) {
            System.out.println("🔄 Первый запрос рекомендаций - получаем свежие рекомендации");
            // При первом запросе сбрасываем все кэши, чтобы начать с чистого листа
            recommendationService.clearAllUserCaches(currentUser.getId());
            statusLabel.setText("Получение рекомендаций...");
        } else {
            System.out.println("🔄 Повторный запрос рекомендаций (" + recommendationRequestCount + ") - ищем новые уникальные");
            statusLabel.setText("Поиск новых уникальных рекомендаций...");
        }
        
        // Запрашиваем рекомендации (функция сама позаботится о выдаче уникальных)
        recommendations = recommendationService.getRecommendationsForUser(currentUser.getId(), count);
        
        // Очищаем старый список и добавляем рекомендации
        movieList.clear();
        movieList.addAll(recommendations);
        
        // Прячем загрузку
        loadingIndicator.setVisible(false);
        
        // Показываем результат
        if (recommendations.isEmpty()) {
            statusLabel.setText("Рекомендации не найдены");
        } else {
            if (recommendationRequestCount == 1) {
                statusMessage = "персональных рекомендаций";
            } else {
                statusMessage = "новых уникальных рекомендаций";
            }
            statusLabel.setText("Получено " + recommendations.size() + " " + statusMessage);
        }
        
        System.out.println("✅ Загружено " + recommendations.size() + " новых рекомендаций");
    }
    
    // Обработчик кнопки очистки показанных рекомендаций
    @FXML
    private void handleClearRecommendations() {
        if (currentUser == null) {
            showError("Пользователь не авторизован");
            return;
        }
        
        System.out.println("🔄 Полностью сбрасываем кэши рекомендаций для пользователя: " + currentUser.getUsername());
        
        // Полностью сбрасываем все кэши
        recommendationService.clearAllUserCaches(currentUser.getId());
        
        // Также сбрасываем фильтры жанров в предпочтениях пользователя
        UserPreferences userPreferences = recommendationService.getUserPreferences(currentUser.getId());
        if (userPreferences != null) {
            System.out.println("🎭 Сбрасываем фильтры жанров для пользователя");
            userPreferences.getPreferredGenres().clear();
            recommendationService.saveUserPreferences(userPreferences);
        }
        
        // Сбрасываем счетчик запросов рекомендаций
        recommendationRequestCount = 0;
        
        // Очищаем текущий список
        movieList.clear();
        movieDetailsArea.clear();
        moviePosterView.setImage(null);
        
        statusLabel.setText("Кэш рекомендаций и фильтры жанров полностью очищены. Теперь вы можете начать с чистого листа!");
    }
    
    // Обработчик кнопки настроек предпочтений
    @FXML
    private void handlePreferences() {
        if (app != null) {
            try {
                app.showPreferencesScreen(currentUser);
            } catch (Exception e) {
                showError("Не удалось открыть настройки: " + e.getMessage());
            }
        }
    }
    
    // Обработчик кнопки оценки фильма
    @FXML
    private void handleRateMovie() {
        System.out.println("🎬 Начинаем процесс оценки фильма...");
        
        Movie selectedMovie = movieListView.getSelectionModel().getSelectedItem();
        System.out.println("📽️ Выбранный фильм: " + (selectedMovie != null ? selectedMovie.getTitle() : "null"));
        
        if (selectedMovie == null) {
            System.out.println("❌ Фильм не выбран");
            showError("Выберите фильм для оценки");
            return;
        }
        
        System.out.println("👤 Текущий пользователь: " + (currentUser != null ? currentUser.getUsername() : "null"));
        if (currentUser == null) {
            System.out.println("❌ Пользователь не авторизован");
            showError("Пользователь не авторизован");
            return;
        }
        
        System.out.println("🚀 Приложение: " + (app != null ? "готово" : "null"));
        if (app != null) {
            try {
                System.out.println("📊 Открываем диалог оценки...");
                app.showRatingDialog(selectedMovie, currentUser);
                System.out.println("✅ Диалог оценки закрыт, обновляем данные фильма");
                
                // Обновляем фильм с новыми данными из базы данных
                Movie updatedMovie = recommendationService.loadMovieWithUserData(selectedMovie.getId(), currentUser.getId());
                if (updatedMovie != null) {
                    // Обновляем фильм в списке
                    int index = movieList.indexOf(selectedMovie);
                    if (index >= 0) {
                        movieList.set(index, updatedMovie);
                        movieListView.getSelectionModel().select(index);
                        System.out.println("🔄 Фильм в списке обновлен с новой оценкой");
                    }
                    // Показываем обновленные детали
                    showMovieDetails(updatedMovie);
                } else {
                    // Fallback - показываем старые детали
                    showMovieDetails(selectedMovie);
                }
            } catch (Exception e) {
                System.err.println("❌ Ошибка при открытии диалога: " + e.getMessage());
                e.printStackTrace();
                showError("Не удалось открыть диалог оценки: " + e.getMessage());
            }
        } else {
            System.out.println("❌ Приложение не инициализировано");
            showError("Приложение не готово");
        }
    }
    
    // Обработчик кнопки "Отметить как просмотренное"
    @FXML
    private void handleMarkWatched() {
        Movie selectedMovie = movieListView.getSelectionModel().getSelectedItem();
        if (selectedMovie == null) {
            showError("Выберите фильм для отметки как просмотренного");
            return;
        }
        
        if (currentUser == null) {
            showError("Пользователь не авторизован");
            return;
        }
        
        // Проверяем, не отмечен ли фильм уже как просмотренный
        if (recommendationService.isMovieWatchedByUser(currentUser.getId(), selectedMovie.getId())) {
            statusLabel.setText("Фильм уже отмечен как просмотренный");
            return;
        }
        
        // Отмечаем фильм как просмотренный
        recommendationService.recordMovieView(currentUser.getId(), selectedMovie.getId());
        
        // Обновляем фильм с новыми данными
        Movie updatedMovie = recommendationService.loadMovieWithUserData(selectedMovie.getId(), currentUser.getId());
        if (updatedMovie != null) {
            // Обновляем фильм в списке
            int index = movieList.indexOf(selectedMovie);
            if (index >= 0) {
                movieList.set(index, updatedMovie);
                movieListView.getSelectionModel().select(index);
            }
            // Показываем обновленные детали
            showMovieDetails(updatedMovie);
        }
        
        statusLabel.setText("✅ Фильм \"" + selectedMovie.getTitle() + "\" отмечен как просмотренный");
        System.out.println("✅ Фильм " + selectedMovie.getTitle() + " отмечен как просмотренный");
    }
    
    // Обработчик изменения категории в комбо-боксе
    private void handleCategoryChange() {
        // String selectedCategory = movieCategoryCombo.getValue(); // FXID NOT FOUND
        // if (selectedCategory == null) return; // FXID NOT FOUND

        // loadingIndicator.setVisible(true); // FXID NOT FOUND
        // statusLabel.setText("Загрузка фильмов..."); // FXID NOT FOUND

        // movieList.clear(); // FXID NOT FOUND
        // List<Movie> newMovies = null; // FXID NOT FOUND

        // switch (selectedCategory) { // FXID NOT FOUND
        //     case "Все фильмы": // FXID NOT FOUND
        //         newMovies = recommendationService.getAllMovies(50); // FXID NOT FOUND
        //         break; // FXID NOT FOUND
        //     case "Поиск": // FXID NOT FOUND
        //         // Оставляем текущие результаты поиска, если они есть
        //         // Или можно предложить пользователю ввести запрос
        //         statusLabel.setText("Введите запрос для поиска"); // FXID NOT FOUND
        //         break; // FXID NOT FOUND
        //     case "Общие рекомендации": // FXID NOT FOUND
        //         newMovies = recommendationService.getGeneralRecommendations(currentUser != null ? currentUser.getId() : -1, 20); // FXID NOT FOUND
        //         break; // FXID NOT FOUND
        //     case "Персональные рекомендации": // FXID NOT FOUND
        //         if (currentUser != null) { // FXID NOT FOUND
        //             newMovies = recommendationService.getPersonalizedRecommendations(currentUser.getId(), 20); // FXID NOT FOUND
        //         } else { // FXID NOT FOUND
        //             showError("Авторизуйтесь для персональных рекомендаций"); // FXID NOT FOUND
        //         } // FXID NOT FOUND
        //         break; // FXID NOT FOUND
        //     case "Для повторного просмотра": // FXID NOT FOUND
        //         if (currentUser != null) { // FXID NOT FOUND
        //             newMovies = recommendationService.getRewatchRecommendations(currentUser.getId(), 20); // FXID NOT FOUND
        //         } else { // FXID NOT FOUND
        //             showError("Авторизуйтесь для рекомендаций к пересмотру"); // FXID NOT FOUND
        //         } // FXID NOT FOUND
        //         break; // FXID NOT FOUND
        //     case "Оцененные фильмы": // FXID NOT FOUND
        //         if (currentUser != null) { // FXID NOT FOUND
        //             newMovies = recommendationService.getRatedMovies(currentUser.getId()); // FXID NOT FOUND
        //         } else { // FXID NOT FOUND
        //             showError("Авторизуйтесь для просмотра оцененных фильмов"); // FXID NOT FOUND
        //         } // FXID NOT FOUND
        //         break; // FXID NOT FOUND
        // } // FXID NOT FOUND

        // if (newMovies != null) { // FXID NOT FOUND
        //     movieList.addAll(newMovies); // FXID NOT FOUND
        // } // FXID NOT FOUND

        // loadingIndicator.setVisible(false); // FXID NOT FOUND
        // if (newMovies != null && newMovies.isEmpty()) { // FXID NOT FOUND
        //     statusLabel.setText("Фильмы в категории '" + selectedCategory + "' не найдены"); // FXID NOT FOUND
        // } else if (newMovies != null) { // FXID NOT FOUND
        //     statusLabel.setText("Загружено " + movieList.size() + " фильмов"); // FXID NOT FOUND
        // } // FXID NOT FOUND
    }

    // Показываем детали фильма
    private void showMovieDetails(Movie movie) {
        // Собираем информацию о фильме по частям
        String details = "";
        details = details + "🎬 " + movie.getTitle() + "\n\n";
        details = details + "📅 Год: " + movie.getReleaseYear() + "\n";
        details = details + "⭐ Рейтинг: " + String.format("%.1f", movie.getVoteAverage()) + "\n";
        details = details + "🔥 Популярность: " + String.format("%.1f", movie.getPopularity()) + "\n";
        details = details + "🎭 Жанры: " + movie.getGenres() + "\n";
        
        // Показываем пользовательскую оценку если есть
        if (currentUser != null && movie.getUserRating() != null) {
            UserRating userRating = movie.getUserRating();
            details = details + "👤 Ваша оценка: " + userRating.getRating() + "/5 ⭐";
            if (userRating.getReviewText() != null && !userRating.getReviewText().isEmpty()) {
                details = details + "\n📝 Ваш отзыв: " + userRating.getReviewText();
            }
            details = details + "\n";
        }
        
        // Если есть предсказанная оценка - добавляем её
        if (movie.getPredictedRating() > 0) {
            details = details + "🤖 Предсказанная оценка: " + String.format("%.2f", movie.getPredictedRating()) + "\n";
        }
        
        // Показываем статус фильма
        if (movie.isWatched()) {
            details = details + "✅ Просмотрено";
            if (movie.isRewatch()) {
                details = details + " | 🔄 Рекомендуется к повторному просмотру";
            }
            details = details + "\n";
        }
        
        details = details + "\n📖 Описание:\n" + movie.getOverview();
        
        // Показываем текст
        movieDetailsArea.setText(details);
        
        // Загружаем постер фильма
        loadMoviePoster(movie);
    }
    
    // Загружаем постер фильма
    private void loadMoviePoster(Movie movie) {
        // Сначала очищаем старый постер
        moviePosterView.setImage(null);
        
        // Получаем ссылку на постер
        String posterUrl = movie.getFullPosterUrl();
        
        // Если нет ссылки - выходим
        if (posterUrl == null || posterUrl.isEmpty()) {
            return;
        }
        
        // Пробуем загрузить картинку
        try {
            Image posterImage = new Image(posterUrl, true); // true = загружать в фоне
            moviePosterView.setImage(posterImage);
        } catch (Exception e) {
            System.err.println("Ошибка загрузки постера: " + e.getMessage());
            // Если ошибка - оставляем пустым
            moviePosterView.setImage(null);
        }
    }
    
    // Закрываем соединения при выходе
    public void shutdown() {
        if (recommendationService != null) {
            recommendationService.shutdown();
        }
    }
    
    // Вспомогательный метод для отображения ошибок
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}