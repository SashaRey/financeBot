package org.example.bot.command;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

/**
 * Результат выполнения команды: текст + опциональные клавиатуры.
 */
public class CommandResult {
    private final String text;
    private final ReplyKeyboardMarkup replyKeyboard;
    private final InlineKeyboardMarkup inlineKeyboard;

    public CommandResult(String text, ReplyKeyboardMarkup replyKeyboard, InlineKeyboardMarkup inlineKeyboard) {
        this.text = text;
        this.replyKeyboard = replyKeyboard;
        this.inlineKeyboard = inlineKeyboard;
    }

    public static CommandResult text(String text) {
        return new CommandResult(text, null, null);
    }

    public static CommandResult withReplyKeyboard(String text, ReplyKeyboardMarkup keyboard) {
        return new CommandResult(text, keyboard, null);
    }

    public static CommandResult withInlineKeyboard(String text, InlineKeyboardMarkup keyboard) {
        return new CommandResult(text, null, keyboard);
    }

    public String getText() {
        return text;
    }

    public ReplyKeyboardMarkup getReplyKeyboard() {
        return replyKeyboard;
    }

    public InlineKeyboardMarkup getInlineKeyboard() {
        return inlineKeyboard;
    }
}
