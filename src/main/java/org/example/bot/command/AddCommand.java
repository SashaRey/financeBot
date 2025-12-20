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
 * Команда /add [сумма] [категория] [описание]
 * Пример: /add 150.50 Еда "Обед в столовой"
 */
public class AddCommand implements Command {
    private final UserService userService;
    private final CategoryService categoryService;
    private final TransactionService transactionService;

    private final ConversationManager conversationManager;

    public AddCommand(UserService userService, CategoryService categoryService, TransactionService transactionService, ConversationManager conversationManager) {
        this.userService = userService;
        this.categoryService = categoryService;
        this.transactionService = transactionService;
        this.conversationManager = conversationManager;
    }

    @Override
    public CommandResult execute(Update update, String[] args) {
        Long chatId = update.getMessage().getChatId();
        User u = userService.registerOrGet(chatId, null, null);

        if (args == null || args.length == 0) {
            // запускаем интерактивный режим через ConversationManager
            return conversationManager.startTransaction(chatId, TransactionType.EXPENSE);
        }

        if (args.length < 2) {
            return CommandResult.text("❌ Неверный формат. Используйте: /add [сумма] [категория] [описание(optional)]");
        }

        String sumStr = args[0];
        String category = args[1];
        String description = "";
        if (args.length > 2) {
            StringBuilder sb = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                if (i > 2) sb.append(' ');
                sb.append(args[i]);
            }
            description = sb.toString();
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(sumStr);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return CommandResult.text("❌ Сумма должна быть больше нуля");
            }
        } catch (NumberFormatException ex) {
            return CommandResult.text("❌ Неверная сумма. Пример: /add 150.50 Еда");
        }

        Category cat = categoryService.ensureCategoryExists(category, TransactionType.EXPENSE, u.getId());
        transactionService.addTransaction(u.getId(), amount, TransactionType.EXPENSE, cat.getId(), description);

        BigDecimal balance = transactionService.getBalance(u.getId(), java.time.Instant.EPOCH, java.time.Instant.now());
        return CommandResult.text("✅ Добавлен расход:\n" +
            "💸 Сумма: " + amount + "\n" +
            "📁 Категория: " + cat.getName() + "\n" +
            "💰 Баланс: " + balance);
    }
}
