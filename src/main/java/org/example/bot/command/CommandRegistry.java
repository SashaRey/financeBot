package org.example.bot.command;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;

/**
 * Простой реестр команд: хранит отображение строки команды -> Command.
 */
public class CommandRegistry {
    private final Map<String, Command> commands = new ConcurrentHashMap<>();

    public void register(String name, Command command) {
        commands.put(name.toLowerCase(), command);
    }

    public Optional<Command> get(String name) {
        return Optional.ofNullable(commands.get(name.toLowerCase()));
    }
}
