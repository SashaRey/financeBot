package org.example.bot.service.impl;

import org.example.bot.model.Transaction;
import org.example.bot.model.TransactionType;
import org.example.bot.repository.TransactionRepository;
import org.example.bot.service.TransactionService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Реализация TransactionService. Содержит базовую бизнес-логику для добавления транзакций
 * и подсчёта баланса за период.
 */
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Transaction addTransaction(Long userId, BigDecimal amount, TransactionType type, Long categoryId, String description) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        Transaction tx = new Transaction(userId, amount, type, categoryId, description);
        return transactionRepository.save(tx);
    }

    @Override
    public BigDecimal getBalance(Long userId, Instant from, Instant to) {
        BigDecimal income = transactionRepository.sumByUserAndPeriod(userId, from, to, TransactionType.INCOME);
        BigDecimal expense = transactionRepository.sumByUserAndPeriod(userId, from, to, TransactionType.EXPENSE);
        return income.subtract(expense);
    }

    @Override
    public List<Transaction> listTransactions(Long userId, Instant from, Instant to) {
        return transactionRepository.findByUserAndPeriod(userId, from, to);
    }

    @Override
    public Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }
}
