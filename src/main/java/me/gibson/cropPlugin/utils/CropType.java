package me.gibson.cropPlugin.utils;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CropType {

    private static final Map<String, CropType> cropTypes = new HashMap<>();
    private final Material blockMaterial;
    private final Material itemMaterial;
    private final String displayName;
    private final int requiredPrestige;
    private final int xpPerCrop;

    public CropType(Material blockMaterial, Material itemMaterial, String displayName, int requiredPrestige, int xpPerCrop) {
        this.blockMaterial = blockMaterial;
        this.itemMaterial = itemMaterial;
        this.displayName = displayName;
        this.requiredPrestige = requiredPrestige;
        this.xpPerCrop = xpPerCrop;
    }

    public Material getBlockMaterial() {
        return blockMaterial;
    }

    public Material getItemMaterial() {
        return itemMaterial;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getRequiredPrestige() {
        return requiredPrestige;
    }

    public int getXpPerCrop() {
        return xpPerCrop;
    }

    public static CropType fromMaterial(Material material) {
        return cropTypes.values().stream()
                .filter(crop -> crop.getBlockMaterial() == material || crop.getItemMaterial() == material)
                .findFirst()
                .orElse(null);
    }

    public static void loadCropTypes(FileConfiguration config) {
        cropTypes.clear();

        if (!config.contains("crops")) {
            throw new IllegalArgumentException("No 'crops' section found in config.yml");
        }

        for (String key : config.getConfigurationSection("crops").getKeys(false)) {
            Material blockMaterial = Material.matchMaterial(config.getString("crops." + key + ".blockMaterial"));
            Material itemMaterial = Material.matchMaterial(config.getString("crops." + key + ".itemMaterial"));
            String displayName = config.getString("crops." + key + ".displayName");
            int requiredPrestige = config.getInt("crops." + key + ".requiredPrestige");
            int xpPerCrop = config.getInt("crops." + key + ".xp", 1); // Default XP if not specified

            if (blockMaterial != null && itemMaterial != null && displayName != null) {
                cropTypes.put(key, new CropType(blockMaterial, itemMaterial, displayName, requiredPrestige, xpPerCrop));
            }
        }
    }

    public static int getXpForCrop(Material material) {
        CropType crop = fromMaterial(material);
        return crop != null ? crop.getXpPerCrop() : 0; // Default 0 if crop not found
    }

    public static List<CropType> getAllCrops() {
        return new ArrayList<>(cropTypes.values());
    }
    public ItemStack toItemStack(int playerPrestige) {
        ItemStack item = new ItemStack(itemMaterial); // Create the item stack with the crop's item material
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Display name with unlock status
            String unlockedStatus = playerPrestige >= requiredPrestige ? "§a[UNLOCKED]" : "§c[LOCKED]";
            meta.setDisplayName("§e" + displayName + " " + unlockedStatus);

            // Lore for the item
            List<String> lore = new ArrayList<>();
            if (playerPrestige >= requiredPrestige) {
                lore.add("§7Selecting this will turn all the");
                lore.add("§7current farm crops into §e" + displayName + "§7.");
                lore.add("");
                lore.add("§a§lCLICK HERE");
                lore.add("§aClick this to select!");
            } else {
                lore.add("§cYou cannot select this crop yet!");
                lore.add("§cYou must be §6Prestige " + requiredPrestige + " §cto use this crop!");
                lore.add("");
                lore.add("§c§lLOCKED");
                lore.add("§cPrestige to unlock this crop!");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

}
