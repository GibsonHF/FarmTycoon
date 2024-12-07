package me.gibson.cropPlugin.managers;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    private static Economy economy = null;

    // Setup economy (called in onEnable)
    public static boolean setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    // Get the Economy instance
    public static Economy getEconomy() {
        return economy;
    }

    // Check a player's balance
    public static double getBalance(Player player) {
        return economy.getBalance(player);
    }

    // Withdraw money from a player
    public static boolean withdraw(Player player, double amount) {
        if (economy.has(player, amount)) {
            economy.withdrawPlayer(player, amount);
            return true;
        }
        return false;
    }

    // Deposit money to a player
    public static void deposit(Player player, double amount) {
        economy.depositPlayer(player, amount);
    }
}
