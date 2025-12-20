package org.example.bot.command;

import org.example.bot.service.TransactionService;
import org.example.bot.service.UserService;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Instant;

public class BalanceCommand implements Command {
    private final TransactionService transactionService;
    private final UserService userService;

    public BalanceCommand(TransactionService transactionService, UserService userService) {
        this.transactionService = transactionService;
        this.userService = userService;
    }

    @Override
    public CommandResult execute(Update update, String[] args) {
        Long chatId = update.getMessage().getChatId();
        var userOpt = userService.findByTelegramId(chatId);
        if (userOpt.isEmpty()) {
            return CommandResult.text("Пользователь не зарегистрирован. Отправьте /start");
        }
        var user = userOpt.get();
        var bal = transactionService.getBalance(user.getId(), Instant.EPOCH, Instant.now());
        return CommandResult.text("💰 Текущий баланс: " + bal);
    }
}
