package com.pon4ikisdonut.elytrashahed;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class AAGunCommand implements CommandExecutor {

    private final ShahedPlugin plugin;

    AAGunCommand(ShahedPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Эту команду может выполнить только игрок.");
            return true;
        }
        if (!player.hasPermission("elytrashahed.aagun")) {
            player.sendMessage(ChatColor.RED + "У тебя нет права на это.");
            return true;
        }

        plugin.giveAAGun(player);
        return true;
    }
}
