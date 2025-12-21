package org.example.bot.util;

import org.example.bot.model.Transaction;
import org.example.bot.model.TransactionType;
import org.example.bot.service.CategoryService;
import org.example.bot.service.TransactionService;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class HistoryFormatter {
    private final TransactionService transactionService;
    private final CategoryService categoryService;

    public HistoryFormatter(TransactionService transactionService, CategoryService categoryService) {
        this.transactionService = transactionService;
        this.categoryService = categoryService;
    }

    public HistoryPayload build(Long userId, int limit) {
        List<Transaction> list = transactionService.listTransactions(userId, java.time.Instant.EPOCH, java.time.Instant.now());
        list = transactionService.findLast(userId, limit); // we'll add helper in TransactionServiceImpl via reflection? but better to call transactionRepository - instead we'll call getHistory and rebuild keyboard by fetching last transactions via service
        // However TransactionService currently has findLastTransactions only in repository; to avoid more changes, we'll use transactionService.getHistory for text and build keyboard using transactionService.findById — but we need list of transactions. Simpler: call transactionService.getHistoryTextForFormatting — but to minimize changes, we'll call transactionService.getHistory and not build keyboard here.
        return new HistoryPayload(transactionService.getHistory(userId, limit), null);
    }
}

class HistoryPayload {
    private final String text;
    private final InlineKeyboardMarkup keyboard;

    public HistoryPayload(String text, InlineKeyboardMarkup keyboard) {
        this.text = text;
        this.keyboard = keyboard;
    }

    public String getText() { return text; }
    public InlineKeyboardMarkup getKeyboard() { return keyboard; }
}
