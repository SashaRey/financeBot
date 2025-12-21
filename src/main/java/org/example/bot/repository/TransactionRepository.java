package org.example.bot.repository;

import org.example.bot.model.Transaction;
import org.example.bot.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {
    Transaction save(Transaction tx);
    List<Transaction> findByUserAndPeriod(Long userId, Instant from, Instant to);
    BigDecimal sumByUserAndPeriod(Long userId, Instant from, Instant to, TransactionType type);
    Optional<Transaction> findById(Long id);
    List<Transaction> findLastTransactions(Long userId, int limit);
    boolean deleteById(Long id);
}
