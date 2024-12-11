package me.gibson.cropPlugin;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import me.gibson.cropPlugin.commands.*;
import me.gibson.cropPlugin.listeners.*;
import me.gibson.cropPlugin.commands.GGWaveCommand;
import me.gibson.cropPlugin.managers.EconomyManager;
import me.gibson.cropPlugin.managers.PrestigeManager;
import me.gibson.cropPlugin.placeholders.PrestigePlaceholder;
import me.gibson.cropPlugin.types.CropType;
import me.gibson.cropPlugin.types.OreType;
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
    private final Map<UUID, Material> selectedOre = new HashMap<>();
    private final Map<UUID, Map<Location, Material>> playerBlockData = new HashMap<>();
    private DataStorage dataStorage;
    private String farmRegionName;
    public String mineRegionName;
    private RegionContainer regionContainer;

    @Override
    public void onEnable() {
        this.dataStorage = new DataStorage(this);
        dataStorage.loadPlayerData();
        farmRegionName = getConfig().getString("farmRegion.name", "FarmRegion");
        mineRegionName = getConfig().getString("mineRegion.name", "MineRegion");
        saveDefaultConfig();
        CropType.loadCropTypes(getConfig());
        OreType.loadOreTypes(getConfig());
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


        /* Register commands */
        getCommand("farmgui").setExecutor(new FarmGUICommand(this));
        getCommand("prestige").setExecutor(new PrestigeCommand());
        getCommand("condense").setExecutor(new CondenseCommand());
        getCommand("sell").setExecutor(new SellCommand(EconomyManager.getEconomy()));
        getCommand("gems").setExecutor(new GemsCommand());
        getCommand("ggwave").setExecutor(new GGWaveCommand(this));
        getCommand("setprestige").setExecutor(new SetPrestigeCommand());
        getCommand("minegui").setExecutor(new MineGUICommand(this));
        /* End Commands */

        /* Register listeners */
        getServer().getPluginManager().registerEvents(new GGWaveCommand(this), this);
        getServer().getPluginManager().registerEvents(new FarmListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(), this);
        getServer().getPluginManager().registerEvents(new MineListener(this), this);
        getServer().getPluginManager().registerEvents(new FishingListener(this), this);
        /* End Listeners */

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

    public final Map<UUID, BukkitRunnable> playerTasks = new HashMap<>();

    public Material getSelectedOre(Player player) {
        return selectedOre.getOrDefault(player.getUniqueId(), Material.COAL_ORE);
    }

   public String getMineRegionName() {
        return mineRegionName;
    }

    public void setSelectedOre(Player player, Material ore) {
        selectedOre.put(player.getUniqueId(), ore);
        dataStorage.savePlayerSelectedOre(player.getUniqueId(), ore);
    }

    public Map<UUID, Material> getSelectedBlockMap() {
        return selectedOre;
    }
}
