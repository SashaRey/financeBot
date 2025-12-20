package org.example.bot.repository;

import org.example.bot.model.Category;
import org.example.bot.model.TransactionType;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    Optional<Category> findById(Long id);
    List<Category> findByUserId(Long userId);
    List<Category> findGlobalByType(TransactionType type);
    Category save(Category category);
}
