package me.gibson.cropPlugin.utils;

import me.gibson.cropPlugin.FarmTycoonPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataStorage {

    private final FarmTycoonPlugin plugin;
    private final File playerDataFile;
    private final FileConfiguration playerDataConfig;

    public DataStorage(FarmTycoonPlugin plugin) {
        this.plugin = plugin;
        this.playerDataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!playerDataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            plugin.saveResource("playerdata.yml", false);
        }
        this.playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
    }

    public void loadPlayerData() {
        for (String key : playerDataConfig.getKeys(false)) {
            UUID playerId = UUID.fromString(key);

            // Load selected crop
            Material selectedCrop = Material.getMaterial(playerDataConfig.getString(key + ".selectedCrop", "WHEAT"));
            Material selectedOre = Material.getMaterial(playerDataConfig.getString(key + ".selectedOre", "COAL_ORE"));
            if (selectedCrop != null) {
                plugin.getSelectedCropMap().put(playerId, selectedCrop);
            }

            if (selectedOre != null) {
                plugin.getSelectedBlockMap().put(playerId, selectedOre);
            }

            // Load prestige
            int prestige = playerDataConfig.getInt(key + ".prestige", 0);
            plugin.getPlayerPrestigeMap().put(playerId, prestige);

            // Load block data
            Map<Location, Material> blockData = loadPlayerBlockMaterials(playerId);
            plugin.setPlayerBlockData(playerId, blockData);
        }
    }

    public void savePlayerData() {
        for (UUID playerId : plugin.getPlayerPrestigeMap().keySet()) {
            savePlayerPrestige(playerId, plugin.getPlayerPrestigeMap().get(playerId));
            savePlayerSelectedCrop(playerId, plugin.getSelectedCropMap().get(playerId));
            savePlayerBlockMaterials(playerId, plugin.getPlayerBlockDataMap().get(playerId));
            savePlayerSelectedOre(playerId, plugin.getSelectedBlockMap().get(playerId));
        }
        try {
            playerDataConfig.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void savePlayerPrestige(UUID playerId, int prestige) {
        playerDataConfig.set(playerId.toString() + ".prestige", prestige);
        saveConfig();
    }

    public void savePlayerSelectedCrop(UUID playerId, Material crop) {
        if (crop != null) {
            playerDataConfig.set(playerId.toString() + ".selectedCrop", crop.name());
        }
        saveConfig();
    }

    public Map<Location, Material> loadPlayerBlockMaterials(UUID playerId) {
        Map<Location, Material> blockData = new HashMap<>();
        if (!playerDataConfig.contains(playerId.toString() + ".blocks")) return blockData;

        for (String key : playerDataConfig.getConfigurationSection(playerId.toString() + ".blocks").getKeys(false)) {
            Location location = Location.deserialize(playerDataConfig.getConfigurationSection(playerId.toString() + ".blocks." + key).getValues(false));
            Material material = Material.getMaterial(playerDataConfig.getString(playerId.toString() + ".blocks." + key + ".material"));

            if (location != null && material != null) {
                blockData.put(location, material);
            }
        }
        return blockData;
    }

    public void savePlayerBlockMaterials(UUID playerId, Map<Location, Material> blockData) {
        String path = playerId.toString() + ".blocks";
        playerDataConfig.set(path, null); // Clear old data

        int index = 0;
        for (Map.Entry<Location, Material> entry : blockData.entrySet()) {
            String key = path + "." + index;
            playerDataConfig.set(key + ".location", entry.getKey().serialize());
            playerDataConfig.set(key + ".material", entry.getValue().name());
            index++;
        }
        saveConfig();
    }

    private void saveConfig() {
        try {
            playerDataConfig.save(playerDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Material getPlayerSelectedCrop(UUID uniqueId) {
        return plugin.getSelectedCropMap().get(uniqueId);
    }

    public void savePlayerSelectedOre(UUID playerId, Material ore) {
        if (ore != null) {
            playerDataConfig.set(playerId.toString() + ".selectedOre", ore.name());
        }
        saveConfig();
    }

}
