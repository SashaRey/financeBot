package org.example.bot.service;

import org.example.bot.model.Transaction;
import org.example.bot.model.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Сервис для работы с транзакциями (бизнес-логика).
 */
public interface TransactionService {
    Transaction addTransaction(Long userId, BigDecimal amount, TransactionType type, Long categoryId, String description);
    BigDecimal getBalance(Long userId, Instant from, Instant to);
    java.util.List<Transaction> listTransactions(Long userId, Instant from, Instant to);
    Optional<Transaction> findById(Long id);
}
