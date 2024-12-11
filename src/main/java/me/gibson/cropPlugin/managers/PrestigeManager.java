package me.gibson.cropPlugin.managers;

import java.util.HashMap;
import java.util.Map;

import ch.njol.skript.variables.Variables;
import me.gibson.cropPlugin.FarmTycoonPlugin;
import me.gibson.cropPlugin.utils.PrestigeRequirement;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class PrestigeManager {

    public static FarmTycoonPlugin plugin;

    public PrestigeManager(FarmTycoonPlugin plugin) {
        this.plugin = plugin;
    }

    public static int getPlayerGenSlots(Player player)
    {
        String variableName = "db::maxgenslots::" + player.getUniqueId();
        Object genslots = Variables.getVariable(variableName, null, false);

        if (genslots instanceof Number) {
            return ((Number) genslots).intValue();
        }
        return 0;
    }

    public static void setPlayerGenSlots(Player player, int genslots)
    {
        String variableName = "db::maxgenslots::" + player.getUniqueId();
        Variables.setVariable(variableName, genslots, null, false);
    }



    // Get the player's current prestige level from Skript
    public static int getPlayerPrestige(OfflinePlayer player) {
        String variableName = "prestige::" + player.getUniqueId();
        Object prestige = Variables.getVariable(variableName, null, false);

        if (prestige instanceof Number) {
            return ((Number) prestige).intValue();
        }
        return 0; // Default to prestige level 0 if not found
    }

    // Set the player's prestige level in Skript
    public static void setPlayerPrestige(Player player, int prestige) {
        String variableName = "prestige::" + player.getUniqueId();
        Variables.setVariable(variableName, prestige, null, false);

        // Update multiplier in SkriptManager
        double multiplierIncrement = plugin.getConfig().getDouble("prestige.multiplierIncrement", 0.25);
        double newMultiplier = 1.0 + (prestige * multiplierIncrement);
        SkriptManager.setMultiplier(player, newMultiplier);
    }

    // Get the prestige requirements for the next level
    public static PrestigeRequirement getRequirement(Player player) {
        int currentPrestige = getPlayerPrestige(player);
        return PrestigeRequirement.getRequirement(currentPrestige + 1);
    }

    // Handle the prestige process
    public static boolean prestige(Player player) {
        int currentLevel = SkriptManager.getLevel(player);
        double currentTokens = SkriptManager.getTokens(player);
        double currentMoney = EconomyManager.getEconomy().getBalance(player);

        // Get the next prestige requirement
        PrestigeRequirement requirement = getRequirement(player);
        if (requirement == null) {
            player.sendMessage("§cYou have reached the maximum prestige level!");
            player.closeInventory();
            return false;
        }

        // Check if the player meets the requirements
        if (currentLevel < requirement.getRequiredLevel()) {
            player.sendMessage("§cYou need to be level " + requirement.getRequiredLevel() + " to prestige!");
            player.closeInventory();
            return false;
        }
        if (currentTokens < requirement.getRequiredTokens()) {
            player.sendMessage("§cYou need " +  formatLargeNumber(requirement.getRequiredTokens()) + " tokens to prestige!");
            player.closeInventory();
            return false;
        }
        if (currentMoney < requirement.getRequiredMoney()) {
            player.sendMessage("§cYou need $" + formatLargeNumber(requirement.getRequiredMoney()) + " to prestige!");
            player.closeInventory();
            return false;
        }

        // Deduct tokens and money, reset level, and increase prestige
        SkriptManager.deductTokens(player, requirement.getRequiredTokens());
        EconomyManager.getEconomy().withdrawPlayer(player, requirement.getRequiredMoney());
        SkriptManager.setLevel(player, 1); // Reset level to 0
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "levels setLevel 1 "+ player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "levels setExp 250 "+ player.getName());
        setPlayerPrestige(player, getPlayerPrestige(player) + 1); // Increase prestige level
        setPlayerGenSlots(player, getPlayerGenSlots(player) + 5);

        // Inform the player
        double multiplierIncrement = plugin.getConfig().getDouble("prestige.multiplierIncrement", 0.25);
        player.sendMessage("§aCongratulations! You are now Prestige " + getPlayerPrestige(player) + "!");
        player.sendMessage("§aYour new multiplier is " + (1.0 + (getPlayerPrestige(player) * multiplierIncrement)) + "x.");
        return true;
    }

    private static String formatLargeNumber(double value) {
        if (value >= 1_000_000_000_000L) {
            return String.format("%.2fT", value / 1_000_000_000_000L); // Trillions
        } else if (value >= 1_000_000_000) {
            return String.format("%.2fB", value / 1_000_000_000); // Billions
        } else if (value >= 1_000_000) {
            return String.format("%.2fM", value / 1_000_000); // Millions
        } else if (value >= 1_000) {
            return String.format("%.2fK", value / 1_000); // Thousands
        } else {
            return String.valueOf((int) value);
        }
    }
}
