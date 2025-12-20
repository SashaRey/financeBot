-- SQLite DDL для финансового бота

PRAGMA foreign_keys = ON;

-- Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    telegram_id INTEGER NOT NULL UNIQUE,
    username TEXT,
    display_name TEXT,
    currency TEXT DEFAULT 'RUB',
    registration_date TEXT NOT NULL DEFAULT (datetime('now')),
    utc_offset INTEGER DEFAULT 0
);

-- Таблица категорий
CREATE TABLE IF NOT EXISTS categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    type TEXT NOT NULL, -- INCOME|EXPENSE|TRANSFER
    parent_id INTEGER,
    user_id INTEGER, -- NULL => глобальная категория
    CONSTRAINT fk_category_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Таблица транзакций
CREATE TABLE IF NOT EXISTS transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    amount NUMERIC NOT NULL,
    type TEXT NOT NULL,
    category_id INTEGER,
    description TEXT,
    timestamp TEXT NOT NULL DEFAULT (datetime('now')),
    CONSTRAINT fk_tx_user FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_tx_category FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL
);

-- Индексы для ускорения отчётов
CREATE INDEX IF NOT EXISTS idx_transactions_user_ts ON transactions(user_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_transactions_category ON transactions(category_id);
CREATE INDEX IF NOT EXISTS idx_categories_user ON categories(user_id);
