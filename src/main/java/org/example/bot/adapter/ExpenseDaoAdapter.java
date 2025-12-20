package org.example.bot.adapter;

import org.example.bot.Expense;
import org.example.bot.ExpenseDao;
import org.example.bot.model.Transaction;
import org.example.bot.model.TransactionType;
import org.example.bot.model.User;
import org.example.bot.service.CategoryService;
import org.example.bot.service.TransactionService;
import org.example.bot.service.UserService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Адаптер между старым интерфейсом ExpenseDao и новым сервисным слоем.
 * Позволяет не менять `TelegramBot` сразу — достаточно заменить реализацию в `Main`.
 */
public class ExpenseDaoAdapter implements ExpenseDao {
    private final UserService userService;
    private final CategoryService categoryService;
    private final TransactionService transactionService;

    public ExpenseDaoAdapter(UserService userService, CategoryService categoryService, TransactionService transactionService) {
        this.userService = userService;
        this.categoryService = categoryService;
        this.transactionService = transactionService;
    }

    @Override
    public void addExpense(Long chatId, double amount, String category) {
        // Регистрируем/получаем пользователя по telegramId (используем chatId)
        User u = userService.registerOrGet(chatId, null, null);
        // Находим или создаём категорию
        var cat = categoryService.ensureCategoryExists(category, TransactionType.EXPENSE, u.getId());
        // Создаём транзакцию
        transactionService.addTransaction(u.getId(), BigDecimal.valueOf(amount), TransactionType.EXPENSE, cat.getId(), null);
    }

    @Override
    public List<Expense> getExpenses(Long chatId) {
        // Берем последние 30 дней по умолчанию
        User u = userService.registerOrGet(chatId, null, null);
        Instant from = Instant.now().minusSeconds(60L * 60 * 24 * 30);
        Instant to = Instant.now();
        List<Transaction> txs = transactionService.listTransactions(u.getId(), from, to);
        return txs.stream().map(t -> {
            String catName = categoryService.findById(t.getCategoryId()).map(c -> c.getName()).orElse("Без категории");
            return new Expense(t.getAmount().doubleValue(), catName);
        }).collect(Collectors.toList());
    }

    @Override
    public double getBalance(Long chatId) {
        User u = userService.registerOrGet(chatId, null, null);
        Instant from = Instant.EPOCH;
        Instant to = Instant.now();
        BigDecimal bal = transactionService.getBalance(u.getId(), from, to);
        return bal.doubleValue();
    }
}
