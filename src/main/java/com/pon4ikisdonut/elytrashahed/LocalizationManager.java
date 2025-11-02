package com.pon4ikisdonut.elytrashahed;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

final class LocalizationManager {

    private final ShahedPlugin plugin;
    private final Map<MessageKey, String> messages = new EnumMap<>(MessageKey.class);
    private String languageCode = "en";

    LocalizationManager(ShahedPlugin plugin) {
        this.plugin = plugin;
        load("en");
    }

    void load(String requestedLanguage) {
        String normalized = normalize(requestedLanguage);
        YamlConfiguration primary = loadLanguageYaml(normalized, true);
        if (primary == null) {
            plugin.getLogger().warning("Language file '" + normalized + "' not found, falling back to English.");
            normalized = "en";
            primary = loadLanguageYaml(normalized, true);
        }

        YamlConfiguration fallback = loadLanguageYaml("en", false);
        messages.clear();
        for (MessageKey key : MessageKey.values()) {
            String raw = getString(primary, fallback, key.path());
            if (raw == null || raw.isBlank()) {
                raw = englishFallback(key);
            }
            messages.put(key, colorize(raw));
        }
        languageCode = normalized;
    }

    String format(MessageKey key, Object... args) {
        String pattern = messages.getOrDefault(key, colorize(englishFallback(key)));
        if (args == null || args.length == 0) {
            return pattern;
        }
        try {
            return String.format(pattern, args);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Failed to format message for key " + key + ": " + ex.getMessage());
            return pattern;
        }
    }

    String getLanguageCode() {
        return languageCode;
    }

    private String normalize(String code) {
        if (code == null || code.isBlank()) {
            return "en";
        }
        return code.trim().toLowerCase(Locale.ROOT);
    }

    private YamlConfiguration loadLanguageYaml(String code, boolean checkDataFolder) {
        if (checkDataFolder) {
            File langDir = new File(plugin.getDataFolder(), "lang");
            File file = new File(langDir, code + ".yml");
            if (file.exists()) {
                return YamlConfiguration.loadConfiguration(file);
            }
        }
        try (InputStream stream = plugin.getResource("lang/" + code + ".yml")) {
            if (stream == null) {
                return null;
            }
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to read language resource '" + code + "': " + ex.getMessage());
            return null;
        }
    }

    private String getString(YamlConfiguration primary, YamlConfiguration fallback, String path) {
        if (primary != null && primary.contains(path)) {
            String value = primary.getString(path);
            if (value != null) {
                return value;
            }
        }
        if (fallback != null) {
            return fallback.getString(path);
        }
        return null;
    }

    private String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }

    private String englishFallback(MessageKey key) {
        return switch (key) {
            case ONLY_PLAYERS -> "&cOnly players can run this command.";
            case NO_PERMISSION -> "&cYou do not have permission for that.";
            case SHAHED_ACTIVATED -> "&cShahed mode armed! You're hot, stay sharp.";
            case SHAHED_DEACTIVATED -> "&aShahed mode disabled.";
            case SHAHED_ALREADY_DISABLED -> "&eShahed mode is already off.";
            case SHAHED_ACTIVATE_FAIL -> "&eCould not enable Shahed mode right now.";
            case SHAHED_POWER_SET -> "&6Shahed power set to x%d.";
            case SHAHED_USAGE -> "&cUsage: /%s [1-%d]";
            case AAGUN_RECEIVED -> "&6AA-Gun received.";
            case CONFIG_RELOADED -> "&aConfiguration reloaded (language: %s).";
            case REACTIVE_ONLY_GLIDING -> "&eReactive boost works only while gliding.";
            case REACTIVE_TRIGGERED -> "&6Reactive boost engaged!";
            case NEED_TNT -> "&cYou need %d TNT in your inventory for scale x%d.";
            case SHAHED_DEATH_MESSAGE -> "&7%s wanted to take a few lives with them... including their own.";
        };
    }
}

