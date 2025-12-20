package org.example.bot.command;

import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Интерфейс команды бота. Возвращает `CommandResult` с текстом и опциональными клавиатурами.
 */
public interface Command {
    CommandResult execute(Update update, String[] args);
}
