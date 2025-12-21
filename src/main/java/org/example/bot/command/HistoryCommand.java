package org.example.bot.command;

import org.example.bot.model.User;
import org.example.bot.model.Transaction;
import org.example.bot.service.TransactionService;
import org.example.bot.service.UserService;
import org.example.bot.service.CategoryService;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Команда /history — показывает последние операции пользователя.
 */
public class HistoryCommand implements Command {
    private final TransactionService transactionService;
    private final UserService userService;
    private final CategoryService categoryService;

    public HistoryCommand(TransactionService transactionService, UserService userService, CategoryService categoryService) {
        this.transactionService = transactionService;
        this.userService = userService;
        this.categoryService = categoryService;
    }

    @Override
    public CommandResult execute(Update update, String[] args) {
        Long chatId = update.getMessage().getChatId();
        User u = userService.registerOrGet(chatId, null, null);
        int limit = 10;
        if (args != null && args.length > 0) {
            try { limit = Integer.parseInt(args[0]); } catch (Exception ignore) {}
        }

        String hist = transactionService.getHistory(u.getId(), limit);

        // Build inline keyboard with delete buttons (shows "Удалить" -> opens confirmation)
        var last = transactionService.findLast(u.getId(), limit);
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Transaction t : last) {
            InlineKeyboardButton btn = new InlineKeyboardButton();
            btn.setText("Удалить /del_" + t.getId());
            btn.setCallbackData("DEL:" + t.getId());
            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(btn);
            rows.add(row);
        }
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        keyboard.setKeyboard(rows);

        return CommandResult.withInlineKeyboard(hist, keyboard);
    }
}
