package org.example.bot.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Транзакция (запись дохода/расхода/перевода).
 * amount положителен; направление определяется полем type.
 */
public class Transaction {
    private Long id;
    private Long userId;
    private BigDecimal amount;
    private TransactionType type;
    private Long categoryId; // ссылка на Category.id
    private String description;
    private Instant timestamp;

    public Transaction() {
        this.timestamp = Instant.now();
    }

    public Transaction(Long userId, BigDecimal amount, TransactionType type, Long categoryId, String description) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.categoryId = categoryId;
        this.description = description;
        this.timestamp = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
