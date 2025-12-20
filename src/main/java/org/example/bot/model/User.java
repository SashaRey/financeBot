package org.example.bot.model;

import java.time.Instant;

/**
 * Модель пользователя бота.
 * Отдельный класс позволяет расширять профиль (валюта, настройки) без изменения логики бота.
 */
public class User {
    private Long id; // внутренний id (DB)
    private Long telegramId; // id пользователя в Telegram (уникальный)
    private String username;
    private String displayName;
    private String currency = "RUB"; // дефолтная валюта
    private Instant createdAt;
    private Integer utcOffset = 0; // смещение в минутах от UTC, пригодится для напоминаний

    public User() {
        this.createdAt = Instant.now();
    }

    public User(Long telegramId, String username, String displayName) {
        this.telegramId = telegramId;
        this.username = username;
        this.displayName = displayName;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTelegramId() {
        return telegramId;
    }

    public void setTelegramId(Long telegramId) {
        this.telegramId = telegramId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getUtcOffset() {
        return utcOffset;
    }

    public void setUtcOffset(Integer utcOffset) {
        this.utcOffset = utcOffset;
    }
}
