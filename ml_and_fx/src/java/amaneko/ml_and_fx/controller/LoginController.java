package amaneko.ml_and_fx.controller;

import java.net.URL;
import java.util.ResourceBundle;

import amaneko.ml_and_fx.model.User;
import amaneko.ml_and_fx.service.AuthService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

// Контроллер для входа и регистрации
public class LoginController implements Initializable {

    // Элементы интерфейса
    @FXML private VBox formContainer;
    @FXML private VBox loginForm;
    @FXML private VBox registerForm;
    @FXML private Button loginTabButton;
    @FXML private Button registerTabButton;

    // Поля для входа
    @FXML private TextField loginUsernameField;
    @FXML private PasswordField loginPasswordField;
    @FXML private Button loginButton;

    // Поля для регистрации
    @FXML private TextField registerUsernameField;
    @FXML private TextField ageField;
    @FXML private PasswordField registerPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button registerButton;

    @FXML private Label messageLabel;
    @FXML private ProgressIndicator loadingIndicator;

    // Переменные для работы
    private AuthService authService;
    private MovieRecommendationApp app;

    // Запускается когда открывается экран
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Показываем форму входа
        showLoginForm();
        setupEnterKeyHandlers();
    }

    // Устанавливаем главное приложение
    public void setApp(MovieRecommendationApp app) {
        this.app = app;
        if (app.getRecommendationService() != null && app.getRecommendationService().getDbConnection() != null) {
             this.authService = new AuthService(app.getRecommendationService().getDbConnection());
        } else {
            System.err.println("LoginController: RecommendationService or its DB connection is not available.");
            showMessage("Ошибка инициализации сервиса аутентификации", true);
            loginButton.setDisable(true);
            registerButton.setDisable(true);
        }
    }

    // Настраиваем клавишу Enter для полей
    private void setupEnterKeyHandlers() {
        // Для входа - переход между полями и вход
        loginUsernameField.setOnAction(e -> loginPasswordField.requestFocus());
        loginPasswordField.setOnAction(e -> handleLogin());

        // Для регистрации - переход между полями и регистрация
        registerUsernameField.setOnAction(e -> ageField.requestFocus());
        ageField.setOnAction(e -> registerPasswordField.requestFocus());
        registerPasswordField.setOnAction(e -> confirmPasswordField.requestFocus());
        confirmPasswordField.setOnAction(e -> handleRegister());
    }
    
    // Обработчик кнопки "Вход"
    @FXML
    private void showLoginForm() {
        // Меняем классы стилей кнопок
        loginTabButton.getStyleClass().clear();
        loginTabButton.getStyleClass().addAll("netflix-tab", "netflix-tab-active");
        registerTabButton.getStyleClass().clear();
        registerTabButton.getStyleClass().add("netflix-tab");
        
        // Показываем форму входа
        loginForm.setVisible(true);
        registerForm.setVisible(false);
        
        // Очищаем поля и прячем сообщения
        clearFields();
        hideMessage();
    }
    
    // Обработчик кнопки "Регистрация"
    @FXML
    private void showRegisterForm() {
        // Меняем классы стилей кнопок
        registerTabButton.getStyleClass().clear();
        registerTabButton.getStyleClass().addAll("netflix-tab", "netflix-tab-active");
        loginTabButton.getStyleClass().clear();
        loginTabButton.getStyleClass().add("netflix-tab");
        
        // Показываем форму регистрации
        registerForm.setVisible(true);
        loginForm.setVisible(false);
        
        // Очищаем поля и прячем сообщения
        clearFields();
        hideMessage();
    }

    // Обработчик кнопки входа
    @FXML
    private void handleLogin() {
        // Проверяем что сервис работает
        if (authService == null) {
            showMessage("Сервис аутентификации не инициализирован", true);
            return;
        }

        // Получаем данные из полей
        String usernameOrEmail = loginUsernameField.getText().trim();
        String password = loginPasswordField.getText();

        // Проверяем что поля заполнены
        if (usernameOrEmail.isEmpty()) {
            showMessage("Введите логин или email", true);
            return;
        }

        if (password.isEmpty()) {
            showMessage("Введите пароль", true);
            return;
        }

        // Показываем загрузку
        setLoginInProgress(true);

        // Пробуем войти
        try {
            User user = authService.authenticateUser(usernameOrEmail, password);
            
            // Прячем загрузку
            setLoginInProgress(false);

            if (user != null) {
                // Успешно вошли
                app.setCurrentUser(user);
                try {
                    app.showMainScreen();
                } catch (Exception e) {
                    e.printStackTrace();
                    showMessage("Ошибка загрузки главного экрана: " + e.getMessage(), true);
                }
            } else {
                showMessage("Неверное имя пользователя или пароль", true);
            }
        } catch (Exception e) {
            setLoginInProgress(false);
            showMessage("Ошибка при входе: " + e.getMessage(), true);
        }
    }

    // Обработчик кнопки регистрации
    @FXML
    private void handleRegister() {
        // Проверяем что сервис работает
        if (authService == null) {
            showMessage("Сервис аутентификации не инициализирован", true);
            return;
        }

        // Получаем данные из полей
        String username = registerUsernameField.getText().trim();
        String ageText = ageField.getText().trim();
        String password = registerPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Проверяем имя пользователя
        if (!AuthService.isValidUsername(username)) {
            showMessage("Имя пользователя должно содержать 3-50 символов (только буквы, цифры и _)", true);
            return;
        }

        // Проверяем возраст
        int age;
        try {
            age = Integer.parseInt(ageText);
            if (age < 1 || age > 120) {
                showMessage("Возраст должен быть от 1 до 120 лет", true);
                return;
            }
        } catch (NumberFormatException e) {
            showMessage("Введите корректный возраст", true);
            return;
        }

        // Пол необязателен - проверка убрана

        // Проверяем пароль
        if (!AuthService.isValidPassword(password)) {
            showMessage("Пароль должен содержать минимум 6 символов", true);
            return;
        }

        // Проверяем что пароли совпадают
        if (!password.equals(confirmPassword)) {
            showMessage("Пароли не совпадают", true);
            return;
        }

        // Показываем загрузку
        setRegisterInProgress(true);

        // Пробуем зарегистрироваться
        try {
            boolean success = authService.registerUser(username, password, age, null);
            
            // Прячем загрузку
            setRegisterInProgress(false);

            if (success) {
                showMessage("Регистрация успешна! Теперь вы можете войти.", false);
                clearRegisterFields();
                // Переключаемся на форму входа
                showLoginForm();
                loginUsernameField.setText(username); // Подставляем логин
            } else {
                showMessage("Пользователь с таким именем уже существует", true);
            }
        } catch (Exception e) {
            setRegisterInProgress(false);
            showMessage("Ошибка при регистрации: " + e.getMessage(), true);
        }
    }

    // Показываем/прячем загрузку для входа
    private void setLoginInProgress(boolean inProgress) {
        loginButton.setDisable(inProgress);
        loginUsernameField.setDisable(inProgress);
        loginPasswordField.setDisable(inProgress);
        loadingIndicator.setVisible(inProgress);

        if (inProgress) {
            loginButton.setText("Вход...");
        } else {
            loginButton.setText("Войти");
        }
    }

    // Показываем/прячем загрузку для регистрации
    private void setRegisterInProgress(boolean inProgress) {
        registerButton.setDisable(inProgress);
        registerUsernameField.setDisable(inProgress);
        ageField.setDisable(inProgress);
        registerPasswordField.setDisable(inProgress);
        confirmPasswordField.setDisable(inProgress);
        loadingIndicator.setVisible(inProgress);

        if (inProgress) {
            registerButton.setText("Регистрация...");
        } else {
            registerButton.setText("Зарегистрироваться");
        }
    }
    
    // Показываем сообщение пользователю
    private void showMessage(String message, boolean isError) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        
        // Используем стили Netflix для ошибок и успешных сообщений
        messageLabel.getStyleClass().clear();
        messageLabel.getStyleClass().add("netflix-message");
        if (isError) {
            messageLabel.getStyleClass().add("netflix-message-error");
        } else {
            messageLabel.getStyleClass().add("netflix-message-success");
        }
    }
    
    // Прячем сообщение
    private void hideMessage() {
        messageLabel.setVisible(false);
    }

    // Очищаем все поля
    private void clearFields() {
        clearLoginFields();
        clearRegisterFields();
    }

    // Очищаем поля входа
    private void clearLoginFields() {
        loginUsernameField.clear();
        loginPasswordField.clear();
    }

    // Очищаем поля регистрации
    private void clearRegisterFields() {
        registerUsernameField.clear();
        ageField.clear();
        registerPasswordField.clear();
        confirmPasswordField.clear();
    }
}
