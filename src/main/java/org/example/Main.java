package org.example;

import org.example.bot.ExpenseDao;
import org.example.bot.TelegramBot;
import org.example.bot.adapter.ExpenseDaoAdapter;
import org.example.bot.dao.InMemoryCategoryRepository;
import org.example.bot.dao.InMemoryTransactionRepository;
import org.example.bot.dao.sqlite.JdbcCategoryRepository;
import org.example.bot.dao.sqlite.JdbcTransactionRepository;
import org.example.bot.dao.DatabaseManager;
import org.example.bot.repository.CategoryRepository;
import org.example.bot.repository.TransactionRepository;
import org.example.bot.repository.UserRepository;
import org.example.bot.service.CategoryService;
import org.example.bot.service.TransactionService;
import org.example.bot.service.UserService;
import org.example.bot.service.impl.CategoryServiceImpl;
import org.example.bot.service.impl.TransactionServiceImpl;
import org.example.bot.service.impl.UserServiceImpl;
import org.example.bot.command.CommandFactory;
import org.example.bot.command.CommandRegistry;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main {
    public static void main(String[] args) {
        String botToken = "7596704485:AAENl2PrL6D7Qxp4ilcQh9KLAR0VrDSXnsg";
        String botUsername = "finance_matmech_bot";
//        Properties props = new Properties();
//        try (InputStream input = Main.class.getClassLoader().getResourceAsStream("bot.properties")) {
//            if (input == null) {
//                System.err.println("❌ Файл bot.properties не найден!");
//                return;
//            }
//            props.load(input);
//        } catch (IOException ex) {
//            System.err.println("❌ Ошибка чтения bot.properties: " + ex.getMessage());
//            ex.printStackTrace();
//            return;
//        }
//
//        String botToken = props.getProperty("BOT_TOKEN");
//        String botUsername = props.getProperty("BOT_USERNAME");
        // Репозитории: используем JDBC-реализацию UserRepository, пока категории/транзакции оставляем in-memory
        // Используем абсолютный путь к файлу базы, чтобы избежать неоднозначности с рабочей директорией
        String dbFile = new java.io.File("finance_bot.db").getAbsolutePath();
        DatabaseManager dbManager = new DatabaseManager(dbFile);
        UserRepository userRepo = new org.example.bot.dao.sqlite.JdbcUserRepository(dbManager);
        CategoryRepository categoryRepo = new JdbcCategoryRepository(dbManager);
        TransactionRepository transactionRepo = new JdbcTransactionRepository(dbManager);

        // Сервисы
        UserService userService = new UserServiceImpl(userRepo);
        CategoryService categoryService = new CategoryServiceImpl(categoryRepo);
        TransactionService transactionService = new TransactionServiceImpl(transactionRepo);

        // Адаптер для совместимости с существующим TelegramBot
        ExpenseDao expenseDao = new ExpenseDaoAdapter(userService, categoryService, transactionService);

        // Инициализация дефолтных категорий будет выполняться при первом /start пользователем

        // Закрытие БД при остановке JVM
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                dbManager.close();
            } catch (Exception e) {
                // ignore
            }
        }));

        // ConversationManager для интерактивных команд
        org.example.bot.conversation.ConversationManager conversationManager = new org.example.bot.conversation.ConversationManager(userService, categoryService, transactionService);

        // Реестр команд (использует сервисы)
        CommandRegistry registry = CommandFactory.createRegistry(userService, categoryService, transactionService, conversationManager);

        // Диагностика: покажем абсолютный путь к файлу и количество пользователей в таблице
        try {
            System.out.println("[Main] DB file absolute path: " + dbManager.getDbFilePath());
            try (java.sql.Statement st = dbManager.getConnection().createStatement(); java.sql.ResultSet rs = st.executeQuery("SELECT COUNT(*) as c FROM users")) {
                if (rs.next()) System.out.println("[Main] users count = " + rs.getInt("c"));
            }
        } catch (Exception ex) {
            System.err.println("[Main] Diagnostics error: " + ex.getMessage());
        }

        if (botToken == null || botToken.isEmpty()) {
            System.err.println("❌ BOT_TOKEN не задан!");
            return;
        }
        if (botUsername == null || botUsername.isEmpty()) {
            System.err.println("❌ BOT_USERNAME не задан!");
            return;
        }

        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            botsApi.registerBot(new TelegramBot(botToken, botUsername, expenseDao, registry, conversationManager));
            System.out.println("✅ Финансовый бот запущен!");
            System.out.println("Бот: " + botUsername);
        } catch (TelegramApiException e) {
            e.printStackTrace();
            System.err.println("❌ Ошибка при запуске бота: " + e.getMessage());
        }
    }
}
