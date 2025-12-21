package org.example.bot.dao.sqlite;

import org.example.bot.dao.DatabaseManager;
import org.example.bot.model.Transaction;
import org.example.bot.model.TransactionType;
import org.example.bot.repository.TransactionRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-реализация TransactionRepository для SQLite.
 */
public class JdbcTransactionRepository implements TransactionRepository {
    private final DatabaseManager dbManager;
    private static final DateTimeFormatter SQLITE_DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public JdbcTransactionRepository(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Transaction save(Transaction tx) {
        String sql = "INSERT INTO transactions(user_id, amount, type, category_id, description, timestamp) VALUES(?,?,?,?,?,?)";
        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, tx.getUserId());
                ps.setBigDecimal(2, tx.getAmount());
                ps.setString(3, tx.getType() != null ? tx.getType().name() : TransactionType.EXPENSE.name());
                if (tx.getCategoryId() != null) ps.setLong(4, tx.getCategoryId()); else ps.setNull(4, java.sql.Types.INTEGER);
                ps.setString(5, tx.getDescription());
                Instant ts = tx.getTimestamp() != null ? tx.getTimestamp() : Instant.now();
                ps.setString(6, ts.toString());

                int affected = ps.executeUpdate();
                if (affected == 0) throw new SQLException("Создание транзакции не получилось, нет затронутых строк");
                try (ResultSet keys = ps.getGeneratedKeys()) { if (keys.next()) tx.setId(keys.getLong(1)); }
            }
            try { if (!conn.getAutoCommit()) conn.commit(); } catch (SQLException ignore) {}
            return tx;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при сохранении транзакции: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transaction> findByUserAndPeriod(Long userId, Instant from, Instant to) {
        String sql = "SELECT * FROM transactions WHERE user_id = ? AND timestamp >= ? AND timestamp <= ? ORDER BY timestamp ASC";
        List<Transaction> res = new ArrayList<>();
        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, userId);
                ps.setString(2, from.toString());
                ps.setString(3, to.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) res.add(mapRow(rs));
                }
            }
            return res;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении транзакций: " + e.getMessage(), e);
        }
    }

    @Override
    public BigDecimal sumByUserAndPeriod(Long userId, Instant from, Instant to, TransactionType type) {
        String sql = "SELECT SUM(amount) as s FROM transactions WHERE user_id = ? AND type = ? AND timestamp >= ? AND timestamp <= ?";
        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, userId);
                ps.setString(2, type.name());
                ps.setString(3, from.toString());
                ps.setString(4, to.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        BigDecimal sum = rs.getBigDecimal("s");
                        return sum != null ? sum : BigDecimal.ZERO;
                    }
                    return BigDecimal.ZERO;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при суммировании транзакций: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        String sql = "SELECT * FROM transactions WHERE id = ?";
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
            throw new RuntimeException("Ошибка при поиске транзакции: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Transaction> findLastTransactions(Long userId, int limit) {
        String sql = "SELECT * FROM transactions WHERE user_id = ? ORDER BY timestamp DESC LIMIT ?";
        List<Transaction> res = new ArrayList<>();
        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, userId);
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) res.add(mapRow(rs));
                }
            }
            return res;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении последних транзакций: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM transactions WHERE id = ?";
        try {
            Connection conn = dbManager.getConnection();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setLong(1, id);
                int affected = ps.executeUpdate();
                try { if (!conn.getAutoCommit()) conn.commit(); } catch (SQLException ignore) {}
                return affected > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при удалении транзакции: " + e.getMessage(), e);
        }
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setId(rs.getLong("id"));
        t.setUserId(rs.getLong("user_id"));
        t.setAmount(rs.getBigDecimal("amount"));
        String typ = rs.getString("type");
        try { t.setType(TransactionType.valueOf(typ)); } catch (Exception ex) { t.setType(TransactionType.EXPENSE); }
        long cid = rs.getLong("category_id"); if (!rs.wasNull()) t.setCategoryId(cid);
        t.setDescription(rs.getString("description"));
        String dbTs = rs.getString("timestamp");
        t.setTimestamp(parseInstant(dbTs));
        return t;
    }

    private Instant parseInstant(String dbValue) {
        if (dbValue == null) return null;
        try { return Instant.parse(dbValue); } catch (Exception ex) {
            try { LocalDateTime ldt = LocalDateTime.parse(dbValue, SQLITE_DATETIME_FORMAT); return ldt.toInstant(ZoneOffset.UTC); } catch (Exception ex2) { return Instant.now(); }
        }
    }
}
