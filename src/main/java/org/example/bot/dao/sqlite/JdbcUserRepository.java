package org.example.bot.dao.sqlite;

import org.example.bot.dao.DatabaseManager;
import org.example.bot.model.User;
import org.example.bot.repository.UserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.io.File;
import java.util.Date;

/**
 * JDBC-реализация UserRepository для SQLite.
 */
public class JdbcUserRepository implements UserRepository {
    private final DatabaseManager dbManager;
    private static final DateTimeFormatter SQLITE_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public JdbcUserRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Optional<User> findByTelegramId(Long telegramId) {
        String sql = "SELECT * FROM users WHERE telegram_id = ?";
        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, telegramId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        User u = mapRow(rs);
                        System.out.println("[JdbcUserRepository] Found user by telegramId=" + telegramId + " -> id=" + u.getId());
                        return Optional.of(u);
                    }
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при поиске пользователя: " + e.getMessage(), e);
        }
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users(telegram_id, username, display_name, currency, registration_date, utc_offset) VALUES(?,?,?,?,?,?)";
        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, user.getTelegramId());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getDisplayName());
            ps.setString(4, user.getCurrency());
            String reg = user.getCreatedAt() != null ? user.getCreatedAt().toString() : Instant.now().toString();
            ps.setString(5, reg);
            ps.setInt(6, user.getUtcOffset() != null ? user.getUtcOffset() : 0); // по умолчанию 0

            int affected = ps.executeUpdate();
            if (affected == 0) throw new SQLException("Создание пользователя не получилось, нет затронутых строк");
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                        System.out.println("[JdbcUserRepository] Inserted user telegramId=" + user.getTelegramId() + " id=" + user.getId());
                }
            }
                // Явный commit для надёжности
                try {
                    Connection conn2 = dbManager.getConnection();
                    if (!conn2.getAutoCommit()) conn2.commit();
                } catch (SQLException ignore) {}
                // Диагностика: покажем путь к файлу БД и информацию о файле
                try {
                    String dbPath = dbManager.getDbFilePath();
                    File f = new File(dbPath);
                    System.out.println("[JdbcUserRepository] user saved. Working dir=" + System.getProperty("user.dir"));
                    System.out.println("[JdbcUserRepository] Checking DB file: " + f.getAbsolutePath() + " exists=" + f.exists() + " size=" + (f.exists() ? f.length() : 0) + " lastModified=" + (f.exists() ? new Date(f.lastModified()) : "n/a"));
                } catch (Exception ex) {
                    System.out.println("[JdbcUserRepository] Diagnostics error: " + ex.getMessage());
                }

                return user;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при создании пользователя: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(User user) {
        if (user.getId() == null) return;
        String sql = "UPDATE users SET username = ?, display_name = ?, currency = ?, registration_date = ?, utc_offset = ? WHERE id = ?";
        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getDisplayName());
                ps.setString(3, user.getCurrency());
                ps.setString(4, user.getCreatedAt() != null ? user.getCreatedAt().toString() : Instant.now().toString());
                ps.setInt(5, user.getUtcOffset() != null ? user.getUtcOffset() : 0);
                ps.setLong(6, user.getId());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при обновлении пользователя: " + e.getMessage(), e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setTelegramId(rs.getLong("telegram_id"));
        u.setUsername(rs.getString("username"));
        u.setDisplayName(rs.getString("display_name"));
        u.setCurrency(rs.getString("currency"));
        String reg = rs.getString("registration_date");
        u.setCreatedAt(parseInstant(reg));
        try {
            Object utc = rs.getObject("utc_offset");
            if (utc != null) u.setUtcOffset(((Number) utc).intValue());
        } catch (Exception ignore) {}
        return u;
    }

    private Instant parseInstant(String dbValue) {
        if (dbValue == null) return null;
        try {
            return Instant.parse(dbValue);
        } catch (Exception ex) {
            // Попробуем формат SQLite datetime('now') => "yyyy-MM-dd HH:mm:ss"
            try {
                LocalDateTime ldt = LocalDateTime.parse(dbValue, SQLITE_DATETIME_FORMAT);
                return ldt.toInstant(ZoneOffset.UTC);
            } catch (Exception ex2) {
                // fallback: текущее время
                return Instant.now();
            }
        }
    }
}
