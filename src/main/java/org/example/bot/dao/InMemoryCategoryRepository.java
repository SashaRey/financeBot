package org.example.bot.dao;

import org.example.bot.model.Category;
import org.example.bot.model.TransactionType;
import org.example.bot.repository.CategoryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory реализация репозитория категорий.
 */
public class InMemoryCategoryRepository implements CategoryRepository {
    private final Map<Long, Category> storage = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1);

    @Override
    public Optional<Category> findById(Long id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Category> findByUserId(Long userId) {
        if (userId == null) return new ArrayList<>();
        return storage.values().stream()
                .filter(c -> userId.equals(c.getUserId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Category> findGlobalByType(TransactionType type) {
        return storage.values().stream()
                .filter(c -> c.getUserId() == null && c.getType() == type)
                .collect(Collectors.toList());
    }

    @Override
    public Category save(Category category) {
        long id = seq.getAndIncrement();
        category.setId(id);
        storage.put(id, category);
        return category;
    }
}
