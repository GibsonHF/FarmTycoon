package me.gibson.cropPlugin.GUI;

import ch.njol.skript.util.chat.ChatCode;
import me.gibson.cropPlugin.managers.PrestigeManager;
import me.gibson.cropPlugin.managers.SkriptManager;
import me.gibson.cropPlugin.utils.PrestigeRequirement;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static me.gibson.cropPlugin.managers.PrestigeManager.plugin;

public class PrestigeGUI {

    public static void openPrestigeGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, "Prestiges");

        // Add corner panes
        addCornerPanes(gui);

        // Add other panes
        addFillerPanes(gui);

        // Add functional buttons
        gui.setItem(11, createPrestigeButton(player)); // Prestige button
        gui.setItem(13, createStatsButton()); // Prestige stats button
        gui.setItem(15, createLeaderboardButton()); // Prestige leaderboard button

        player.openInventory(gui);
    }

    private static void addCornerPanes(Inventory gui) {
        ItemStack cornerPane = createGlassPane(Material.YELLOW_STAINED_GLASS_PANE, " ");
        int[] cornerSlots = {0,1,9, 17, 25, 26};
        for (int slot : cornerSlots) {
            gui.setItem(slot, cornerPane);
        }
    }

    private static void addFillerPanes(Inventory gui) {
        ItemStack fillerPane = createGlassPane(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, fillerPane);
            }
        }
    }

    private static ItemStack createGlassPane(Material material, String displayName) {
        ItemStack pane = new ItemStack(material);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private static ItemStack createPrestigeButton(Player player) {
        int prestige = PrestigeManager.getPlayerPrestige(player);
        PrestigeRequirement requirement = PrestigeManager.getRequirement(player);

        ItemStack prestigeButton = new ItemStack(Material.EMERALD);
        ItemMeta meta = prestigeButton.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.DARK_GREEN + ""+ ChatColor.BOLD + "Prestige "+ChatColor.GREEN+""+ ChatColor.BOLD + (prestige + 1));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY+ "ᴘʀᴇsᴛɪɢᴇs");
            lore.add("");
            lore.add(ChatColor.GRAY + "When you prestige you will reset back to");
            lore.add(ChatColor.GRAY + "level 1 but will unlock new areas, more");
            lore.add(ChatColor.GRAY + "generator slots & better money multipliers!");
            lore.add("");

            if (requirement != null) {
                lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "[REQUIREMENTS]");
                lore.add(ChatColor.DARK_GREEN + " * " + ChatColor.GRAY + "Level: " + ChatColor.DARK_AQUA + "["+ ChatColor.AQUA + ChatColor.BOLD+ requirement.getRequiredLevel() + ChatColor.DARK_AQUA+ "]");
                lore.add(ChatColor.DARK_GREEN + " * " + ChatColor.GRAY + "Money: " + ChatColor.GREEN + "$" + formatLargeNumber(requirement.getRequiredMoney()));
                lore.add(ChatColor.DARK_GREEN + " * " + ChatColor.GRAY + "Tokens: " + ChatColor.YELLOW + formatLargeNumber(requirement.getRequiredTokens()));
                lore.add("");

                lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "[REWARDS]");
            lore.add(ChatColor.DARK_GREEN + " * " + ChatColor.GRAY + "Slots: " + ChatColor.WHITE + "+5 Generator Slots");
                double multiplier = SkriptManager.getMultiplier(player);
                double newMultiplier = multiplier + plugin.getConfig().getDouble("prestige.multiplierIncrement", 0.25);
                lore.add(ChatColor.DARK_GREEN + " * " + ChatColor.GRAY + "Multiplier: " + ChatColor.RED + String.format("%.2f", multiplier) + "x" + ChatColor.DARK_GRAY + " → " + ChatColor.GREEN + String.format("%.2f", newMultiplier) + "x");
                lore.add("");
                lore.add(ChatColor.GREEN + "" + ChatColor.BOLD + "CLICK HERE");
                lore.add(ChatColor.DARK_GREEN + "Click this to prestige!");
            } else {
                lore.add(ChatColor.RED + "You have reached the maximum prestige!");
            }

            meta.setLore(lore);
            prestigeButton.setItemMeta(meta);
        }

        return prestigeButton;
    }

    private static ItemStack createStatsButton() {
        ItemStack statsButton = new ItemStack(Material.CHEST);
        ItemMeta meta = statsButton.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Prestige Stats");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Every time you prestige you will gain");
            lore.add(ChatColor.GREEN + "1 Stat Point" + ChatColor.GRAY + " which can be used on");
            lore.add(ChatColor.GRAY + "prestige stats!");
            lore.add("");
            lore.add(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "CLICK HERE");
            lore.add(ChatColor.LIGHT_PURPLE + "Click this to view Stats Menu!");
            meta.setLore(lore);
            statsButton.setItemMeta(meta);
        }

        return statsButton;
    }

    private static ItemStack createLeaderboardButton() {
        ItemStack leaderboardButton = new ItemStack(Material.LADDER);
        ItemMeta meta = leaderboardButton.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Prestige Leaderboard");
            List<String> lore = new ArrayList<>();

            lore.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "LEADERBOARD");
            addLeaderboardLore(lore);
            lore.add("");
            lore.add(ChatColor.YELLOW + "" + ChatColor.BOLD + "LEADERBOARD");
            meta.setLore(lore);
            leaderboardButton.setItemMeta(meta);
        }

        return leaderboardButton;
    }

    private static void addLeaderboardLore(List<String> lore) {
        Map<OfflinePlayer, Integer> leaderboard = getPrestigeLeaderboard();
        int rank = 1;
        for (Map.Entry<OfflinePlayer, Integer> entry : leaderboard.entrySet()) {
            if (rank > 10) break; // Limit to top 10
            OfflinePlayer player = entry.getKey();
            int prestige = entry.getValue();
            lore.add(ChatColor.YELLOW + " #" + rank + " " + ChatColor.GOLD + player.getName() + ChatColor.GRAY + " (" + prestige + ")");
            rank++;
        }
    }

    private static Map<OfflinePlayer, Integer> getPrestigeLeaderboard() {
        Map<OfflinePlayer, Integer> leaderboard = new HashMap<>();
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            int prestige = PrestigeManager.getPlayerPrestige(offlinePlayer); // Pass OfflinePlayer
            if (prestige > 0) {
                leaderboard.put(offlinePlayer, prestige);
            }
        }
        return sortByValueDescending(leaderboard);
    }

    private static Map<OfflinePlayer, Integer> sortByValueDescending(Map<OfflinePlayer, Integer> map) {
        List<Map.Entry<OfflinePlayer, Integer>> list = new ArrayList<>(map.entrySet());
        list.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        Map<OfflinePlayer, Integer> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<OfflinePlayer, Integer> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
        return sortedMap;
    }

    private static String formatLargeNumber(double value) {
        if (value >= 1_000_000_000_000_000_000L) {
            return String.format("E%d", (int) Math.log10(value)); // Scientific notation for large numbers
        }else
        if (value >= 1_000_000_000_000_000_000L) {
            return String.format("%.2fQt", value / 1_000_000_000_000_000_000L); // Quintillions
        } else if (value >= 1_000_000_000_000_000L) {
            return String.format("%.2fQ", value / 1_000_000_000_000_000L); // Quadrillions
        } else if (value >= 1_000_000_000_000L) {
            return String.format("%.2fT", value / 1_000_000_000_000L); // Trillions
        } else if (value >= 1_000_000_000) {
            return String.format("%.2fB", value / 1_000_000_000); // Billions
        } else if (value >= 1_000_000) {
            return String.format("%.2fM", value / 1_000_000); // Millions
        } else if (value >= 1_000) {
            return String.format("%.2fK", value / 1_000); // Thousands
        }  else {
            return String.valueOf((int) value); // Below 1,000
        }
    }


}
