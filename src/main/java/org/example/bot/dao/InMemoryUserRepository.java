package org.example.bot.dao;

import org.example.bot.model.User;
import org.example.bot.repository.UserRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Простая in-memory реализация `UserRepository` для разработки и тестов.
 * Позволяет позже заменить на JDBC/ORM реализацию без изменения сервисов.
 */
public class InMemoryUserRepository implements UserRepository {
    private final Map<Long, User> byId = new ConcurrentHashMap<>();
    private final Map<Long, User> byTelegramId = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1);

    @Override
    public Optional<User> findByTelegramId(Long telegramId) {
        return Optional.ofNullable(byTelegramId.get(telegramId));
    }

    @Override
    public User save(User user) {
        long id = seq.getAndIncrement();
        user.setId(id);
        byId.put(id, user);
        if (user.getTelegramId() != null) {
            byTelegramId.put(user.getTelegramId(), user);
        }
        return user;
    }

    @Override
    public void update(User user) {
        if (user.getId() == null) return;
        byId.put(user.getId(), user);
        if (user.getTelegramId() != null) {
            byTelegramId.put(user.getTelegramId(), user);
        }
    }
}
