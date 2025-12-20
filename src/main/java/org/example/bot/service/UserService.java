package org.example.bot.service;

import org.example.bot.model.User;

import java.util.Optional;

/**
 * Сервис для работы с пользователями.
 * Отделяет логику регистрации/поиска пользователя от репозиториев и бота.
 */
public interface UserService {
    User registerOrGet(Long telegramId, String username, String displayName);
    Optional<User> findByTelegramId(Long telegramId);
}
