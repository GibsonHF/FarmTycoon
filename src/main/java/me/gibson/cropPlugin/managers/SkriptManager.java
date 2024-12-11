package me.gibson.cropPlugin.managers;

import ch.njol.skript.variables.Variables;
import org.bukkit.entity.Player;

public class SkriptManager {

    // Get player's tokens
    public static double getTokens(Player player) {
        String variableName = "tokens::" + player.getUniqueId();
        Object tokens = Variables.getVariable(variableName, null, false);

        if (tokens instanceof Number) {
            return ((Number) tokens).doubleValue();
        }
        return 0;
    }

    // Add tokens to a player
    public static void addTokens(Player player, int amount) {
        String variableName = "tokens::" + player.getUniqueId();
        double currentTokens = getTokens(player);
        Variables.setVariable(variableName, currentTokens + amount, null, false);
    }

    // Deduct tokens from a player
    public static boolean deductTokens(Player player, double amount) {
        double currentTokens = getTokens(player);

        if (currentTokens >= amount) {
            String variableName = "tokens::" + player.getUniqueId();
            Variables.setVariable(variableName, currentTokens - amount, null, false);
            return true;
        }
        return false;
    }

    public static int getLevel(Player player) {
        String variableName = "level::" + player.getUniqueId();
        Object level = Variables.getVariable(variableName, null, false);

        if (level instanceof Number) {
            return ((Number) level).intValue();
        }
        return 0;
    }

    public static void setLevel(Player player, int level) {
        String variableName = "level::" + player.getUniqueId();
        Variables.setVariable(variableName, level, null, false);
    }

    public static void addLevel(Player player, int amount) {
        String variableName = "level::" + player.getUniqueId();
        int currentLevel = getLevel(player);
        Variables.setVariable(variableName, currentLevel + amount, null, false);
    }

    // Get player's multiplier
    public static double getMultiplier(Player player) {
        String variableName = "multiplier::" + player.getUniqueId();
        Object multiplier = Variables.getVariable(variableName, null, false);

        if (multiplier instanceof Number) {
            return ((Number) multiplier).doubleValue();
        }
        return 1.0; // Default multiplier
    }

    // Set player's multiplier
    public static void setMultiplier(Player player, double multiplier) {
        String variableName = "multiplier::" + player.getUniqueId();
        Variables.setVariable(variableName, multiplier, null, false);
    }

    // Increase player's multiplier by a specific amount
    public static void addMultiplier(Player player, double amount) {
        String variableName = "multiplier::" + player.getUniqueId();
        double currentMultiplier = getMultiplier(player);
        Variables.setVariable(variableName, currentMultiplier + amount, null, false);
    }

    //add player gems
    public static void addGems(Player player, int amount) {
        String variableName = "gems::" + player.getUniqueId();
        int currentGems = getGems(player);
        Variables.setVariable(variableName, currentGems + amount, null, false);
    }

    public static void setGems(Player player, int amount) {
        String variableName = "gems::" + player.getUniqueId();
        Variables.setVariable(variableName, amount, null, false);
    }

    public static int getGems(Player player) {
        String variableName = "gems::" + player.getUniqueId();
        Object gems = Variables.getVariable(variableName, null, false);

        if (gems instanceof Number) {
            return ((Number) gems).intValue();
        }
        return 0;
    }
}
