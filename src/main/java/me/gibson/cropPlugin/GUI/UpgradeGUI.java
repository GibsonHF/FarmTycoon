package me.gibson.cropPlugin.GUI;

import de.tr7zw.nbtapi.NBTItem;
import me.gibson.cropPlugin.managers.SkriptManager;
import me.gibson.cropPlugin.types.EnchantmentType;
import me.gibson.cropPlugin.utils.ToolUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class UpgradeGUI implements Listener {

    public static void openUpgradeGUI(Player player, ItemStack tool) {
        Inventory gui = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "Upgrade Enchantments");
        NBTItem nbtItem = new NBTItem(tool);

        int slot = 10; // Starting slot for enchantments
        for (EnchantmentType enchant : EnchantmentType.values()) {
            int level = nbtItem.getInteger(enchant.name());

            gui.setItem(slot, createEnchantItem(enchant, level));
            slot += 2;
        }

        player.openInventory(gui);
    }

    private static ItemStack createEnchantItem(EnchantmentType enchant, int level) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        assert meta != null;

        double cost = enchant.calculateCost(level);
        meta.setDisplayName(ChatColor.GREEN + enchant.getDisplayName());
        meta.setLore(Arrays.asList(
                ChatColor.GRAY + "Level: " + ChatColor.YELLOW + level + "/" + enchant.getMaxLevel(),
                ChatColor.GRAY + "Cost: " + ChatColor.GOLD + cost + " Tokens",
                ChatColor.LIGHT_PURPLE + "Click to upgrade!"
        ));
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(ChatColor.DARK_PURPLE + "Upgrade Enchantments")) return;

        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        ItemStack tool = player.getInventory().getItemInMainHand();

        if (clicked == null || !clicked.hasItemMeta()) return;

        NBTItem nbtTool = new NBTItem(tool);
        for (EnchantmentType enchant : EnchantmentType.values()) {
            if (clicked.getItemMeta().getDisplayName().contains(enchant.getDisplayName())) {
                int level = nbtTool.getInteger(enchant.name());

                // Check if enchantment is already maxed
                if (level >= enchant.getMaxLevel()) {
                    player.sendMessage(ChatColor.RED + "This enchantment is already maxed out!");
                    return;
                }

                // Calculate cost and deduct tokens
                double cost = enchant.calculateCost(level);
                if (SkriptManager.getTokens(player) >= cost) {
                    SkriptManager.deductTokens(player, (long) cost);

                    // Increment enchantment level and update the tool
                    ToolUtils.setEnchantmentLevel(tool, enchant, level + 1);

                    // Update the player's inventory
                    player.getInventory().setItemInMainHand(tool);

                    player.sendMessage(ChatColor.GREEN + "Upgraded " + enchant.getDisplayName() + " to Level " + (level + 1));
                } else {
                    player.sendMessage(ChatColor.RED + "Not enough tokens!");
                }
                return;
            }
        }
    }

}
