package amaneko.ml_and_fx.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class UserTest {
    @Test
    void testUserFields() {
        User user = new User("testuser", 25, "M");
        user.setId(1);
        assertEquals(1, user.getId());
        assertEquals("testuser", user.getUsername());
        assertEquals(25, user.getAge());
        assertEquals("M", user.getGender());
    }
}
