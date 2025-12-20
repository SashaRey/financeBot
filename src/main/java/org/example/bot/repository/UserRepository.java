package org.example.bot.repository;

import org.example.bot.model.User;

import java.util.Optional;

/**
 * Интерфейс репозитория пользователей.
 * Отделяет слой доступа к данным от бизнес-логики (Repository pattern).
 */
public interface UserRepository {
    Optional<User> findByTelegramId(Long telegramId);
    User save(User user);
    void update(User user);
}
