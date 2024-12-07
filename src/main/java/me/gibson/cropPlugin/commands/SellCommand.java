package me.gibson.cropPlugin.commands;

import me.gibson.cropPlugin.managers.EconomyManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class SellCommand implements CommandExecutor {

    private final Economy economy;

    public SellCommand(Economy economy) {
        this.economy = economy;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;
        Inventory inventory = player.getInventory();
        double totalSellPrice = 0; // Track total money earned
        boolean anySold = false; // Track if anything was sold

        for (ItemStack item : inventory.getContents()) {
            if (item == null || !item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.getLore();
            if (lore == null || lore.isEmpty()) continue;

            // Look for "Sell Price" in lore
            String sellPriceLine = null;
            for (String loreLine : lore) {
                if (ChatColor.stripColor(loreLine).startsWith("Sell Price: $")) {
                    sellPriceLine = ChatColor.stripColor(loreLine);
                    break;
                }
            }
            if (sellPriceLine == null) continue; // Skip items without a sell price

            // Extract sell price
            double sellPrice = Double.parseDouble(sellPriceLine.replace("Sell Price: $", ""));
            int itemAmount = item.getAmount();

            // Add to total sell price
            totalSellPrice += sellPrice * itemAmount;
            inventory.remove(item); // Remove the item from inventory
            anySold = true;
        }

        if (anySold) {
            // Deposit the total earned money
            EconomyManager.getEconomy().depositPlayer(player, totalSellPrice);
            player.sendMessage("§aYou sold your crops for §6$" + formatLargeNumber(totalSellPrice) + "§a!");
        } else {
            player.sendMessage("§cYou don’t have any crops to sell!");
        }

        return true;
    }


    private ItemStack createCondensedItem(ItemStack original, double combinedPrice) {
        ItemStack newItem = new ItemStack(original.getType());
        ItemMeta newMeta = newItem.getItemMeta();
        if (newMeta != null) {
            newMeta.setDisplayName(original.getItemMeta().getDisplayName());
            List<String> newLore = new ArrayList<>(original.getItemMeta().getLore());

            // Update the sell price
            newLore.set(1, "§fSell Price: §6$" + combinedPrice);
            newMeta.setLore(newLore);

            // Add an invisible enchant to indicate it's condensed
            newMeta.addEnchant(Enchantment.LURE, original.getItemMeta().getEnchantLevel(Enchantment.LURE), true);
            newItem.setItemMeta(newMeta);
        }
        return newItem;
    }

    private static String formatLargeNumber(double value) {
        if (value >= 1_000_000_000) {
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
