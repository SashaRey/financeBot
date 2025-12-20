package org.example.bot.command;

import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Команда /help — краткая справка.
 */
public class HelpCommand implements Command {
    @Override
    public CommandResult execute(Update update, String[] args) {
        String text = "📋 Команды:\n" +
                "/add [сумма] [категория] [описание(optional)] - Добавить расход\n" +
                "/income [сумма] [категория] - Добавить доход\n" +
                "/balance - Показать баланс\n" +
                "/expenses - Последние расходы\n" +
                "/help - Справка";
        return CommandResult.text(text);
    }
}
