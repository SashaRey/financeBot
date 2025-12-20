package org.example.bot.command;

import org.example.bot.model.Category;
import org.example.bot.model.TransactionType;
import org.example.bot.model.User;
import org.example.bot.service.CategoryService;
import org.example.bot.service.TransactionService;
import org.example.bot.service.UserService;
import org.example.bot.conversation.ConversationManager;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.math.BigDecimal;

/**
 * Команда /income — добавление дохода; работает как /add, но с типом INCOME
 */
public class IncomeCommand implements Command {
    private final UserService userService;
    private final CategoryService categoryService;
    private final TransactionService transactionService;
    private final ConversationManager conversationManager;

    public IncomeCommand(UserService userService, CategoryService categoryService, TransactionService transactionService, ConversationManager conversationManager) {
        this.userService = userService;
        this.categoryService = categoryService;
        this.transactionService = transactionService;
        this.conversationManager = conversationManager;
    }

    @Override
    public CommandResult execute(Update update, String[] args) {
        Long chatId = update.getMessage().getChatId();
        if (args == null || args.length == 0) {
            return conversationManager.startTransaction(chatId, TransactionType.INCOME);
        }

        if (args.length < 2) {
            return CommandResult.text("❌ Неверный формат. Используйте: /income [сумма] [категория]");
        }

        String sumStr = args[0];
        String category = args[1];
        BigDecimal amount;
        try {
            amount = new BigDecimal(sumStr);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return CommandResult.text("❌ Сумма должна быть больше нуля");
            }
        } catch (NumberFormatException ex) {
            return CommandResult.text("❌ Неверная сумма. Пример: /income 1500");
        }

        User u = userService.registerOrGet(chatId, null, null);
        Category cat = categoryService.ensureCategoryExists(category, TransactionType.INCOME, u.getId());
        transactionService.addTransaction(u.getId(), amount, TransactionType.INCOME, cat.getId(), null);
        BigDecimal balance = transactionService.getBalance(u.getId(), java.time.Instant.EPOCH, java.time.Instant.now());
        return CommandResult.text("✅ Добавлен доход:\n💸 Сумма: " + amount + "\n📁 Категория: " + cat.getName() + "\n💰 Баланс: " + balance);
    }
}
