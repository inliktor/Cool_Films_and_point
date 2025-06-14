package amaneko.ml_and_fx.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/*  
    Тесты для isValidPassword (проверка пароля):

isValidPassword_valid(): Проверяет, что метод AuthService.isValidPassword() правильно определяет 
валидные (корректные) пароли. Ожидается, что для "password123" и "123456" метод вернет true.

isValidPassword_invalid_tooShort(): Проверяет, что метод правильно определяет невалидный пароль, 
если он слишком короткий ("12345"). Ожидается, что метод вернет false.

isValidPassword_invalid_null(): Проверяет, что метод правильно обрабатывает ситуацию, когда вместо 
пароля передается null (отсутствие значения). Ожидается, что метод вернет false.

    Тесты для isValidUsername (проверка имени пользователя):

isValidUsername_valid(): Проверяет, что метод AuthService.isValidUsername() правильно определяет 
валидные имена пользователя ("user123", "test_user", "abc"). Ожидается, что для каждого из них метод
 вернет true.

isValidUsername_invalid_tooShort(): Проверяет невалидное имя пользователя, если оно слишком короткое
 ("us"). Ожидается false.

isValidUsername_invalid_tooLong(): Проверяет невалидное имя пользователя, если оно слишком длинное
 (строка из 51 символа 'a'). Ожидается false.

isValidUsername_invalid_characters(): Проверяет, что метод правильно определяет невалидные имена 
пользователя, содержащие недопустимые символы ("user!", "user name", "пользователь"). Для каждого 
из них ожидается false.

isValidUsername_invalid_null(): Проверяет обработку null в качестве имени пользователя. Ожидается 
false.




    assertTrue(условие, "сообщение об ошибке");


    
 */
class AuthServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceTest.class);

    // --- Тесты для isValidPassword 
    @Test
    void isValidPassword_valid() {
        logger.info("Тестирование isValidPassword с валидными данными...");
        assertTrue(AuthService.isValidPassword("password123"), "Пароль 'password123' должен быть валидным");
        logger.info("Проверка 'password123' прошла успешно.");
        assertTrue(AuthService.isValidPassword("123456"), "Пароль '123456' должен быть валидным");
        logger.info("Проверка '123456' прошла успешно.");
        logger.info("Тест isValidPassword_valid завершен.");
    }

    @Test
    void isValidPassword_invalid_tooShort() {
        logger.info("Тестирование isValidPassword со слишком коротким паролем...");
        String shortPassword = "12345";
        assertFalse(AuthService.isValidPassword(shortPassword), "Пароль '" + shortPassword + "' слишком короткий");
        logger.info("Проверка короткого пароля '" + shortPassword + "' прошла успешно.");
        logger.info("Тест isValidPassword_invalid_tooShort завершен.");
    }

    @Test
    void isValidPassword_invalid_null() {
        logger.info("Тестирование isValidPassword с null...");
        assertFalse(AuthService.isValidPassword(null), "Null пароль должен быть невалидным");
        logger.info("Проверка null пароля прошла успешно.");
        logger.info("Тест isValidPassword_invalid_null завершен.");
    }

    // --- Тесты для isValidUsername 
    @Test
    void isValidUsername_valid() {
        logger.info("Тестирование isValidUsername с валидными именами пользователя...");
        String[] validUsernames = {"user123", "test_user", "abc"};
        for (String username : validUsernames) {
            assertTrue(AuthService.isValidUsername(username), "Имя пользователя '" + username + "' должно быть валидным");
            logger.info("Проверка валидного имени '" + username + "' прошла успешно.");
        }
        logger.info("Тест isValidUsername_valid завершен.");
    }

    @Test
    void isValidUsername_invalid_tooShort() {
        logger.info("Тестирование isValidUsername со слишком коротким именем пользователя...");
        String shortUsername = "us";
        assertFalse(AuthService.isValidUsername(shortUsername), "Имя пользователя '" + shortUsername + "' слишком короткое");
        logger.info("Проверка слишком короткого имени '" + shortUsername + "' прошла успешно.");
        logger.info("Тест isValidUsername_invalid_tooShort завершен.");
    }

    @Test
    void isValidUsername_invalid_tooLong() {
        logger.info("Тестирование isValidUsername со слишком длинным именем пользователя...");
        String longUsername = "a".repeat(51);
        assertFalse(AuthService.isValidUsername(longUsername), "Имя пользователя '" + longUsername + "' слишком длинное");
        logger.info("Проверка слишком длинного имени пользователя прошла успешно.");
        logger.info("Тест isValidUsername_invalid_tooLong завершен.");
    }

    @Test
    void isValidUsername_invalid_characters() {
        logger.info("Тестирование isValidUsername с недопустимыми символами...");
        String[] invalidUsernames = {"user!", "user name", "пользователь"};
        String[] reasons = {"Спецсимвол '!' недопустим", "Пробел недопустим", "Кириллица недопустима"};
        for (int i = 0; i < invalidUsernames.length; i++) {
            String username = invalidUsernames[i];
            String reason = reasons[i];
            assertFalse(AuthService.isValidUsername(username), "Имя пользователя '" + username + "' должно быть невалидным: " + reason);
            logger.info("Проверка имени '" + username + "' с недопустимыми символами прошла успешно (" + reason + ").");
        }
        logger.info("Тест isValidUsername_invalid_characters завершен.");
    }

    @Test
    void isValidUsername_invalid_null() {
        logger.info("Тестирование isValidUsername с null...");
        assertFalse(AuthService.isValidUsername(null), "Null имя пользователя должно быть невалидным");
        logger.info("Проверка null имени пользователя прошла успешно.");
        logger.info("Тест isValidUsername_invalid_null завершен.");
    }
}
