package org.example.bot.dao;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Управляет подключением к SQLite-файлу и инициализацией схемы из resources/schema.sql.
 */
public class DatabaseManager implements AutoCloseable {
    private final String dbUrl;
    private final String dbFilePath;
    private final Connection connection;

    public DatabaseManager(String dbFilePath) {
        this.dbFilePath = dbFilePath;
        this.dbUrl = "jdbc:sqlite:" + dbFilePath;
        try {
            this.connection = DriverManager.getConnection(dbUrl);
            // Явно включаем auto-commit для простоты (каждый запрос сразу коммитится)
            this.connection.setAutoCommit(true);
            // Логируем путь к файлу БД для отладки
            System.out.println("[DatabaseManager] Connected to DB URL: " + dbUrl);
            // Включаем foreign keys
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
            initSchemaIfNeeded();
            // Убедимся, что схема записана
            try (Statement stmt = connection.createStatement()) { stmt.execute("PRAGMA schema_version;"); }
        } catch (SQLException e) {
            throw new RuntimeException("Не удалось инициализировать базу данных: " + e.getMessage(), e);
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public String getDbFilePath() {
        return dbFilePath;
    }

    private void initSchemaIfNeeded() {
        // Загружаем schema.sql из ресурсов и выполняем команды по очереди
        try (InputStream is = DatabaseManager.class.getResourceAsStream("/schema.sql")) {
            if (is == null) {
                throw new RuntimeException("schema.sql не найден в ресурсах");
            }
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            String sql = sb.toString();
            // Разделяем по точке с запятой и выполняем каждый оператор отдельно
            String[] parts = sql.split(";\s*(?=\n|$)");
            try (Statement stmt = connection.createStatement()) {
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (trimmed.isEmpty()) continue;
                    stmt.execute(trimmed);
                }
            }
            // Явный commit на всякий случай
            try {
                if (!connection.getAutoCommit()) connection.commit();
            } catch (SQLException ignore) {}
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при инициализации схемы БД: " + e.getMessage(), e);
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            // логируем и продолжаем
            System.err.println("Ошибка при закрытии соединения БД: " + e.getMessage());
        }
    }
}
