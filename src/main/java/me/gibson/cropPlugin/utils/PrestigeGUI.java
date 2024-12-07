package me.gibson.cropPlugin.utils;

import me.gibson.cropPlugin.managers.PrestigeManager;
import me.gibson.cropPlugin.managers.SkriptManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

import static me.gibson.cropPlugin.managers.PrestigeManager.plugin;

public class PrestigeGUI {

    public static void openPrestigeGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "Prestiges");

        // Retrieve current prestige and next requirements
        int prestige = PrestigeManager.getPlayerPrestige(player);
        PrestigeRequirement requirement = PrestigeManager.getRequirement(player);

        // Button for prestige
        ItemStack prestigeButton = new ItemStack(Material.EMERALD);
        ItemMeta prestigeMeta = prestigeButton.getItemMeta();

        if (prestigeMeta != null) {
            prestigeMeta.setDisplayName("§aPrestige " + (prestige + 1));
            List<String> lore = new ArrayList<>();
            lore.add("§7When you prestige you will reset back to");
            lore.add("§7level 1 but unlock new perks & multipliers!");
            lore.add("");

            if (requirement != null) {
                lore.add("§8[§6REQUIREMENTS§8]");
                lore.add("§7* Level: §b[" + requirement.getRequiredLevel() + "]");
                lore.add("§7* Tokens: §e" + formatLargeNumber(requirement.getRequiredTokens()));
                lore.add("§7* Money: §6$" + formatLargeNumber(requirement.getRequiredMoney()));
                lore.add("");
                lore.add("§8[§aREWARDS§8]");
                double multiplierIncrement = plugin.getConfig().getDouble("prestige.multiplierIncrement", 0.25);
                lore.add("§7* New Multiplier: §e" + SkriptManager.getMultiplier(player) + "x §7→ §a" + (SkriptManager.getMultiplier(player) + multiplierIncrement) + "x");
                lore.add("");
                lore.add("§a§lCLICK HERE");
                lore.add("§7Click to prestige!");
            } else {
                lore.add("§cYou have reached the maximum prestige!");
            }

            prestigeMeta.setLore(lore);
            prestigeButton.setItemMeta(prestigeMeta);
        }

        gui.setItem(13, prestigeButton);
        player.openInventory(gui);
    }


    // Utility function to format large numbers (e.g., 1000000 -> 1,000,000)
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

