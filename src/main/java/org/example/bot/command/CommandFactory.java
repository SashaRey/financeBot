package org.example.bot.command;

import org.example.bot.service.CategoryService;
import org.example.bot.service.TransactionService;
import org.example.bot.service.UserService;

/**
 * Утилита для сборки реестра команд с переданными сервисами.
 */
public class CommandFactory {
    public static CommandRegistry createRegistry(UserService userService, CategoryService categoryService, TransactionService transactionService, org.example.bot.conversation.ConversationManager conversationManager) {
        CommandRegistry registry = new CommandRegistry();
        registry.register("/start", new StartCommand(userService, categoryService));
        registry.register("/help", new HelpCommand());
        registry.register("/add", new AddCommand(userService, categoryService, transactionService, conversationManager));
        registry.register("/income", new org.example.bot.command.IncomeCommand(userService, categoryService, transactionService, conversationManager));
        registry.register("/balance", new org.example.bot.command.BalanceCommand(transactionService, userService));
        return registry;
    }
}
