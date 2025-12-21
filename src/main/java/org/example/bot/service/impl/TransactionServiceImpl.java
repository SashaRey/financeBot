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
    private final org.example.bot.service.CategoryService categoryService;

    public TransactionServiceImpl(TransactionRepository transactionRepository, org.example.bot.service.CategoryService categoryService) {
        this.transactionRepository = transactionRepository;
        this.categoryService = categoryService;
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

    @Override
    public String getHistory(Long userId, int limit) {
        var list = transactionRepository.findLastTransactions(userId, limit);
        if (list == null || list.isEmpty()) return "История пуста";
        StringBuilder sb = new StringBuilder();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(java.time.ZoneId.systemDefault());
        int idx = 0;
        for (var tx : list) {
            idx++;
            String date = tx.getTimestamp() != null ? fmt.format(tx.getTimestamp()) : "--.--";
            String sign = tx.getType() == TransactionType.EXPENSE ? "-" : "+";
            String cat = "";
            try {
                if (tx.getCategoryId() != null) cat = " (" + categoryService.findById(tx.getCategoryId()).map(org.example.bot.model.Category::getName).orElse("Неизвестная") + ")";
            } catch (Exception ignore) {}
            sb.append(idx).append(") ").append(date).append(" — ").append(sign).append(tx.getAmount()).append(" ₽").append(cat)
                    .append("\n").append("/del_").append(tx.getId()).append("\n\n");
        }
        return sb.toString();
    }

    @Override
    public boolean deleteTransaction(Long userId, Long txId) {
        var maybe = transactionRepository.findById(txId);
        if (maybe.isEmpty()) return false;
        var tx = maybe.get();
        if (!tx.getUserId().equals(userId)) throw new IllegalArgumentException("Нет доступа к этой транзакции");
        return transactionRepository.deleteById(txId);
    }

    @Override
    public java.util.List<Transaction> findLast(Long userId, int limit) {
        return transactionRepository.findLastTransactions(userId, limit);
    }
}
