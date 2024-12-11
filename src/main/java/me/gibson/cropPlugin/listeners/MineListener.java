package me.gibson.cropPlugin.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.gibson.cropPlugin.types.CropType;
import me.gibson.cropPlugin.types.OreType;
import me.gibson.cropPlugin.FarmTycoonPlugin;
import me.gibson.cropPlugin.managers.PrestigeManager;
import me.gibson.cropPlugin.managers.SkriptManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class MineListener implements Listener {
    private final FarmTycoonPlugin plugin;
    private ProtocolManager protocolManager;
    private final Random rand;

    // Track the block location and player who broke it
    private Location brokenBlockLocation;
    private Player breakerPlayer;

    // Track last selection time per player to enforce delay between ore changes
    private final Map<UUID, Long> lastSelectionTime = new HashMap<>();

    public MineListener(FarmTycoonPlugin plugin) {
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
                if(event.getPlayer().getWorld().getName().equals("CaveWorld")) {
                    handleMapChunkPacket(event);
                }
            }
        });
    }

    private void handleMapChunkPacket(PacketEvent event) {
        Player player = event.getPlayer();
        PacketContainer packet = event.getPacket();
        int chunkX = packet.getIntegers().read(0);
        int chunkZ = packet.getIntegers().read(1);

        World world = player.getWorld();
        Chunk chunk = world.getChunkAt(chunkX, chunkZ);

        RegionManager regionManager = plugin.getRegionManager(world);
        if (regionManager == null) return;

        for (ProtectedRegion region : regionManager.getRegions().values()) {
            if (!region.getId().equalsIgnoreCase(plugin.getMineRegionName())) continue;

            if (isChunkInRegion(chunk, region)) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    replaceOresForPlayerInChunk(player, chunk, region);
                }, 10L);
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

    private void replaceOresForPlayerInChunk(Player player, Chunk chunk, ProtectedRegion region) {
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

                    if (isOreType(block.getType())) {
                        Material selectedOre = plugin.getSelectedOre(player);
                        if (selectedOre == null) continue;

                        BlockData blockData = Bukkit.createBlockData(selectedOre);
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
                if(isOreType(event.getPacket().getBlockData().read(0).getType())) {
                    event.setCancelled(true);
                    //event.setPacket(null);
                }

            }
        };

        protocolManager.addPacketListener(listener);
    }
    private final Map<Location, Long> blockBreakCooldown = new HashMap<>();

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (!player.getWorld().getName().equalsIgnoreCase("CaveWorld")) {
            return;
        }

        if (!isOreType(block.getType())) return;


        Material selectedOre = plugin.getSelectedOre(player); // Fetch player's selected ore
        if (selectedOre == null) {
            player.sendMessage(ChatColor.RED + "You have no ore selected!");
            event.setCancelled(true);
            return;
        }

        OreType oreType = OreType.fromMaterial(selectedOre);
        if (oreType == null) {
            player.sendMessage(ChatColor.RED + "Invalid ore selected!");
            event.setCancelled(true);
            return;
        }

        // Check prestige requirement
        if (PrestigeManager.getPlayerPrestige(player) < oreType.getRequiredPrestige()) {
            player.sendMessage("§cYou need Prestige " + oreType.getRequiredPrestige() + " to mine this ore!");
            event.setCancelled(true);
            return;
        }

//        // Ensure player uses a valid pickaxe
        if (player.getInventory().getItemInMainHand().getType() != Material.WOODEN_PICKAXE &&
                player.getInventory().getItemInMainHand().getType() != Material.STONE_PICKAXE &&
                player.getInventory().getItemInMainHand().getType() != Material.IRON_PICKAXE &&
                player.getInventory().getItemInMainHand().getType() != Material.GOLDEN_PICKAXE &&
                player.getInventory().getItemInMainHand().getType() != Material.DIAMOND_PICKAXE &&
                player.getInventory().getItemInMainHand().getType() != Material.NETHERITE_PICKAXE) {
            player.sendMessage("§cYou must use a pickaxe to break ores!");
            event.setCancelled(true);
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            sendFakeBlockChange(player, block, Bukkit.createBlockData(Material.BEDROCK));
        });

        // Record that this block is "broken" for this player
        brokenBlockLocation = block.getLocation();
        breakerPlayer = player;


        event.setCancelled(true);
        event.setExpToDrop(0);
        event.setDropItems(false);
        // Give the player rewards
        giveRewards(player, oreType);

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
                Material currentSelected = plugin.getSelectedOre(player);
                if (currentSelected == null) currentSelected = selectedOre;

                BlockData oreData = Bukkit.createBlockData(currentSelected);
                sendFakeBlockChange(player, block, oreData);

                // Restore normal behavior
                brokenBlockLocation = null;
                breakerPlayer = null;
            }
        }.runTaskLater(plugin, rand.nextInt(40) + 60);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        ItemStack clickedItem = event.getCurrentItem();

        if (title.startsWith("Select a Ore")) {
            event.setCancelled(true); // Prevent item movement

            if (clickedItem == null || clickedItem.getType() == Material.AIR) {
                return;
            }

            String displayName = clickedItem.getItemMeta() != null ? clickedItem.getItemMeta().getDisplayName() : "";

            if (displayName.equals(ChatColor.GREEN + "Next Page")) {
                int currentPage = getCurrentPage(player);


                setCurrentPage(player, currentPage + 1); // Move to the next page
                openOrePage(player, getSortedOres(), currentPage + 1); // Open the next page
            } else if (displayName.equals(ChatColor.GREEN + "Previous Page")) {
                int currentPage = getCurrentPage(player);


                setCurrentPage(player, currentPage - 1); // Move to the previous page
                openOrePage(player, getSortedOres(), currentPage - 1); // Open the previous page
            }
            else {
                OreType selectedOre = OreType.fromMaterial(clickedItem.getType());
                if (selectedOre == null) {
                    player.sendMessage(ChatColor.RED + "Invalid ore type selected.");
                    return;
                }

                UUID playerId = player.getUniqueId();
                long currentTime = System.currentTimeMillis();
                if (lastSelectionTime.containsKey(playerId)) {
                    long lastTime = lastSelectionTime.get(playerId);
                    if ((currentTime - lastTime) < 5000) {
                        player.sendMessage(ChatColor.RED + "You can only change ores every 5 seconds!");
                        return;
                    }
                }

                lastSelectionTime.put(playerId, currentTime);

                int playerPrestige = PrestigeManager.getPlayerPrestige(player);

                if (selectedOre.getRequiredPrestige() > playerPrestige) {
                    player.sendMessage(ChatColor.RED + "You need Prestige " + selectedOre.getRequiredPrestige() + " to use this ore.");
                } else {
                    plugin.setSelectedOre(player, selectedOre.getBlockMaterial());
                    ProtectedRegion region = plugin.getRegionManager(player.getWorld()).getRegion(plugin.getMineRegionName());

                    if (region != null && selectedOre != null) {
                        replaceOresForPlayerInRegion(player, region, selectedOre);
                    } else {
                        player.sendMessage(ChatColor.RED + "Could not update ores. Region or ore type not found.");
                    }
                    plugin.getDataStorage().savePlayerSelectedOre(player.getUniqueId(), selectedOre.getBlockMaterial());
                }
            }
        }
    }

    /**
     * Show the player all ores in the specified region as their selected ores type.
     * This does not change the server block state, only the player's view.
     */
    public void replaceOresForPlayerInRegion(Player player, ProtectedRegion region, OreType oreType) {
        if (oreType == null) return;

        BlockData blockData;
        try {
            blockData = Bukkit.createBlockData(oreType.getBlockMaterial());
        } catch (IllegalArgumentException e) {
            player.sendMessage("§cError: Invalid ore data.");
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
                    if (isOreType(block.getType())) {
                        sendFakeBlockChange(player, block, blockData);
                    }
                }
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                player.sendMessage("§aOres updated to " + oreType.getDisplayName() + "!");
            }
        }.runTask(plugin);
        });
    }

    private void giveRewards(Player player, OreType oreType) {
        ItemStack item = new ItemStack(oreType.getItemMaterial());
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + oreType.getDisplayName());
            meta.setLore(Collections.singletonList(ChatColor.GRAY + "Sell Price: " + ChatColor.GOLD + "$" +
                    calculateSellPrice(oreType, player)));
            item.setItemMeta(meta);
        }

        //random to add items 1 in 5 chance
        if (rand.nextInt(5) == 0) {
            player.getInventory().addItem(item);
        }

        int totalXP = (int) (oreType.getXpPerOre() * SkriptManager.getMultiplier(player)); // Calculate total XP
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "levels addExp " + totalXP + " " + player.getName());
        int totalTokens = (int) (oreType.getXpPerOre() * SkriptManager.getMultiplier(player) * 50);
        SkriptManager.addTokens(player, totalTokens); // Add tokens equivalent to base XP

    }

    private String calculateSellPrice(OreType oreType, Player player) {
        return String.format("%.2f", oreType.getXpPerOre() * SkriptManager.getMultiplier(player) * 10);
    }

    private boolean isOreType(Material material) {
        return OreType.fromMaterial(material) != null;
    }

    private void sendFakeBlockChange(Player player, Block block, BlockData blockData) {
        PacketContainer packet = new PacketContainer(PacketType.Play.Server.BLOCK_CHANGE);
        packet.getBlockPositionModifier().write(0, new BlockPosition(block.getX(), block.getY(), block.getZ()));
        packet.getBlockData().write(0, WrappedBlockData.createData(blockData));
        protocolManager.sendServerPacket(player, packet, false);
    }

    public void openOreSelectionGUI(Player player) {
        List<OreType> ores = getSortedOres();
        int oresPerPage = 28;
        int totalPages = (int) Math.ceil((double) ores.size() / oresPerPage);

        setTotalPages(player, Math.max(totalPages, 1)); // Ensure at least 1 page
        setCurrentPage(player, 1); // Start on page 1

        openOrePage(player, ores, 1); // Open the first page
    }

    private void openOrePage(Player player, List<OreType> ores, int page) {
        int oresPerPage = 28;
        int totalPages = (int) Math.ceil((double) ores.size() / oresPerPage); // Dynamically calculate total pages


        int startIndex = (page - 1) * oresPerPage; // Convert to 0-based index
        int endIndex = Math.min(startIndex + oresPerPage, ores.size());

        Inventory gui = Bukkit.createInventory(null, 54, "Select a Ore - Page " + page + " of " + totalPages);

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

        // Add ore to GUI
        int[] oreSlots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34,
                37, 38, 39, 40, 41, 42, 43
        };

        for (int i = startIndex, slotIndex = 0; i < endIndex && slotIndex < oreSlots.length; i++, slotIndex++) {
            OreType ore = ores.get(i);
            gui.setItem(oreSlots[slotIndex], ore.toItemStack(PrestigeManager.getPlayerPrestige(player)));
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


    private List<OreType> getSortedOres() {
        List<OreType> ores = new ArrayList<>(OreType.getAllOres());
        ores.sort(Comparator.comparingInt(OreType::getRequiredPrestige));
        return ores;
    }
}
