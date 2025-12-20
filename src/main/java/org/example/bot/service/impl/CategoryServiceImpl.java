package org.example.bot.service.impl;

import org.example.bot.model.Category;
import org.example.bot.model.TransactionType;
import org.example.bot.repository.CategoryRepository;
import org.example.bot.service.CategoryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Реализация CategoryService на основе CategoryRepository.
 */
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<Category> listCategories(Long userId, TransactionType type) {
        List<Category> result = new ArrayList<>();
        // Глобальные категории
        result.addAll(categoryRepository.findGlobalByType(type));
        // Пользовательские
        if (userId != null) {
            result.addAll(categoryRepository.findByUserId(userId));
        }
        return result;
    }

    @Override
    public Category ensureCategoryExists(String name, TransactionType type, Long userId) {
        String lower = name.trim().toLowerCase(Locale.ROOT);
        // ищем в пользовательских
        if (userId != null) {
            Optional<Category> found = categoryRepository.findByUserId(userId).stream()
                    .filter(c -> c.getName() != null && c.getName().trim().toLowerCase(Locale.ROOT).equals(lower))
                    .findFirst();
            if (found.isPresent()) return found.get();
        }
        // ищем в глобальных
        Optional<Category> foundGlobal = categoryRepository.findGlobalByType(type).stream()
                .filter(c -> c.getName() != null && c.getName().trim().toLowerCase(Locale.ROOT).equals(lower))
                .findFirst();
        if (foundGlobal.isPresent()) return foundGlobal.get();

        Category c = new Category(name.trim(), type, userId);
        return categoryRepository.save(c);
    }

    @Override
    public Optional<Category> findById(Long id) {
        return categoryRepository.findById(id);
    }

    @Override
    public void initDefaultsIfNeeded() {
        // Проверяем глобальные категории для EXPENSE и INCOME
        var exp = categoryRepository.findGlobalByType(TransactionType.EXPENSE);
        if (exp.isEmpty()) {
            categoryRepository.save(new Category("Еда", TransactionType.EXPENSE, null));
            categoryRepository.save(new Category("Транспорт", TransactionType.EXPENSE, null));
            categoryRepository.save(new Category("Другое", TransactionType.EXPENSE, null));
        }
        var inc = categoryRepository.findGlobalByType(TransactionType.INCOME);
        if (inc.isEmpty()) {
            categoryRepository.save(new Category("Зарплата", TransactionType.INCOME, null));
            categoryRepository.save(new Category("Подарок", TransactionType.INCOME, null));
        }
    }
}
