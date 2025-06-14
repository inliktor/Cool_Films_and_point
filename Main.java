package org.example;

import java.sql.SQLException;

public class Main {


    public static void main(String[] args) throws Exception {
        // Инициализация подключений
        org.example.initdb.initdb();

        // Создаем таблицу для хранения фильмов
        org.example.createMoviesTable.createMoviesTable();

        // Получаем все фильмы с 1990 по 2025
        org.example.parse_movie.parse_movie();

        org.example.parse_movie.closeConnections();
    }
}