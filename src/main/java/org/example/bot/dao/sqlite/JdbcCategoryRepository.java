package org.example.bot.dao.sqlite;

import org.example.bot.dao.DatabaseManager;
import org.example.bot.model.Category;
import org.example.bot.model.TransactionType;
import org.example.bot.repository.CategoryRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-реализация CategoryRepository для SQLite.
 */
public class JdbcCategoryRepository implements CategoryRepository {
    private final DatabaseManager dbManager;

    public JdbcCategoryRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Optional<Category> findById(Long id) {
        String sql = "SELECT * FROM categories WHERE id = ?";
        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return Optional.of(mapRow(rs));
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при поиске категории: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Category> findByUserId(Long userId) {
        String sql = "SELECT * FROM categories WHERE user_id = ?";
        List<Category> res = new ArrayList<>();
        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) res.add(mapRow(rs));
                }
            }
            return res;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении категорий пользователя: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Category> findGlobalByType(TransactionType type) {
        String sql = "SELECT * FROM categories WHERE user_id IS NULL AND type = ?";
        List<Category> res = new ArrayList<>();
        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, type.name());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) res.add(mapRow(rs));
                }
            }
            return res;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении глобальных категорий: " + e.getMessage(), e);
        }
    }

    @Override
    public Category save(Category category) {
        String sql = "INSERT INTO categories(name, type, parent_id, user_id) VALUES(?,?,?,?)";
        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, category.getName());
                ps.setString(2, category.getType() != null ? category.getType().name() : TransactionType.EXPENSE.name());
                if (category.getParentId() != null) ps.setLong(3, category.getParentId()); else ps.setNull(3, java.sql.Types.INTEGER);
                if (category.getUserId() != null) ps.setLong(4, category.getUserId()); else ps.setNull(4, java.sql.Types.INTEGER);

                int affected = ps.executeUpdate();
                if (affected == 0) throw new SQLException("Создание категории не получилось, нет затронутых строк");
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) category.setId(keys.getLong(1));
                }
            }
            // commit если нужно
            try { if (!conn.getAutoCommit()) conn.commit(); } catch (SQLException ignore) {}
            return category;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при сохранении категории: " + e.getMessage(), e);
        }
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        Category c = new Category();
        c.setId(rs.getLong("id"));
        c.setName(rs.getString("name"));
        String t = rs.getString("type");
        try { c.setType(TransactionType.valueOf(t)); } catch (Exception ex) { c.setType(TransactionType.EXPENSE); }
        long pid = rs.getLong("parent_id");
        if (!rs.wasNull()) c.setParentId(pid);
        long uid = rs.getLong("user_id");
        if (!rs.wasNull()) c.setUserId(uid);
        return c;
    }
}
