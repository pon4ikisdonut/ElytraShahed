package com.pon4ikisdonut.elytrashahed;

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
            sender.sendMessage(plugin.message(MessageKey.ONLY_PLAYERS));
            return true;
        }
        if (!player.hasPermission("elytrashahed.shahed")) {
            player.sendMessage(plugin.message(MessageKey.NO_PERMISSION));
            return true;
        }

        if (args.length == 0) {
            if (plugin.isShahed(player)) {
                if (!plugin.deactivateShahed(player)) {
                    player.sendMessage(plugin.message(MessageKey.SHAHED_ALREADY_DISABLED));
                }
            } else if (!plugin.activateShahed(player, 1)) {
                player.sendMessage(plugin.message(MessageKey.SHAHED_ACTIVATE_FAIL));
            }
            return true;
        }

        try {
            int val = Integer.parseInt(args[0]);
            int max = Math.max(1, plugin.getConfig().getInt("max-shahed-power", 16));
            int clamped = Math.max(1, Math.min(max, val));
            boolean success;
            if (plugin.isShahed(player)) {
                success = plugin.updateShahedScale(player, clamped);
            } else {
                success = plugin.activateShahed(player, clamped);
            }
            if (success) {
                player.sendMessage(plugin.message(MessageKey.SHAHED_POWER_SET, clamped));
            }
        } catch (NumberFormatException e) {
            int max = Math.max(1, plugin.getConfig().getInt("max-shahed-power", 16));
            player.sendMessage(plugin.message(MessageKey.SHAHED_USAGE, label, max));
        }
        return true;
    }
}
