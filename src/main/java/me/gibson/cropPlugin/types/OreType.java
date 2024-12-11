package me.gibson.cropPlugin.types;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OreType {

    private static final Map<String, OreType> oreTypes = new HashMap<>();
    private final Material blockMaterial;
    private final Material itemMaterial;
    private final String displayName;
    private final int requiredPrestige;
    private final int xpPerOre;

    public OreType(Material blockMaterial, Material itemMaterial, String displayName, int requiredPrestige, int xpPerOre) {
        this.blockMaterial = blockMaterial;
        this.itemMaterial = itemMaterial;
        this.displayName = displayName;
        this.requiredPrestige = requiredPrestige;
        this.xpPerOre = xpPerOre;
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

    public int getXpPerOre() {
        return xpPerOre;
    }

    public static OreType fromMaterial(Material material) {
        return oreTypes.values().stream()
                .filter(ore -> ore.getBlockMaterial() == material || ore.getItemMaterial() == material)
                .findFirst()
                .orElse(null);
    }

    public static void loadOreTypes(FileConfiguration config) {
        oreTypes.clear();

        if (!config.contains("ores")) {
            throw new IllegalArgumentException("No 'ores' section found in config.yml");
        }

        for (String key : config.getConfigurationSection("ores").getKeys(false)) {
            Material blockMaterial = Material.matchMaterial(config.getString("ores." + key + ".blockMaterial"));
            Material itemMaterial = Material.matchMaterial(config.getString("ores." + key + ".itemMaterial"));
            String displayName = config.getString("ores." + key + ".displayName");
            int requiredPrestige = config.getInt("ores." + key + ".requiredPrestige");
            int xpPerOre = config.getInt("ores." + key + ".xp", 1); // Default XP if not specified

            if (blockMaterial != null && itemMaterial != null && displayName != null) {
                oreTypes.put(key, new OreType(blockMaterial, itemMaterial, displayName, requiredPrestige, xpPerOre));
            }
        }
    }

    public static int getXpForOre(Material material) {
        OreType ore = fromMaterial(material);
        return ore != null ? ore.getXpPerOre() : 0; // Default 0 if ore not found
    }

    public static List<OreType> getAllOres() {
        return new ArrayList<>(oreTypes.values());
    }

    public ItemStack toItemStack(int playerPrestige) {
        ItemStack item = new ItemStack(itemMaterial); // Create the item stack with the ore's item material
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Display name with unlock status
            String unlockedStatus = playerPrestige >= requiredPrestige ? "§a[UNLOCKED]" : "§c[LOCKED]";
            meta.setDisplayName("§e" + displayName + " " + unlockedStatus);

            // Lore for the item
            List<String> lore = new ArrayList<>();
            if (playerPrestige >= requiredPrestige) {
                lore.add("§7Selecting this will enable mining of");
                lore.add("§7§e" + displayName + "§7 in the mine.");
                lore.add("");
                lore.add("§a§lCLICK HERE");
                lore.add("§aClick this to select!");
            } else {
                lore.add("§cYou cannot mine this ore yet!");
                lore.add("§cYou must be §6Prestige " + requiredPrestige + " §cto unlock this ore!");
                lore.add("");
                lore.add("§c§lLOCKED");
                lore.add("§cPrestige to unlock this ore!");
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }
}
