package me.gibson.cropPlugin.utils;

import de.tr7zw.nbtapi.NBTItem;
import me.gibson.cropPlugin.types.EnchantmentType;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ToolUtils {

    /**
     * Creates a new farming tool with a given tier, material, and enchantment slots.
     *
     * @param tier       The tier of the tool (1 to 5).
     * @param material   The material of the tool (e.g., WOODEN_HOE, IRON_HOE).
     * @param displayNameColor The color code for the tool's display name.
     * @param tierColor  The color code for the tier information.
     * @return A new farming tool ItemStack with NBT data.
     */
    public static ItemStack createFarmingTool(int tier, Material material, String displayNameColor, String tierColor) {
        ItemStack tool = new ItemStack(material);
        ItemMeta meta = tool.getItemMeta();
        assert meta != null;

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayNameColor + "&lFarming Tool &8[&fTier " + tier + "&8]"));

        // Generate default lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "A special farming tool.");
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', tierColor + "Tier: &f" + tier));
        lore.add(ChatColor.translateAlternateColorCodes('&', tierColor + "Usage: Farming crops."));
        lore.add("");
        lore.add(ChatColor.translateAlternateColorCodes('&', tierColor + "Enchantments:"));
        for (EnchantmentType enchant : EnchantmentType.values()) {
            lore.add(ChatColor.YELLOW + enchant.getDisplayName() + ": " + ChatColor.GRAY + "Level 0");
        }
        meta.setLore(lore);
        tool.setItemMeta(meta);

        // Add NBT data
        NBTItem nbtItem = new NBTItem(tool);
        nbtItem.setInteger("tier", tier);
        for (EnchantmentType enchant : EnchantmentType.values()) {
            nbtItem.setInteger(enchant.name(), 0); // Initialize enchantment levels
        }

        return nbtItem.getItem();
    }

    /**
     * Updates the lore of a tool to reflect its current enchantment levels and stats.
     *
     * @param tool The tool ItemStack to update.
     */
    public static void updateLore(ItemStack tool) {
        if (tool == null || !tool.hasItemMeta()) return;

        ItemMeta meta = tool.getItemMeta();
        assert meta != null;

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "A special farming tool.");
        lore.add("");

        NBTItem nbtItem = new NBTItem(tool);
        int tier = nbtItem.getInteger("tier");
        lore.add(ChatColor.translateAlternateColorCodes('&', "&6Tier: &f" + tier));
        lore.add(ChatColor.translateAlternateColorCodes('&', "&6Usage: Farming crops."));
        lore.add("");

        lore.add(ChatColor.translateAlternateColorCodes('&', "&6Enchantments:"));
        for (EnchantmentType enchant : EnchantmentType.values()) {
            int level = nbtItem.getInteger(enchant.name());
            lore.add(ChatColor.YELLOW + enchant.getDisplayName() + ": " + ChatColor.GRAY + "Level " + level);
        }

        meta.setLore(lore);
        tool.setItemMeta(meta);
    }

    /**
     * Checks if the given ItemStack is a valid farming tool.
     *
     * @param tool The ItemStack to check.
     * @return True if the ItemStack is a farming tool, false otherwise.
     */
    public static boolean isFarmingTool(ItemStack tool) {
        if (tool == null || !tool.hasItemMeta()) return false;

        NBTItem nbtItem = new NBTItem(tool);
        return nbtItem.hasKey("tier");
    }

    /**
     * Gets the enchantment level of a specific enchantment on a tool.
     *
     * @param tool        The tool ItemStack.
     * @param enchantment The enchantment to check.
     * @return The level of the enchantment, or 0 if not present.
     */
    public static int getEnchantmentLevel(ItemStack tool, EnchantmentType enchantment) {
        if (!isFarmingTool(tool)) return 0;

        NBTItem nbtItem = new NBTItem(tool);
        return nbtItem.getInteger(enchantment.name());
    }

    /**
     * Sets the enchantment level of a specific enchantment on a tool.
     *
     * @param tool        The tool ItemStack.
     * @param enchantment The enchantment to set.
     * @param level       The level to set.
     */
    public static void setEnchantmentLevel(ItemStack tool, EnchantmentType enchantment, int level) {
        if (!isFarmingTool(tool)) return;

        // Update the NBT data
        NBTItem nbtItem = new NBTItem(tool);
        nbtItem.setInteger(enchantment.name(), level);

        // Update the lore
        ItemStack updatedTool = nbtItem.getItem(); // Get the updated ItemStack with new NBT
        updateLore(updatedTool);

        // Reassign the updated ItemStack to the original reference
        tool.setItemMeta(updatedTool.getItemMeta());
    }


    /**
     * Calculates the total enchantment levels on a tool.
     *
     * @param tool The tool ItemStack.
     * @return The total levels of all enchantments on the tool.
     */
    public static int getTotalEnchantmentLevels(ItemStack tool) {
        if (!isFarmingTool(tool)) return 0;

        NBTItem nbtItem = new NBTItem(tool);
        int totalLevels = 0;

        for (EnchantmentType enchant : EnchantmentType.values()) {
            totalLevels += nbtItem.getInteger(enchant.name());
        }

        return totalLevels;
    }
}
