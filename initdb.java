package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public class initdb {
    private static final String DB_URL = "jdbc:postgresql://localhost:15432/postgres";
    private static final String DB_USER = "zwloader";
    private static final String DB_PASSWORD = "0010085070Pgsql";

    private static Connection dbConnection;
    private static OkHttpClient httpClient;
    
    public static void initdb() throws SQLException {
        // Инициализация соединения с базой данных
        dbConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        dbConnection.setAutoCommit(false); // Для управления транзакциями

        httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        System.out.println("Соединения установлены успешно");
    }
    
    // Метод для получения соединения с базой данных
    public static Connection getDbConnection() throws SQLException {
        if (dbConnection == null || dbConnection.isClosed()) {
            dbConnection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            dbConnection.setAutoCommit(false); // Для управления транзакциями
            System.out.println("Создано новое соединение с БД");
        }
        return dbConnection;
    }
    
    // Метод для получения HTTP клиента
    public static OkHttpClient getHttpClient() {
        return httpClient;
    }
}
