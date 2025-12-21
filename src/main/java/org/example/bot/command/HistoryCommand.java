package org.example.bot.command;

import org.example.bot.model.User;
import org.example.bot.service.TransactionService;
import org.example.bot.service.UserService;
import org.telegram.telegrambots.meta.api.objects.Update;

/**
 * Команда /history — показывает последние операции пользователя.
 */
public class HistoryCommand implements Command {
    private final TransactionService transactionService;
    private final UserService userService;

    public HistoryCommand(TransactionService transactionService, UserService userService) {
        this.transactionService = transactionService;
        this.userService = userService;
    }

    @Override
    public CommandResult execute(Update update, String[] args) {
        Long chatId = update.getMessage().getChatId();
        User u = userService.registerOrGet(chatId, null, null);
        String hist = transactionService.getHistory(u.getId(), 5);
        return CommandResult.text(hist);
    }
}
