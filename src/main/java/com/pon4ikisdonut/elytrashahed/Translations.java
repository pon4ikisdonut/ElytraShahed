package com.pon4ikisdonut.elytrashahed;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.bukkit.ChatColor;

final class Translations {

    private final Map<MessageKey, String> messages;
    private final String languageCode;

    Translations(String languageCode) {
        String normalized = Objects.requireNonNullElse(languageCode, "ru").trim().toLowerCase(Locale.ROOT);
        this.languageCode = normalized;
        this.messages = switch (normalized) {
            case "en", "eng", "english" -> loadEnglish();
            default -> loadRussian();
        };
    }

    String getLanguageCode() {
        return languageCode;
    }

    String format(MessageKey key, Object... args) {
        String template = messages.getOrDefault(key, key.name());
        return args == null || args.length == 0 ? template : String.format(Locale.ROOT, template, args);
    }

    private Map<MessageKey, String> loadRussian() {
        Map<MessageKey, String> map = new EnumMap<>(MessageKey.class);
        map.put(MessageKey.ONLY_PLAYERS, ChatColor.RED + "Эту команду может выполнить только игрок.");
        map.put(MessageKey.NO_PERMISSION, ChatColor.RED + "У тебя нет права на это.");
        map.put(MessageKey.SHAHED_ACTIVATED, ChatColor.RED + "Режим «Шахед» активирован! Ты заряжен, будь осторожен.");
        map.put(MessageKey.SHAHED_DEACTIVATED, ChatColor.GREEN + "Режим «Шахед» отключён.");
        map.put(MessageKey.SHAHED_ALREADY_DISABLED, ChatColor.YELLOW + "Режим «Шахед» уже выключен.");
        map.put(MessageKey.SHAHED_ACTIVATE_FAIL, ChatColor.YELLOW + "Режим «Шахед» не удалось включить.");
        map.put(MessageKey.SHAHED_POWER_SET, ChatColor.GOLD + "Мощность «Шахеда» настроена на x%d.");
        map.put(MessageKey.SHAHED_USAGE, ChatColor.RED + "Использование: /%s [1-%d]");
        map.put(MessageKey.AAGUN_RECEIVED, ChatColor.GOLD + "Получен AA-Gun.");
        map.put(MessageKey.CONFIG_RELOADED, ChatColor.GREEN + "Конфиг перезагружен (язык: %s).");
        map.put(MessageKey.GHOST_USAGE, ChatColor.RED + "Использование: /%s [1-64]");
        map.put(MessageKey.GHOST_RECEIVED, ChatColor.GRAY + "Выданы громкие фейерверки x%d.");
        map.put(MessageKey.REACTIVE_ONLY_GLIDING, ChatColor.YELLOW + "Реактивный импульс доступен только в полёте.");
        map.put(MessageKey.REACTIVE_TRIGGERED, ChatColor.GOLD + "Реактивный импульс активирован!");
        map.put(MessageKey.NEED_TNT, ChatColor.RED + "Нужно %d TNT в инвентаре для мощности x%d.");
        map.put(MessageKey.SHAHED_DEATH_MESSAGE, "%s решил унести пару жизней... включая свою.");
        return map;
    }

    private Map<MessageKey, String> loadEnglish() {
        Map<MessageKey, String> map = new EnumMap<>(MessageKey.class);
        map.put(MessageKey.ONLY_PLAYERS, ChatColor.RED + "Only players can run this command.");
        map.put(MessageKey.NO_PERMISSION, ChatColor.RED + "You don't have permission for this.");
        map.put(MessageKey.SHAHED_ACTIVATED, ChatColor.RED + "Shahed mode armed! You're hot, stay sharp.");
        map.put(MessageKey.SHAHED_DEACTIVATED, ChatColor.GREEN + "Shahed mode disabled.");
        map.put(MessageKey.SHAHED_ALREADY_DISABLED, ChatColor.YELLOW + "Shahed mode is already off.");
        map.put(MessageKey.SHAHED_ACTIVATE_FAIL, ChatColor.YELLOW + "Could not enable Shahed mode right now.");
        map.put(MessageKey.SHAHED_POWER_SET, ChatColor.GOLD + "Shahed power set to x%d.");
        map.put(MessageKey.SHAHED_USAGE, ChatColor.RED + "Usage: /%s [1-%d]");
        map.put(MessageKey.AAGUN_RECEIVED, ChatColor.GOLD + "AA-Gun received.");
        map.put(MessageKey.CONFIG_RELOADED, ChatColor.GREEN + "Configuration reloaded (language: %s).");
        map.put(MessageKey.GHOST_USAGE, ChatColor.RED + "Usage: /%s [1-64]");
        map.put(MessageKey.GHOST_RECEIVED, ChatColor.GRAY + "Loud fireworks x%d delivered.");
        map.put(MessageKey.REACTIVE_ONLY_GLIDING, ChatColor.YELLOW + "Reactive boost only works while gliding.");
        map.put(MessageKey.REACTIVE_TRIGGERED, ChatColor.GOLD + "Reactive boost engaged!");
        map.put(MessageKey.NEED_TNT, ChatColor.RED + "You need %d TNT in your inventory for power x%d.");
        map.put(MessageKey.SHAHED_DEATH_MESSAGE, "%s decided to take a few lives... including their own.");
        return map;
    }
}
