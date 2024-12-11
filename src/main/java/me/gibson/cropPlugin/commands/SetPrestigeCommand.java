package me.gibson.cropPlugin.commands;

import me.gibson.cropPlugin.managers.PrestigeManager;
import me.gibson.cropPlugin.managers.SkriptManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SetPrestigeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("cropPlugin.setprestige")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /setprestige <player> <level>");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(args[0]);
        if (targetPlayer == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Invalid level. Please enter a number.");
            return true;
        }

        PrestigeManager.setPlayerPrestige(targetPlayer, level);
        sender.sendMessage(ChatColor.GREEN + "Set " + targetPlayer.getName() + "'s prestige level to " + level + ".");
        targetPlayer.sendMessage(ChatColor.GREEN + "Your prestige level has been set to " + level + " by an admin.");

        return true;
    }
}