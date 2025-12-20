package org.example.bot.command;

import org.example.bot.model.User;
import org.example.bot.service.UserService;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Команда /start — регистрирует пользователя и возвращает приветствие.
 */
public class StartCommand implements Command {
    private final UserService userService;
    private final org.example.bot.service.CategoryService categoryService;

    public StartCommand(UserService userService, org.example.bot.service.CategoryService categoryService) {
        this.userService = userService;
        this.categoryService = categoryService;
    }

    @Override
    public CommandResult execute(Update update, String[] args) {
        Long chatId = update.getMessage().getChatId();
        String username = update.getMessage().getFrom() != null ? update.getMessage().getFrom().getUserName() : null;
        String name = update.getMessage().getFrom() != null ? (update.getMessage().getFrom().getFirstName() + " " + (update.getMessage().getFrom().getLastName() == null ? "" : update.getMessage().getFrom().getLastName())).trim() : null;
        User u = userService.registerOrGet(chatId, username, name);

        // Инициализируем дефолтные глобальные категории при первом /start
        try { categoryService.initDefaultsIfNeeded(); } catch (Exception ignore) {}

        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("➕ Расход"));
        row1.add(new KeyboardButton("💰 Доход"));
        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("💎 Баланс"));
        rows.add(row1);
        rows.add(row2);
        keyboard.setKeyboard(rows);

        String text = "Привет, " + (u.getDisplayName() != null ? u.getDisplayName() : "пользователь") + "!\nЯ — ваш финансовый трекер.";
        return CommandResult.withReplyKeyboard(text, keyboard);
    }
}
