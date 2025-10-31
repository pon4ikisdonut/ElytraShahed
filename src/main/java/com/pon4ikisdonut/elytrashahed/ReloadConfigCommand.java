package com.pon4ikisdonut.elytrashahed;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

class ReloadConfigCommand implements CommandExecutor {

    private final ShahedPlugin plugin;

    ReloadConfigCommand(ShahedPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("elytrashahed.reload")) {
            sender.sendMessage(plugin.message(MessageKey.NO_PERMISSION));
            return true;
        }

        plugin.reloadPluginSettings();
        sender.sendMessage(plugin.message(MessageKey.CONFIG_RELOADED, plugin.getActiveLanguage()));
        return true;
    }
}
