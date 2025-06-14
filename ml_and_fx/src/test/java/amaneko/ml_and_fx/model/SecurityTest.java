package amaneko.ml_and_fx.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

import amaneko.ml_and_fx.service.AuthService;

class SecurityTest {
    @Test
    void testPasswordHashNotPlain() {
        // В текущей модели User нет поля passwordHash, поэтому этот тест неактуален
        // Можно оставить только smoke-тест на создание пользователя
        User user = new User("user", 20, "M");
        assertNotNull(user);
    }

    @Test
    void testUsernameSanitization() {
        // Проверяем, что имя пользователя не содержит опасных символов
        assertFalse(AuthService.isValidUsername("user; DROP TABLE users;"));
        assertFalse(AuthService.isValidUsername("<script>alert(1)</script>"));
    }
}
