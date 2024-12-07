package me.gibson.cropPlugin.commands;

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

public class CondenseCommand implements CommandExecutor {

    private boolean isValidCondensable(ItemStack item, String expectedLore) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta().getLore() == null) {
            return false;
        }

        for (String loreLine : item.getItemMeta().getLore()) {
            if (ChatColor.stripColor(loreLine).equals(expectedLore)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;
        Inventory inventory = player.getInventory();
        List<ItemStack> condensedItems = new ArrayList<>();
        boolean anyCondensed = false; // Track if anything was condensed

        for (ItemStack item : inventory.getContents()) {
            if (item == null || !item.hasItemMeta()) continue;

            ItemMeta meta = item.getItemMeta();
            List<String> lore = meta.getLore();
            if (lore == null || lore.size() < 2) continue; // Ensure lore has at least 2 lines (including "Sell Price")

            // Check for "Sell Price" in the lore
            String sellPriceLine = ChatColor.stripColor(lore.get(1)); // Assume second line is "Sell Price"
            if (!sellPriceLine.startsWith("Sell Price: $")) continue;

            // Extract sell price from the lore
            double basePrice = Double.parseDouble(sellPriceLine.replace("Sell Price: $", ""));
            int amountToCondense = (meta.hasEnchant(Enchantment.LURE)) ? 16 : 8;

            // Condense as many items as possible
            while (item.getAmount() >= amountToCondense) {
                anyCondensed = true; // Mark that we condensed something
                item.setAmount(item.getAmount() - amountToCondense);

                // Create a condensed item with the combined price
                double combinedPrice = basePrice * amountToCondense;
                ItemStack condensedItem = createCondensedItem(item, combinedPrice);
                condensedItems.add(condensedItem);
            }
        }

        // Add condensed items back to the inventory
        condensedItems.forEach(inventory::addItem);

        if (anyCondensed) {
            player.sendMessage("§aYour crops have been condensed!");
        } else {
            player.sendMessage("§cYou don’t have enough crops to condense!");
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
            newMeta.addEnchant(Enchantment.LURE, 1, true);
            newItem.setItemMeta(newMeta);
        }
        return newItem;
    }


}
