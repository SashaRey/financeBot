package org.example.bot.model;

/**
 * Категория операций. Может быть глобальной (userId == null) или пользовательской.
 */
public class Category {
    private Long id;
    private String name;
    private TransactionType type; // EXPENSE / INCOME / TRANSFER
    private Long parentId; // для иерархии категорий
    private Long userId; // null => глобальная категория

    public Category() {}

    public Category(String name, TransactionType type, Long userId) {
        this.name = name;
        this.type = type;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
