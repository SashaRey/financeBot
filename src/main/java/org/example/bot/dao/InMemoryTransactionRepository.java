package org.example.bot.dao;

import org.example.bot.model.Transaction;
import org.example.bot.model.TransactionType;
import org.example.bot.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * In-memory реализация хранения транзакций.
 */
public class InMemoryTransactionRepository implements TransactionRepository {
    private final Map<Long, Transaction> byId = new ConcurrentHashMap<>();
    private final Map<Long, List<Transaction>> byUser = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(1);

    @Override
    public Transaction save(Transaction tx) {
        long id = seq.getAndIncrement();
        tx.setId(id);
        byId.put(id, tx);
        byUser.putIfAbsent(tx.getUserId(), new ArrayList<>());
        byUser.get(tx.getUserId()).add(tx);
        return tx;
    }

    @Override
    public List<Transaction> findByUserAndPeriod(Long userId, Instant from, Instant to) {
        List<Transaction> list = byUser.getOrDefault(userId, new ArrayList<>());
        return list.stream()
                .filter(t -> !t.getTimestamp().isBefore(from) && !t.getTimestamp().isAfter(to))
                .collect(Collectors.toList());
    }

    @Override
    public BigDecimal sumByUserAndPeriod(Long userId, Instant from, Instant to, TransactionType type) {
        List<Transaction> list = byUser.getOrDefault(userId, new ArrayList<>());
        return list.stream()
                .filter(t -> t.getType() == type)
                .filter(t -> !t.getTimestamp().isBefore(from) && !t.getTimestamp().isAfter(to))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public List<Transaction> findLastTransactions(Long userId, int limit) {
        List<Transaction> list = byUser.getOrDefault(userId, new ArrayList<>());
        return list.stream()
                .sorted(Comparator.comparing(Transaction::getTimestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public boolean deleteById(Long id) {
        Transaction removed = byId.remove(id);
        if (removed != null) {
            List<Transaction> list = byUser.getOrDefault(removed.getUserId(), new ArrayList<>());
            list.removeIf(t -> t.getId() == id);
            return true;
        }
        return false;
    }
}
