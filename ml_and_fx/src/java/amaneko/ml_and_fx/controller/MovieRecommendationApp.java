package amaneko.ml_and_fx.controller;

import amaneko.ml_and_fx.model.Movie;
import amaneko.ml_and_fx.model.User;
import amaneko.ml_and_fx.service.SimpleMovieRecommendationService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MovieRecommendationApp extends Application {
    private MainController mainController;
    private LoginController loginController;
    private Stage primaryStage;
    private User currentUser;
    private SimpleMovieRecommendationService recommendationService;

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        recommendationService = new SimpleMovieRecommendationService();
        showLoginScreen();
    }

    public void showLoginScreen() throws Exception {
        currentUser = null;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/amaneko/ml_and_fx/login-view.fxml"));
        Parent root = loader.load();
        loginController = loader.getController();
        loginController.setApp(this);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/amaneko/ml_and_fx/netflix-styles.css").toExternalForm());
        primaryStage.setTitle("Вход в систему - Рекомендации фильмов");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.setResizable(true);
        if (!primaryStage.isShowing()) {
            primaryStage.show();
        }
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void showMainScreen() throws Exception {
        if (currentUser == null) {
            throw new IllegalStateException("Пользователь не установлен");
        }
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/amaneko/ml_and_fx/hello-view.fxml"));
        Parent root = loader.load();
        mainController = loader.getController();
        mainController.setCurrentUser(currentUser);
        mainController.setApp(this);
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/amaneko/ml_and_fx/simple-styles.css").toExternalForm());
        primaryStage.setTitle("Рекомендации фильмов - " + currentUser.getUsername());
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.setResizable(true);
        primaryStage.setOnCloseRequest(event -> {
            if (mainController != null) {
                mainController.shutdown();
            }
        });
    }

    public void showPreferencesScreen(User user) throws Exception {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        showSimpleFilterDialog(user);
    }

    public void showSimpleFilterDialog(User user) throws Exception {
        if (user == null) {
            throw new IllegalArgumentException("Пользователь не может быть null");
        }
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/amaneko/ml_and_fx/simple-filter.fxml"));
        Parent root = loader.load();
        SimpleFilterController filterController = loader.getController();
        filterController.setData(recommendationService, user);
        Stage filterStage = new Stage();
        Scene scene = new Scene(root);
        filterStage.setTitle("Настройки фильтров");
        filterStage.setScene(scene);
        filterStage.setResizable(false);
        filterStage.initModality(Modality.WINDOW_MODAL);
        filterStage.initOwner(primaryStage);
        filterStage.centerOnScreen();
        filterStage.showAndWait();
    }

    public void showRatingDialog(Movie movie, User user) throws Exception {
        if (movie == null || user == null) {
            throw new IllegalArgumentException("Фильм и пользователь не могут быть null");
        }
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/amaneko/ml_and_fx/rating-dialog.fxml"));
        Parent root = loader.load();
        RatingDialogController ratingController = loader.getController();
        ratingController.setMovie(movie);
        ratingController.setCurrentUser(user);
        ratingController.setRecommendationService(recommendationService);
        Stage ratingStage = new Stage();
        Scene scene = new Scene(root, 600, 550);
        root.setStyle("-fx-background-color: #141414;");
        ratingStage.setTitle("Оценить фильм");
        ratingStage.setScene(scene);
        ratingStage.setResizable(false);
        ratingStage.setMinWidth(600);
        ratingStage.setMinHeight(550);
        ratingStage.initModality(Modality.WINDOW_MODAL);
        ratingStage.initOwner(primaryStage);
        ratingStage.centerOnScreen();
        ratingController.setDialogStage(ratingStage);
        ratingStage.showAndWait();
    }

    public SimpleMovieRecommendationService getRecommendationService() {
        return recommendationService;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
