package org.example.bot.service;

import org.example.bot.model.Category;
import org.example.bot.model.TransactionType;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления категориями.
 */
public interface CategoryService {
    List<Category> listCategories(Long userId, TransactionType type);
    Category ensureCategoryExists(String name, TransactionType type, Long userId);
    Optional<Category> findById(Long id);
    /**
     * Инициализировать базовый набор глобальных категорий, если они ещё не созданы.
     * Вызывается при первом /start.
     */
    void initDefaultsIfNeeded();
}
