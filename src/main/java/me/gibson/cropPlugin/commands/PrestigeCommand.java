package me.gibson.cropPlugin.commands;

import me.gibson.cropPlugin.GUI.PrestigeGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PrestigeCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;

        // Open the Prestige GUI
        PrestigeGUI.openPrestigeGUI(player);

        return true;
    }
}
