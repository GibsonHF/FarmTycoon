package me.gibson.cropPlugin.commands;

import me.gibson.cropPlugin.GUI.ConfirmPayGUI;
import me.gibson.cropPlugin.managers.SkriptManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GemsCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 && sender instanceof Player) {
            Player player = (Player) sender;
            int balance = SkriptManager.getGems(player);
            player.sendMessage("§aYou have §6" + balance + " §agems.");
            return true;
        }

        if (args.length < 3) {
            if (sender instanceof Player && args.length >= 1 && args[0].equalsIgnoreCase("pay")) {
                sender.sendMessage("§cUsage: /gems pay <player> <amount>");
            } else {
                sender.sendMessage("§cUsage: /gems <add|set|pay> <player> <amount>");
            }
            return true;
        }

        String subCommand = args[0];
        Player targetPlayer = Bukkit.getPlayer(args[1]);
        int amount;

        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cAmount must be a valid number.");
            return true;
        }

        if (amount <= 0) {
            sender.sendMessage("§cAmount must be greater than zero.");
            return true;
        }

        if (subCommand.equalsIgnoreCase("pay")) {
            // Handle payment between players
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cOnly players can use the pay command.");
                return true;
            }

            Player payer = (Player) sender;

            if (targetPlayer == null) {
                sender.sendMessage("§cPlayer not found.");
                return true;
            }

            if (payer.equals(targetPlayer)) {
                sender.sendMessage("§cYou cannot pay yourself.");
                return true;
            }

            int payerGems = SkriptManager.getGems(payer);
            if (payerGems < amount) {
                payer.sendMessage("§cYou do not have enough gems to pay " + targetPlayer.getName() + ".");
                return true;
            }

            ConfirmPayGUI.openConfirmPayGUI(payer, targetPlayer, amount);


//            // Perform the transaction
//            SkriptManager.addGems(targetPlayer, amount);
//            SkriptManager.setGems(payer, payerGems - amount);
//
//            payer.sendMessage("§aYou have successfully paid §6" + amount + " §agems to " + targetPlayer.getName() + ".");
//            targetPlayer.sendMessage("§aYou have received §6" + amount + " §agems from " + payer.getName() + ".");
            return true;
        }

        // Restrict "add" and "set" commands to only ops or console
        if (!sender.isOp() && !(sender instanceof org.bukkit.command.ConsoleCommandSender)) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (targetPlayer == null) {
            sender.sendMessage("§cPlayer not found.");
            return true;
        }

        switch (subCommand.toLowerCase()) {
            case "add":
                SkriptManager.addGems(targetPlayer, amount);
                sender.sendMessage("§aAdded " + amount + " gems to " + targetPlayer.getName() + ".");
                break;
            case "set":
                SkriptManager.setGems(targetPlayer, amount);
                sender.sendMessage("§aSet " + targetPlayer.getName() + "'s gems to " + amount + ".");
                break;
            default:
                sender.sendMessage("§cUsage: /gems <add|set|pay> <player> <amount>");
                break;
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if (sender.isOp() || sender instanceof org.bukkit.command.ConsoleCommandSender) {
                if ("add".startsWith(args[0].toLowerCase())) {
                    completions.add("add");
                }
                if ("set".startsWith(args[0].toLowerCase())) {
                    completions.add("set");
                }
            }
            if ("pay".startsWith(args[0].toLowerCase())) {
                completions.add("pay");
            }
        } else if (args.length == 2) {
            String partialName = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(partialName)) {
                    completions.add(player.getName());
                }
            }
        }
        return completions;
    }
}
