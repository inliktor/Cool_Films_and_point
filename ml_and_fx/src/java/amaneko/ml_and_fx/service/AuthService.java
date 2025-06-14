package amaneko.ml_and_fx.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;

import amaneko.ml_and_fx.model.User;

public class AuthService {

    private final Connection dbConnection;

    public AuthService(Connection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public boolean registerUser(String username, String password, int age, String gender) {
        if (userExists(username)) {
            return false;
        }
        String passwordHash = hashPassword(password);
        String insertSQL = "INSERT INTO users (username, password_hash, age, gender) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = dbConnection.prepareStatement(insertSQL)) {
            stmt.setString(1, username);
            stmt.setString(2, passwordHash);
            stmt.setInt(3, age);
            if (gender != null && !gender.isEmpty()) {
                stmt.setString(4, gender);
            } else {
                stmt.setNull(4, java.sql.Types.VARCHAR);
            }
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Пользователь " + username + " зарегистрирован");
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Ошибка регистрации пользователя: " + e.getMessage());
        }
        return false;
    }

    public User authenticateUser(String username, String password) {
        String selectSQL = "SELECT id, username, password_hash, age, gender FROM users WHERE username = ?";
        try (PreparedStatement stmt = dbConnection.prepareStatement(selectSQL)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password_hash");
                if (verifyPassword(password, storedHash)) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setUsername(rs.getString("username"));
                    user.setAge(rs.getInt("age"));
                    user.setGender(rs.getString("gender"));
                    System.out.println("✅ Пользователь " + user.getUsername() + " вошел в систему");
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Ошибка аутентификации: " + e.getMessage());
        }
        return null;
    }

    private boolean userExists(String username) {
        String selectSQL = "SELECT COUNT(*) FROM users WHERE username = ?";
        try (PreparedStatement stmt = dbConnection.prepareStatement(selectSQL)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Ошибка проверки существования пользователя: " + e.getMessage());
        }
        return false;
    }

    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Ошибка хеширования пароля", e);
        }
    }

    private boolean verifyPassword(String password, String storedHash) {
        String hashedInput = hashPassword(password);
        return hashedInput.equals(storedHash);
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isValidUsername(String username) {
        return username != null &&
               username.length() >= 3 &&
               username.length() <= 50 &&
               username.matches("^[a-zA-Z0-9_]+$");
    }
}
