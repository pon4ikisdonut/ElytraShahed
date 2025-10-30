package com.pon4ikisdonut.elytrashahed;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class ShahedToggleCommand implements CommandExecutor {

    private final ShahedPlugin plugin;

    ShahedToggleCommand(ShahedPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Эту команду может выполнить только игрок.");
            return true;
        }
        if (!player.hasPermission("elytrashahed.shahed")) {
            player.sendMessage(ChatColor.RED + "У тебя нет права на это.");
            return true;
        }

        if (args.length == 0) {
            if (plugin.isShahed(player)) {
                if (!plugin.deactivateShahed(player)) {
                    player.sendMessage(ChatColor.YELLOW + "Режим «Шахед» уже выключен.");
                }
            } else if (!plugin.activateShahed(player)) {
                player.sendMessage(ChatColor.YELLOW + "Режим «Шахед» не удалось включить.");
            }
            return true;
        }

        try {
            int val = Integer.parseInt(args[0]);
            int max = Math.max(1, plugin.getConfig().getInt("max-shahed-power", 16));
            int clamped = Math.max(1, Math.min(max, val));
            if (!plugin.isShahed(player)) {
                plugin.activateShahed(player);
            }
            plugin.setShahedScale(player, clamped);
            player.sendMessage(ChatColor.GOLD + "Мощность «Шахеда» настроена на x" + clamped + ".");
            plugin.getLogger().info("[" + plugin.getName() + "] " + player.getName() + " set Shahed scale to x" + clamped);
        } catch (NumberFormatException e) {
            int max = Math.max(1, plugin.getConfig().getInt("max-shahed-power", 16));
            player.sendMessage(ChatColor.RED + "Использование: /" + label + " [1-" + max + "]");
        }
        return true;
    }
}
