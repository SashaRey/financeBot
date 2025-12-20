package org.example.bot.conversation;

import org.example.bot.model.Category;
import org.example.bot.model.TransactionType;
import org.example.bot.service.CategoryService;
import org.example.bot.service.TransactionService;
import org.example.bot.service.UserService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Менеджер простых интерактивных разговоров (пока только /add).
 */
public class ConversationManager {
    private final UserService userService;
    private final CategoryService categoryService;
    private final TransactionService transactionService;

    private enum Stage { WAITING_AMOUNT, WAITING_CATEGORY }

    private static class Conv {
        Stage stage;
        BigDecimal amount;
        TransactionType type;
    }

    private final Map<Long, Conv> convs = new HashMap<>();

    public ConversationManager(UserService userService, CategoryService categoryService, TransactionService transactionService) {
        this.userService = userService;
        this.categoryService = categoryService;
        this.transactionService = transactionService;
    }

    public boolean hasConversation(Long chatId) {
        return convs.containsKey(chatId);
    }

    public org.example.bot.command.CommandResult startTransaction(Long chatId, org.example.bot.model.TransactionType type) {
        Conv c = new Conv();
        c.stage = Stage.WAITING_AMOUNT;
        c.type = type;
        convs.put(chatId, c);
        return org.example.bot.command.CommandResult.text("💸 Введите сумму:");
    }

    public org.example.bot.command.CommandResult handleMessage(Long chatId, String text) {
        Conv c = convs.get(chatId);
        if (c == null) return org.example.bot.command.CommandResult.text("Ошибка состояния. Используйте /add чтобы начать.");

        if (c.stage == Stage.WAITING_AMOUNT) {
            try {
                BigDecimal amount = new BigDecimal(text.trim());
                if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                    convs.remove(chatId);
                    return org.example.bot.command.CommandResult.text("❌ Сумма должна быть больше 0. Начните заново с /add");
                }
                c.amount = amount;
                c.stage = Stage.WAITING_CATEGORY;
                var list = categoryService.listCategories(null, c.type);
                org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup markup = new org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup();
                java.util.List<java.util.List<org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton>> rows = new java.util.ArrayList<>();
                for (var cat : list) {
                    var button = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton();
                    button.setText(cat.getName());
                    String data = "TX:" + c.type.name() + ":" + cat.getId() + ":" + c.amount.toPlainString();
                    button.setCallbackData(data);
                    java.util.List<org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton> row = new java.util.ArrayList<>();
                    row.add(button);
                    rows.add(row);
                }
                markup.setKeyboard(rows);
                return org.example.bot.command.CommandResult.withInlineKeyboard("📁 Выберите категорию:", markup);
            } catch (NumberFormatException ex) {
                convs.remove(chatId);
                return org.example.bot.command.CommandResult.text("❌ Неверный формат суммы. Начните заново с /add");
            }
        }

        return org.example.bot.command.CommandResult.text("Ожидается выбор категории (нажмите кнопку)");
    }

    public org.example.bot.command.CommandResult handleCallback(Long chatId, String data) {
        // формат: TX:TYPE:categoryId:amount
        try {
            if (data == null || !data.startsWith("TX:")) return org.example.bot.command.CommandResult.text("Неправильные данные callback");
            String[] p = data.split(":");
            if (p.length < 4) return org.example.bot.command.CommandResult.text("Неполные данные callback");
            TransactionType type = TransactionType.valueOf(p[1]);
            Long catId = Long.parseLong(p[2]);
            BigDecimal amount = new BigDecimal(p[3]);

            var user = userService.registerOrGet(chatId, null, null);
            transactionService.addTransaction(user.getId(), amount, type, catId, null);
            convs.remove(chatId);
            String catName = categoryService.findById(catId).map(Category::getName).orElse("Неизвестная");
            String t = type == TransactionType.EXPENSE ? "Расход" : "Доход";
            return org.example.bot.command.CommandResult.text("✅ " + t + " записан: " + amount + " руб. (Категория: " + catName + ")");
        } catch (Exception ex) {
            convs.remove(chatId);
            return org.example.bot.command.CommandResult.text("Ошибка при обработке выбора: " + ex.getMessage());
        }
    }

    public void cancel(Long chatId) {
        convs.remove(chatId);
    }
}
