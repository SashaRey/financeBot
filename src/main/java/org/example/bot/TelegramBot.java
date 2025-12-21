package org.example.bot;

import org.example.bot.command.Command;
import org.example.bot.command.CommandRegistry;
import org.example.bot.command.CommandResult;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;



import java.util.*;

public class TelegramBot extends TelegramLongPollingBot {

    private Map<Long, String> userStates = new HashMap<>();
    private Map<Long, Double> temporaryAmounts = new HashMap<>();
    private String botToken = "7596704485:AAENl2PrL6D7Qxp4ilcQh9KLAR0VrDSXnsg";
    private String botUsername = "finance_matmech_bot";
    private final ExpenseDao expenseDao;
    private final CommandRegistry commandRegistry;
    private final org.example.bot.conversation.ConversationManager conversationManager;
    private final org.example.bot.service.TransactionService transactionService;
    private final org.example.bot.service.UserService userService;
    private final org.example.bot.util.HistoryMessageStore historyStore;


    public TelegramBot(String botToken, String botUsername, ExpenseDao expenseDao, CommandRegistry commandRegistry, org.example.bot.conversation.ConversationManager conversationManager, org.example.bot.service.TransactionService transactionService, org.example.bot.service.UserService userService, org.example.bot.util.HistoryMessageStore historyStore) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.expenseDao = expenseDao;
        this.commandRegistry = commandRegistry;
        this.conversationManager = conversationManager;
        this.transactionService = transactionService;
        this.userService = userService;
        this.historyStore = historyStore;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            var cq = update.getCallbackQuery();
            Long chatId = cq.getMessage().getChatId();
            String data = cq.getData();

            // Handle delete callbacks first
            if (data != null && data.startsWith("DEL:")) {
                String idStr = data.substring("DEL:".length());
                try {
                    Long txId = Long.parseLong(idStr);
                    var user = userService.registerOrGet(chatId, null, null);
                    boolean deleted = transactionService.deleteTransaction(user.getId(), txId);
                    AnswerCallbackQuery acq = new AnswerCallbackQuery();
                    acq.setCallbackQueryId(cq.getId());
                    acq.setText(deleted ? "✅ Транзакция удалена" : "❌ Транзакция не найдена");
                    try { execute(acq); } catch (TelegramApiException ignore) {}

                    // Rebuild history message and keyboard and edit original message
                    int limit = 10;
                    String newHist = transactionService.getHistory(user.getId(), limit);
                    var last = transactionService.findLast(user.getId(), limit);
                    List<List<org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton>> rows = new ArrayList<>();
                    for (var t : last) {
                        org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton btn = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton();
                        btn.setText("Удалить /del_" + t.getId());
                        btn.setCallbackData("DEL:" + t.getId());
                        List<org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton> row = new ArrayList<>();
                        row.add(btn);
                        rows.add(row);
                    }
                    org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard = new org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup();
                    keyboard.setKeyboard(rows);

                    try {
                        org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText edit = new org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText();
                        edit.setChatId(cq.getMessage().getChatId().toString());
                        edit.setMessageId(cq.getMessage().getMessageId());
                        edit.setText(newHist);
                        edit.setReplyMarkup(keyboard);
                        execute(edit);
                    } catch (TelegramApiException ignore) {}

                    return;
                } catch (Exception ex) {
                    AnswerCallbackQuery acq = new AnswerCallbackQuery();
                    acq.setCallbackQueryId(cq.getId());
                    acq.setText("❌ Ошибка удаления");
                    try { execute(acq); } catch (TelegramApiException ignore) {}
                    return;
                }
            }

            var result = conversationManager.handleCallback(chatId, data);

            String resText = result != null && result.getText() != null ? result.getText() : "";
            String catName = null;
            int idx = resText.indexOf("(Категория:");
            if (idx >= 0) {
                int start = idx + "(Категория:".length();
                int end = resText.indexOf(')', start);
                if (end > start) catName = resText.substring(start, end).trim();
            }

            String editText = catName != null ? ("✅ Категория выбрана: " + catName) : "✅ Категория выбрана";
            try {
                org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText editTextMsg = new org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText();
                editTextMsg.setChatId(cq.getMessage().getChatId().toString());
                editTextMsg.setMessageId(cq.getMessage().getMessageId());
                editTextMsg.setText(editText);
                execute(editTextMsg);
            } catch (TelegramApiException ignore) {}

            AnswerCallbackQuery acq = new AnswerCallbackQuery();
            acq.setCallbackQueryId(cq.getId());
            try { execute(acq); } catch (TelegramApiException ignore) {}

            ReplyKeyboardMarkup menu = new ReplyKeyboardMarkup();
            menu.setResizeKeyboard(true);
            menu.setOneTimeKeyboard(false);
            java.util.List<KeyboardRow> rows = new java.util.ArrayList<>();
            KeyboardRow row1 = new KeyboardRow();
            row1.add(new KeyboardButton("➕ Расход"));
            row1.add(new KeyboardButton("💰 Доход"));
            KeyboardRow row2 = new KeyboardRow();
            row2.add(new KeyboardButton("💎 Баланс"));
            row2.add(new KeyboardButton("📜 История"));
            rows.add(row1);
            rows.add(row2);
            menu.setKeyboard(rows);

            sendMessage(chatId, result.getText(), menu, null);
            return;
        }

        if (!update.hasMessage()) return;
        Message message = update.getMessage();
        if (!message.hasText()) return;
        Long chatId = message.getChatId();
        String text = message.getText().trim();

        boolean isMenuAction = text.startsWith("/")
                || text.equals("➕ Расход") || text.equalsIgnoreCase("Расход")
                || text.equals("💰 Доход") || text.equalsIgnoreCase("Доход")
                || text.equals("💎 Баланс") || text.equalsIgnoreCase("Баланс")
                || text.equals("📜 История") || text.equalsIgnoreCase("История");

            if (isMenuAction) {
            CommandResult cmdResult = processCommand(chatId, text, update);
            org.telegram.telegrambots.meta.api.objects.Message sent = sendMessage(chatId, cmdResult.getText(), cmdResult.getReplyKeyboard(), cmdResult.getInlineKeyboard());
            if (cmdResult.getInlineKeyboard() != null && sent != null) {
                historyStore.put(chatId, sent.getMessageId());
            }
        } else if (conversationManager != null && conversationManager.hasConversation(chatId)) {
            CommandResult result = conversationManager.handleMessage(chatId, text);
            org.telegram.telegrambots.meta.api.objects.Message sent = sendMessage(chatId, result.getText(), null, result.getInlineKeyboard());
            if (result.getInlineKeyboard() != null && sent != null) historyStore.put(chatId, sent.getMessageId());
        } else {
            CommandResult cmdResult = processCommand(chatId, text, update);
            org.telegram.telegrambots.meta.api.objects.Message sent = sendMessage(chatId, cmdResult.getText(), cmdResult.getReplyKeyboard(), cmdResult.getInlineKeyboard());
            if (cmdResult.getInlineKeyboard() != null && sent != null) historyStore.put(chatId, sent.getMessageId());
        }
    }

    private CommandResult processCommand(Long chatId, String text, Update update) {
        String state = userStates.get(chatId);

        if (state != null) {
            return CommandResult.text(handleState(chatId, text, state));
        }

        if (text != null && text.startsWith("/")) {
            if (text.startsWith("/del_")) {
                // Text-based deletion: /del_<id>
                try {
                    String[] parts = text.trim().split("_");
                    Long txId = Long.parseLong(parts[1]);
                    var user = userService.registerOrGet(chatId, null, null);
                    boolean ok = transactionService.deleteTransaction(user.getId(), txId);
                    // Try to update stored history message if present
                    Integer histMsgId = historyStore.get(chatId);
                    if (histMsgId != null) {
                        int limit = 10;
                        String newHist = transactionService.getHistory(user.getId(), limit);
                        var last = transactionService.findLast(user.getId(), limit);
                        List<List<org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton>> rows = new ArrayList<>();
                        for (var t : last) {
                            org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton btn = new org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton();
                            btn.setText("Удалить /del_" + t.getId());
                            btn.setCallbackData("DEL:" + t.getId());
                            List<org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton> row = new ArrayList<>();
                            row.add(btn);
                            rows.add(row);
                        }
                        org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard = new org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup();
                        keyboard.setKeyboard(rows);
                        try {
                            org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText edit = new org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText();
                            edit.setChatId(chatId.toString());
                            edit.setMessageId(histMsgId);
                            edit.setText(newHist);
                            edit.setReplyMarkup(keyboard);
                            execute(edit);
                        } catch (TelegramApiException ignore) {
                        }
                    }
                    return CommandResult.text(ok ? "✅ Транзакция удалена" : "❌ Транзакция не найдена");
                } catch (Exception ex) {
                    return CommandResult.text("❌ Неверный формат команды. Используйте /del_<id>");
                }
            }
            String[] parts = text.trim().split("\\s+");
            String cmdName = parts[0].toLowerCase();
            java.util.Optional<Command> cmd = commandRegistry != null ? commandRegistry.get(cmdName) : java.util.Optional.empty();
            if (cmd.isPresent()) {
                String[] args = parts.length > 1 ? java.util.Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
                try {
                    return cmd.get().execute(update, args);
                } catch (Exception e) {
                    e.printStackTrace();
                    return CommandResult.text("❌ Ошибка при выполнении команды: " + e.getMessage());
                }
            }
        }

        String key = text.toLowerCase();
        if (text.equals("➕ Расход") || text.equalsIgnoreCase("Расход")) key = "/add";
        if (text.equals("💰 Доход") || text.equalsIgnoreCase("Доход")) key = "/income";
        if (text.equals("💎 Баланс") || text.equalsIgnoreCase("Баланс")) key = "/balance";
        if (text.equals("📜 История") || text.equalsIgnoreCase("История")) key = "/history";
        if (text.equals("❓ Помощь") || text.equalsIgnoreCase("Помощь")) key = "/help";

        if ("/add".equals(key)) {
            return conversationManager.startTransaction(chatId, org.example.bot.model.TransactionType.EXPENSE);
        }
        if ("/income".equals(key)) {
            return conversationManager.startTransaction(chatId, org.example.bot.model.TransactionType.INCOME);
        }

        // Если ключ начинается с '/', пробуем выполнить команду из реестра
        if (key.startsWith("/")) {
            java.util.Optional<Command> cmd = commandRegistry != null ? commandRegistry.get(key) : java.util.Optional.empty();
            if (cmd.isPresent()) {
                try {
                    return cmd.get().execute(update, new String[0]);
                } catch (Exception e) {
                    e.printStackTrace();
                    return CommandResult.text("❌ Ошибка при выполнении команды: " + e.getMessage());
                }
            }
        }

        return switch (key) {
            case "/start" -> CommandResult.text(handleStart(chatId));
            case "/balance" -> CommandResult.text(handleBalance(chatId));
            case "/expenses" -> CommandResult.text(handleExpenses(chatId));
            case "/help" -> CommandResult.text(handleHelp(chatId));
            default -> CommandResult.text(handleUnknown(chatId, text));
        };
    }

    private String handleStart(Long chatId) {
        return "💰 Финансовый бот\n\n" +
                "Команды:\n" +
                "/add - Добавить расход\n" +
                "/balance - Баланс\n" +
                "/expenses - Последние расходы\n" +
                "/help - Помощь";
    }

    private String handleAdd(Long chatId) {
        userStates.put(chatId, "WAITING_AMOUNT");
        return "💸 Введите сумму расхода:";
    }

    private String handleBalance(Long chatId) {
        return "💰 Баланс: " + expenseDao.getBalance(chatId) + " руб.";
    }

    private String handleExpenses(Long chatId) {
        return getLastExpenses(chatId);
    }

    private String handleHelp(Long chatId) {
        return "📋 Команды:\n" +
                "/add - Добавить расход\n" +
                "/balance - Баланс\n" +
                "/expenses - Последние расходы\n" +
                "/help - Помощь";
    }

    private String handleUnknown(Long chatId, String text) {
        return "Используйте /help для списка команд";
    }

    private String handleState(Long chatId, String text, String state) {
        return switch (state) {
            case "WAITING_AMOUNT" -> handleWaitingAmount(chatId, text);
            case "WAITING_CATEGORY" -> handleWaitingCategory(chatId, text);
            default -> {
                userStates.remove(chatId);
                temporaryAmounts.remove(chatId);
                yield "Ошибка состояния. Используйте /help для списка команд";
            }
        };
    }

    private String handleWaitingAmount(Long chatId, String text) {
        try {
            double amount = Double.parseDouble(text);
            if (amount <= 0) {
                return "❌ Сумма должна быть больше 0!";
            }
            temporaryAmounts.put(chatId, amount); // сохраняем сумму
            userStates.put(chatId, "WAITING_CATEGORY");
            return "📁 Выберите категорию:\n" +
                    "1 - Еда\n" +
                    "2 - Транспорт\n" +
                    "3 - Развлечения\n" +
                    "4 - Коммунальные\n" +
                    "5 - Другое";
        } catch (NumberFormatException e) {
            userStates.remove(chatId);
            return "❌ Ошибка! Введите корректную сумму (например: 1500 или 1500.50):";
        }
    }

    private String handleWaitingCategory(Long chatId, String text) {
        String category = getCategoryByNumber(text);
        Double amount = temporaryAmounts.get(chatId);

        if (amount == null) {
            userStates.remove(chatId);
            return "❌ Ошибка данных. Начните заново с /add";
        }

        Expense expense = new Expense(amount, category);
//        userExpenses.get(chatId).add(expense);
//
//        userBalances.compute(chatId, (k, currentBalance) -> currentBalance - amount);
        expenseDao.addExpense(chatId, amount, category);
        userStates.remove(chatId);
        temporaryAmounts.remove(chatId);

        return "✅ Добавлен расход:\n" +
                "💸 Сумма: " + amount + " руб.\n" +
                "📁 Категория: " + category + "\n" +
                "💰 Новый баланс: " + expenseDao.getBalance(chatId) + " руб.";
    }

    private String getCategoryByNumber(String number) {
        switch (number) {
            case "1": return "Еда";
            case "2": return "Транспорт";
            case "3": return "Развлечения";
            case "4": return "Коммунальные";
            default: return "Другое";
        }
    }

    private String getLastExpenses(Long chatId) {
        List<Expense> expenses = expenseDao.getExpenses(chatId);
        if (expenses.isEmpty()) {
            return "📊 Расходы отсутствуют";
        }

        StringBuilder sb = new StringBuilder("📊 Последние расходы:\n");
        int count = Math.min(expenses.size(), 5);

        // Берем последние 5 расходов
        for (int i = expenses.size() - 1; i >= Math.max(0, expenses.size() - count); i--) {
            Expense exp = expenses.get(i);
            sb.append("• ").append(exp.amount).append(" руб. - ").append(exp.category).append("\n");
        }

        sb.append("\n💰 Общий баланс: ").append(expenseDao.getBalance(chatId)).append(" руб.");
        return sb.toString();
    }

    private org.telegram.telegrambots.meta.api.objects.Message sendMessage(Long chatId, String text) {
        return sendMessage(chatId, text, null, null);
    }

    private org.telegram.telegrambots.meta.api.objects.Message sendMessage(Long chatId, String text, ReplyKeyboardMarkup replyKeyboard, InlineKeyboardMarkup inlineKeyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        if (replyKeyboard != null) message.setReplyMarkup(replyKeyboard);
        if (inlineKeyboard != null) message.setReplyMarkup(inlineKeyboard);

        try {
            return execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }
}
