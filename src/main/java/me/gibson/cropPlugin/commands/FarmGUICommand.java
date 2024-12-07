package me.gibson.cropPlugin.commands;

import me.gibson.cropPlugin.listeners.FarmTycoonListener;
import me.gibson.cropPlugin.FarmTycoonPlugin;
import me.gibson.cropPlugin.utils.CropType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FarmGUICommand implements CommandExecutor {

    private final FarmTycoonPlugin plugin;

    public FarmGUICommand(FarmTycoonPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("farmtycoon.reload")) {
                sender.sendMessage("§cYou don't have permission to perform this command!");
                return true;
            }

            plugin.reloadConfig();
            CropType.loadCropTypes(plugin.getConfig());
            sender.sendMessage("§aConfiguration reloaded successfully!");
            return true;
        }

        try {
            FarmTycoonListener listener = new FarmTycoonListener(plugin);
            listener.openCropSelectionGUI(player);
        } catch (Exception e) {
            player.sendMessage("§cAn error occurred while opening the GUI. Please contact an administrator.");
            e.printStackTrace();
        }

        return true;
    }
}
