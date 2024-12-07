package me.gibson.cropPlugin;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import me.gibson.cropPlugin.commands.CondenseCommand;
import me.gibson.cropPlugin.commands.FarmGUICommand;
import me.gibson.cropPlugin.commands.PrestigeCommand;
import me.gibson.cropPlugin.commands.SellCommand;
import me.gibson.cropPlugin.listeners.FarmTycoonListener;
import me.gibson.cropPlugin.managers.EconomyManager;
import me.gibson.cropPlugin.managers.PrestigeManager;
import me.gibson.cropPlugin.placeholders.PrestigePlaceholder;
import me.gibson.cropPlugin.utils.CropType;
import me.gibson.cropPlugin.utils.DataStorage;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FarmTycoonPlugin extends JavaPlugin {

    private final Map<UUID, Integer> playerPrestige = new HashMap<>();
    private final Map<UUID, Material> selectedCrop = new HashMap<>();
    private final Map<UUID, Map<Location, Material>> playerBlockData = new HashMap<>();
    private DataStorage dataStorage;
    private String farmRegionName;
    private RegionContainer regionContainer;

    @Override
    public void onEnable() {
        this.dataStorage = new DataStorage(this);
        dataStorage.loadPlayerData();
        farmRegionName = getConfig().getString("farmRegion.name", "FarmRegion");
       // keepRegionChunksLoaded(getFarmRegionName());
        // Save the default configuration file if it doesn't exist
        saveDefaultConfig();
        // Load crops from the configuration into CropType
        CropType.loadCropTypes(getConfig());
        if (!setupWorldGuard()) {
            getLogger().severe("WorldGuard not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("WorldGuard hooked successfully!");
        if (!EconomyManager.setupEconomy()) {
            getLogger().severe("Vault economy not found! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PrestigePlaceholder(new PrestigeManager(this)).register();
        }
        getLogger().info("Vault economy hooked successfully!");
        getCommand("farmgui").setExecutor(new FarmGUICommand(this));
        getCommand("prestige").setExecutor(new PrestigeCommand());
        getCommand("condense").setExecutor(new CondenseCommand());
        getCommand("sell").setExecutor(new SellCommand(EconomyManager.getEconomy()));
        getServer().getPluginManager().registerEvents(new FarmTycoonListener(this), this);

        getLogger().info("FarmTycoonPlugin Enabled!");
    }

    public String getFarmRegionName() {
        return farmRegionName;
    }

    @Override
    public void onDisable() {
        dataStorage.savePlayerData();
        for (BukkitRunnable task : playerTasks.values()) {
            task.cancel();
        }
        playerTasks.clear();
        getLogger().info("FarmTycoonPlugin Disabled!");
    }

    public DataStorage getDataStorage() {
        return dataStorage;
    }

    // Prestige Management
    public int getPrestige(Player player) {
        return playerPrestige.getOrDefault(player.getUniqueId(), 0);
    }

    public void setPrestige(Player player, int prestige) {
        playerPrestige.put(player.getUniqueId(), prestige);
        dataStorage.savePlayerPrestige(player.getUniqueId(), prestige);
    }

    // Selected Crop Management
    public Material getSelectedCrop(Player player) {
        return selectedCrop.getOrDefault(player.getUniqueId(), Material.WHEAT);
    }

    public void setSelectedCrop(Player player, Material crop) {
        selectedCrop.put(player.getUniqueId(), crop);
        dataStorage.savePlayerSelectedCrop(player.getUniqueId(), crop);
    }

    public Map<UUID, Material> getSelectedCropMap() {
        return selectedCrop;
    }

    // Player Block Data Management
    public Map<Location, Material> getPlayerBlockData(Player player) {
        return playerBlockData.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
    }

    public void setPlayerBlockData(UUID playerId, Map<Location, Material> blockData) {
        playerBlockData.put(playerId, blockData);
    }

    public Map<UUID, Map<Location, Material>> getPlayerBlockDataMap() {
        return playerBlockData;
    }

    public Map<UUID, Integer> getPlayerPrestigeMap() {
        return playerPrestige;
    }
    private boolean setupWorldGuard() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
        if (plugin instanceof WorldGuardPlugin) {
            this.regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();
            return true;
        }
        return false;
    }

    public RegionManager getRegionManager(org.bukkit.World world) {
        if (regionContainer != null) {
            return regionContainer.get(BukkitAdapter.adapt(world));
        }
        return null;
    }

    private void keepRegionChunksLoaded(String regionName) {
        RegionManager regionManager = getRegionManager(Bukkit.getWorld("GensSpawn")); // Replace "world" with your world name
        if (regionManager == null) return;

        ProtectedRegion region = regionManager.getRegion(regionName);
        if (region == null) return;

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();

        World world = Bukkit.getWorld("world");
        if (world == null) return;

        for (int x = min.getBlockX() >> 4; x <= max.getBlockX() >> 4; x++) {
            for (int z = min.getBlockZ() >> 4; z <= max.getBlockZ(); z++) {
                Chunk chunk = world.getChunkAt(x, z);
                chunk.setForceLoaded(true); // Keep the chunk loaded
            }
        }

        getLogger().info("Chunks in region " + regionName + " are now force-loaded.");
    }

    public final Map<UUID, BukkitRunnable> playerTasks = new HashMap<>();


    public void cancelPlayerTask(UUID playerId) {
        if (playerTasks.containsKey(playerId)) {
            playerTasks.get(playerId).cancel();
            playerTasks.remove(playerId);
        }
    }

}
