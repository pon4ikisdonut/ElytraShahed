package com.pon4ikisdonut.elytrashahed;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class GhostFireworkCommand implements CommandExecutor {

    private final ShahedPlugin plugin;

    GhostFireworkCommand(ShahedPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Эту команду может выполнить только игрок.");
            return true;
        }
        if (!player.hasPermission("elytrashahed.ghostfirework")) {
            player.sendMessage(ChatColor.RED + "У тебя нет права на это.");
            return true;
        }

        int amount = 3;
        if (args.length > 0) {
            try {
                amount = Math.max(1, Math.min(64, Integer.parseInt(args[0])));
            } catch (NumberFormatException ex) {
                player.sendMessage(ChatColor.RED + "Использование: /" + label + " [1-64]");
                return true;
            }
        }

        plugin.giveGhostFireworks(player, amount);
        return true;
    }
}
