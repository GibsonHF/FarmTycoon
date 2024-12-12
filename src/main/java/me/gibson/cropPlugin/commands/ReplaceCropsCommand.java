package me.gibson.cropPlugin.commands;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.gibson.cropPlugin.FarmTycoonPlugin;
import me.gibson.cropPlugin.types.CropType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ReplaceCropsCommand implements CommandExecutor, TabCompleter {

    private final FarmTycoonPlugin plugin;

    public ReplaceCropsCommand(FarmTycoonPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("farmtycoon.replacecrops")) {
            player.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if (args.length != 1) {
            player.sendMessage("Usage: /replacecrops <crop>");
            return true;
        }

        Material cropMaterial;
        try {
            cropMaterial = Material.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            player.sendMessage("Invalid crop type.");
            return true;
        }

        if (!isCropType(cropMaterial)) {
            player.sendMessage("Invalid crop type.");
            return true;
        }

        RegionManager regionManager = plugin.getRegionManager(player.getWorld());
        if (regionManager == null) {
            player.sendMessage("Region manager not found.");
            return true;
        }

        ProtectedRegion region = regionManager.getRegion(plugin.getFarmRegionName());
        if (region == null) {
            player.sendMessage("Farm region not found.");
            return true;
        }

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
                for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                    for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                        Location loc = new Location(player.getWorld(), x, y, z);
                        Block block = loc.getBlock();
                        if (block.getType() == Material.FARMLAND) {
                            Block aboveBlock = loc.add(0, 1, 0).getBlock();
                                aboveBlock.setType(cropMaterial);
                                if (aboveBlock.getBlockData() instanceof Ageable) {
                                    Ageable ageable = (Ageable) aboveBlock.getBlockData();
                                    ageable.setAge(ageable.getMaximumAge());
                                    aboveBlock.setBlockData(ageable);
                                }
                        }
                    }
                }
            }
            player.sendMessage("Crops replaced with " + cropMaterial.name() + " at max age.");
        });

        return true;
    }

    private boolean isCropType(Material material) {
        return CropType.fromMaterial(material) != null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return CropType.getAllCrops().stream()
                    .map(CropType::getBlockMaterial)
                    .map(Material::name)
                    .filter(crop -> crop.startsWith(args[0].toUpperCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}