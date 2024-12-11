package me.gibson.cropPlugin.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.gibson.cropPlugin.types.CropType;
import me.gibson.cropPlugin.FarmTycoonPlugin;
import me.gibson.cropPlugin.managers.PrestigeManager;
import me.gibson.cropPlugin.managers.SkriptManager;
import me.gibson.cropPlugin.types.OreType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class FarmListener implements Listener {
    private final FarmTycoonPlugin plugin;
    private ProtocolManager protocolManager;
    private final Random rand;


    // Track last selection time per player to enforce delay between crop changes
    private final Map<UUID, Long> lastSelectionTime = new HashMap<>();

    public FarmListener(FarmTycoonPlugin plugin) {
        this.plugin = plugin;
        this.rand = new Random();
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        setupPacketInterception();
        setupChunkLoadInterceptor();
    }

    public void setupChunkLoadInterceptor() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(plugin, PacketType.Play.Server.MAP_CHUNK) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if(event.getPlayer().getWorld().getName().equals("FarmWorld")) {
                    handleMapChunkPacket(event);
                }
            }

        });
    }
    private void handleMapChunkPacket(PacketEvent event) {
        Player player = event.getPlayer();

        // Get the chunk coordinates from the packet
        PacketContainer packet = event.getPacket();
        int chunkX = packet.getIntegers().read(0);
        int chunkZ = packet.getIntegers().read(1);

        // Check if the chunk overlaps with the farm region
        World world = player.getWorld();
        Chunk chunk = world.getChunkAt(chunkX, chunkZ);

        RegionManager regionManager = plugin.getRegionManager(world);
        if (regionManager == null) return;

        for (ProtectedRegion region : regionManager.getRegions().values()) {
            if (!region.getId().equalsIgnoreCase(plugin.getFarmRegionName())) continue;

            if (isChunkInRegion(chunk, region)) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    replaceCropsForPlayerInChunk(player, chunk, region);
                }, 10L); // 10 ticks = 0.5 seconds
            }
        }
    }

    private boolean isChunkInRegion(Chunk chunk, ProtectedRegion region) {
        int chunkX = chunk.getX();
        int chunkZ = chunk.getZ();

        int minChunkX = region.getMinimumPoint().getBlockX() >> 4;
        int minChunkZ = region.getMinimumPoint().getBlockZ() >> 4;
        int maxChunkX = region.getMaximumPoint().getBlockX() >> 4;
        int maxChunkZ = region.getMaximumPoint().getBlockZ() >> 4;

        return chunkX >= minChunkX && chunkX <= maxChunkX &&
                chunkZ >= minChunkZ && chunkZ <= maxChunkZ;
    }

    private void replaceCropsForPlayerInChunk(Player player, Chunk chunk, ProtectedRegion region) {
        World world = chunk.getWorld();

        int chunkStartX = chunk.getX() << 4;
        int chunkStartZ = chunk.getZ() << 4;

        int minX = Math.max(chunkStartX, region.getMinimumPoint().getBlockX());
        int maxX = Math.min(chunkStartX + 15, region.getMaximumPoint().getBlockX());
        int minZ = Math.max(chunkStartZ, region.getMinimumPoint().getBlockZ());
        int maxZ = Math.min(chunkStartZ + 15, region.getMaximumPoint().getBlockZ());

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = region.getMinimumPoint().getBlockY(); y <= region.getMaximumPoint().getBlockY(); y++) {
                    Block block = world.getBlockAt(x, y, z);
                    //if block is air skip
                    if(block.getType() == Material.AIR) continue;

                    if (isCropType(block.getType())) {
                        Material selectedCrop = plugin.getSelectedCrop(player);
                        if (selectedCrop == null) continue;

                        BlockData blockData = Bukkit.createBlockData(selectedCrop);
                        if (blockData instanceof Ageable) {
                            ((Ageable) blockData).setAge(((Ageable) blockData).getMaximumAge());
                        }
                        if(blockData instanceof Waterlogged)
                        {
                            ((Waterlogged) blockData).setWaterlogged(false);
                        }
                        // Send the block change to the player
                        sendFakeBlockChange(player, block, blockData);
                    }
                }
            }
        }
    }




    public void setupPacketInterception()
    {
       //on packet send of block change cancel it for anyone who didnt break the block
        PacketListener listener = new PacketAdapter(plugin, PacketType.Play.Server.BLOCK_CHANGE) {
            @Override
            public void onPacketSending(PacketEvent event) {
                if(isCropType(event.getPacket().getBlockData().read(0).getType())) {
                    event.setCancelled(true);
                    //event.setPacket(null);
                }

            }
        };

        protocolManager.addPacketListener(listener);
    }

    public void openCropSelectionGUI(Player player) {
        List<CropType> crops = getSortedCrops();
        int cropsPerPage = 28;
        int totalPages = (int) Math.ceil((double) crops.size() / cropsPerPage);

        setTotalPages(player, Math.max(totalPages, 1)); // Ensure at least 1 page
        setCurrentPage(player, 1); // Start on page 1

        openCropPage(player, crops, 1); // Open the first page
    }

    private void openCropPage(Player player, List<CropType> crops, int page) {
        int cropsPerPage = 28;
        int totalPages = (int) Math.ceil((double) crops.size() / cropsPerPage); // Dynamically calculate total pages


        int startIndex = (page - 1) * cropsPerPage; // Convert to 0-based index
        int endIndex = Math.min(startIndex + cropsPerPage, crops.size());

        Inventory gui = Bukkit.createInventory(null, 54, "Select a Crop - Page " + page + " of " + totalPages);

        // Fill GUI with filler items
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }

        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                gui.setItem(i, filler);
            }
        }

        // Add crops to GUI
        int[] cropSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        for (int i = startIndex, slotIndex = 0; i < endIndex && slotIndex < cropSlots.length; i++, slotIndex++) {
            CropType crop = crops.get(i);
            gui.setItem(cropSlots[slotIndex], crop.toItemStack(PrestigeManager.getPlayerPrestige(player)));
        }

        // Add navigation buttons
        if (page > 1) { // Add Previous Page button if not on the first page
            ItemStack previousPage = new ItemStack(Material.ARROW);
            ItemMeta meta = previousPage.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + "Previous Page");
                previousPage.setItemMeta(meta);
            }
            gui.setItem(45, previousPage);
        }

        if (page < totalPages) { // Add Next Page button if not on the last page
            ItemStack nextPage = new ItemStack(Material.ARROW);
            ItemMeta meta = nextPage.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + "Next Page");
                nextPage.setItemMeta(meta);
            }
            gui.setItem(53, nextPage);
        }

        player.openInventory(gui);
    }






    private final Map<UUID, Integer> currentPageMap = new HashMap<>();
    private final Map<UUID, Integer> totalPagesMap = new HashMap<>();

    private int getCurrentPage(Player player) {
        int currentPage = currentPageMap.getOrDefault(player.getUniqueId(), 1); // Default to page 1
        return currentPage;
    }

    private void setCurrentPage(Player player, int page) {
        currentPageMap.put(player.getUniqueId(), page);
    }


    private int getTotalPages(Player player) {
        int totalPages = totalPagesMap.getOrDefault(player.getUniqueId(), 1);
        return totalPages;
    }


    private void setTotalPages(Player player, int totalPages) {
        totalPagesMap.put(player.getUniqueId(), totalPages);
    }


    private List<CropType> getSortedCrops() {
        List<CropType> crops = new ArrayList<>(CropType.getAllCrops());
        crops.sort(Comparator.comparingInt(CropType::getRequiredPrestige));
        return crops;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        ItemStack clickedItem = event.getCurrentItem();

        if (title.startsWith("Select a Crop")) {
            event.setCancelled(true); // Prevent item movement

            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            String displayName = clickedItem.getItemMeta() != null ? clickedItem.getItemMeta().getDisplayName() : "";

            if (displayName.equals(ChatColor.GREEN + "Next Page")) {
                int currentPage = getCurrentPage(player);


                setCurrentPage(player, currentPage + 1); // Move to the next page
                openCropPage(player, getSortedCrops(), currentPage + 1); // Open the next page
            } else if (displayName.equals(ChatColor.GREEN + "Previous Page")) {
                int currentPage = getCurrentPage(player);


                setCurrentPage(player, currentPage - 1); // Move to the previous page
                openCropPage(player, getSortedCrops(), currentPage - 1); // Open the previous page
            }
            else {
                // Handle crop selection
                CropType selectedCrop = CropType.fromMaterial(clickedItem.getType());
                if (selectedCrop == null) {
                    player.sendMessage(ChatColor.RED + "Invalid crop type selected.");
                    return;
                }

                UUID playerId = player.getUniqueId();
                long currentTime = System.currentTimeMillis();
                if (lastSelectionTime.containsKey(playerId)) {
                    long lastTime = lastSelectionTime.get(playerId);
                    if ((currentTime - lastTime) < 5000) {
                        player.sendMessage(ChatColor.RED + "You can only change crops every 5 seconds!");
                        return;
                    }
                }

                lastSelectionTime.put(playerId, currentTime);

                int playerPrestige = PrestigeManager.getPlayerPrestige(player);

                if (selectedCrop.getRequiredPrestige() > playerPrestige) {
                    player.sendMessage(ChatColor.RED + "You need Prestige " + selectedCrop.getRequiredPrestige() + " to use this crop.");
                } else {
                    plugin.setSelectedCrop(player, selectedCrop.getBlockMaterial());
                    ProtectedRegion region = plugin.getRegionManager(player.getWorld()).getRegion(plugin.getFarmRegionName());

                    if (region != null && selectedCrop != null) {
                        replaceCropsForPlayerInRegion(player, region, selectedCrop);
                    } else {
                        player.sendMessage(ChatColor.RED + "Could not update crops. Region or crop type not found.");
                    }
                    plugin.getDataStorage().savePlayerSelectedCrop(player.getUniqueId(), selectedCrop.getBlockMaterial());
                }
            }
        } else if (title.equals("Prestiges")) {
            event.setCancelled(true);
            if (clickedItem != null && clickedItem.getType() == Material.EMERALD) {
                boolean success = PrestigeManager.prestige(player);
                if (success) {
                    player.closeInventory();
                }
            }
        }
    }

    /**
     * Show the player all crops in the specified region as their selected crop type.
     * This does not change the server block state, only the player's view.
     */
    public void replaceCropsForPlayerInRegion(Player player, ProtectedRegion region, CropType cropType) {
        if (cropType == null) return;

        BlockData blockData;
        try {
            blockData = Bukkit.createBlockData(cropType.getBlockMaterial());
            if (blockData instanceof Ageable) {
                ((Ageable) blockData).setAge(((Ageable) blockData).getMaximumAge());
            }
            if (blockData instanceof Waterlogged) {
                ((Waterlogged) blockData).setWaterlogged(false);
            }
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cError: Invalid crop data.");
            return;
        }

        // Get the region bounds
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        World world = player.getWorld();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

        // Iterate through all blocks in the region
        for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
            for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
                for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
                    Location loc = new Location(world, x, y, z);
                    Block block = loc.getBlock();
                    if (isCropType(block.getType())) {
                        sendFakeBlockChange(player, block, blockData);
                    }
                }
            }
        }

        // Update crops in batches to prevent server lag
        new BukkitRunnable() {

            @Override
            public void run() {
                    player.sendMessage("§aCrops updated to " + cropType.getDisplayName() + "!");
                }

        }.runTask(plugin);
        });
    }


    private final Map<Location, Long> blockBreakCooldown = new HashMap<>();


    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        UUID playerId = player.getUniqueId();

        if (!player.getWorld().getName().equalsIgnoreCase("FarmWorld")) {
            return; // Only track blocks in the FarmWorld
        }

        if (!isCropType(block.getType())) return;

        Material selectedCrop = plugin.getSelectedCrop(player); // Fetch player's selected ore
        if (selectedCrop == null) {
            player.sendMessage(ChatColor.RED + "You have no crop selected!");
            event.setCancelled(true);
            return;
        }

        CropType cropType = CropType.fromMaterial(selectedCrop);
        if (cropType == null) {
            player.sendMessage(ChatColor.RED + "Invalid crop selected!");
            event.setCancelled(true);
            return;
        }

        // Check prestige requirement
        if (PrestigeManager.getPlayerPrestige(player) < cropType.getRequiredPrestige()) {
            player.sendMessage("§cYou need Prestige " + cropType.getRequiredPrestige() + " to mine this crop!");
            event.setCancelled(true);
            return;
        }

        //make sure we're using a hoeif not cancel
        if(player.getInventory().getItemInMainHand().getType() != Material.WOODEN_HOE &&
                player.getInventory().getItemInMainHand().getType() != Material.STONE_HOE &&
                player.getInventory().getItemInMainHand().getType() != Material.IRON_HOE &&
                player.getInventory().getItemInMainHand().getType() != Material.GOLDEN_HOE &&
                player.getInventory().getItemInMainHand().getType() != Material.DIAMOND_HOE &&
                player.getInventory().getItemInMainHand().getType() != Material.NETHERITE_HOE)
        {
            player.sendMessage("§cYou must use a hoe to break crops!");
            event.setCancelled(true);
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            sendFakeBlockChange(player, block, Bukkit.createBlockData(Material.AIR));
        });

        event.setCancelled(true);

        // Give them the item and rewards
        Material itemMaterial = cropType.getItemMaterial();
        ItemStack item = new ItemStack(itemMaterial);
        ItemMeta meta = item.getItemMeta();
        int baseXP = 0;
        if (meta != null) {
            // Set display name and lore with updated XP, sell price, and multiplier logic
            meta.setDisplayName("§a" + cropType.getDisplayName());
            baseXP = getBaseXP(selectedCrop);
            double multiplier = SkriptManager.getMultiplier(player); // Retrieve player's multiplier
            double sellPrice = baseXP * multiplier * 10; // Calculate sell price based on base XP and multiplier

            meta.setLore(Arrays.asList(
                    "§8Resource",
                    "§fSell Price: §6$" + String.format("%.2f", sellPrice)
            ));
            item.setItemMeta(meta);
        }
        //if inventory full and has perm condense then auto condense inv
        if(player.getInventory().firstEmpty() == -1 && player.hasPermission("farmtycoon.autocondense"))
        {
            Bukkit.dispatchCommand(player, "condense");
        }
        player.getInventory().addItem(item);

        int totalXP = (int) (getBaseXP(selectedCrop) * SkriptManager.getMultiplier(player)); // Calculate total XP
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "levels addExp " + totalXP + " " + player.getName());
        double tokensPerXP = 220.0 / 5.0;
        int tokens = (int) (totalXP * tokensPerXP);
        SkriptManager.addTokens(player, tokens); // Add tokens based on the calculated ratio


        Location loc = event.getBlock().getLocation();
        long now = System.currentTimeMillis();
        if (blockBreakCooldown.containsKey(loc) && now - blockBreakCooldown.get(loc) < 1000) {
            // Too soon, don't give rewards again
            return;
        }
        blockBreakCooldown.put(loc, now);

        // After delay, show selected crop again
        new BukkitRunnable() {
            @Override
            public void run() {
                Material currentSelected = plugin.getSelectedCrop(player);
                if (currentSelected == null) currentSelected = selectedCrop;

                BlockData cropData = Bukkit.createBlockData(currentSelected);
                if (cropData instanceof Ageable) {
                    ((Ageable) cropData).setAge(((Ageable) cropData).getMaximumAge());
                }
                if(cropData instanceof Waterlogged)
                {
                    ((Waterlogged) cropData).setWaterlogged(false);
                }
                sendFakeBlockChange(player, block, cropData);
            }
        }.runTaskLater(plugin, rand.nextInt(40) + 60);
    }

    private void sendFakeBlockChange(Player player, Block block, BlockData blockData) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.BLOCK_CHANGE);
        packet.getBlockPositionModifier().write(0, new BlockPosition(block.getX(), block.getY(), block.getZ()));
        packet.getBlockData().write(0, WrappedBlockData.createData(blockData));
        protocolManager.sendServerPacket(player, packet, false);
    }


    @EventHandler
    public void onDisconnect(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getDataStorage().savePlayerData();
    }



    @EventHandler
    public void onTrampleFarmland(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) {
            Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.FARMLAND) {
                event.setCancelled(true);
            }
        }
    }


    private boolean isCropType(Material material) {
        return CropType.fromMaterial(material) != null;
    }

    private int getBaseXP(Material cropType) {
        return CropType.getXpForCrop(cropType);
    }

    public void spawnClones(Player player)
    {
        for(int i = 0; i < 3; i++)
        {
            PacketListener listener = new PacketAdapter(plugin, PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    if(event.getPlayer().getUniqueId() == player.getUniqueId())
                    {
                        event.setCancelled(true);
                    }
                }
            };
        }
    }
}
