package org.example.bot.service.impl;

import org.example.bot.model.User;
import org.example.bot.repository.UserRepository;
import org.example.bot.service.UserService;

import java.util.Optional;

/**
 * Простая реализация UserService, использует UserRepository.
 */
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User registerOrGet(Long telegramId, String username, String displayName) {
        Optional<User> exist = userRepository.findByTelegramId(telegramId);
        if (exist.isPresent()) return exist.get();

        User u = new User(telegramId, username, displayName);
        return userRepository.save(u);
    }

    @Override
    public Optional<User> findByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }
}
