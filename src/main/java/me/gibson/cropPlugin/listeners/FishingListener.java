package me.gibson.cropPlugin.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import me.gibson.cropPlugin.FarmTycoonPlugin;
import me.gibson.cropPlugin.managers.PrestigeManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Silverfish;
import org.bukkit.entity.Player;
import org.bukkit.entity.TropicalFish;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class FishingListener implements Listener {
    private final FarmTycoonPlugin plugin;
    private final Random random;
    private final Map<Player, Long> cooldowns; // Cooldown map to track last fishing time

    public FishingListener(FarmTycoonPlugin plugin) {
        this.plugin = plugin;
        this.random = new Random();
        this.cooldowns = new HashMap<>();
    }

    public int prestige;
    public double multiplier;
    public int amount;

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();

        if (!player.getWorld().getName().contains("FishWorld")) {
            player.sendMessage(ChatColor.RED + "You can only fish at /fish!");
            return;
        }

        // Allow REEL_IN state to let players stop fishing
        if (event.getState() == PlayerFishEvent.State.REEL_IN) {
            return;
        }

        // Cancel all states except FISHING and REEL_IN
        if (event.getState() != PlayerFishEvent.State.FISHING) {
            event.setCancelled(true);
            return;
        }

        // Check for cooldown
        long currentTime = System.currentTimeMillis();
        long lastFishingTime = cooldowns.getOrDefault(player, 0L);

        if (currentTime - lastFishingTime < 5000) { // 5-second cooldown
            player.sendMessage(ChatColor.RED + "You must wait before casting again!");
            event.setCancelled(true);
            return;
        }

        cooldowns.put(player, currentTime); // Update the last fishing time

        // Add a delay before the first TropicalFish spawn
        new BukkitRunnable() {
            private boolean initialDelay = true;

            @Override
            public void run() {
                // Ensure the bobber is valid
                if (!event.getHook().isValid()) {
                    cancel();
                    return;
                }

                // Add a delay to avoid spawning TropicalFish at the player's head
                if (initialDelay) {
                    initialDelay = false;
                    return;
                }

                Location bobberLocation = event.getHook().getLocation();
                Location spawnLocation = calculateSpawnLocation(bobberLocation, player);

                int prestige = PrestigeManager.getPlayerPrestige(player);
                double multiplier = plugin.getConfig().getDouble("prestige.multiplier", 0.25);

                // Generate a new amount for this fish
                int amount = (int) (500 * multiplier * prestige * (0.5 + random.nextDouble())); // Randomized multiplier

                TropicalFish fish = spawnLocation.getWorld().spawn(spawnLocation, TropicalFish.class);
                fish.setPersistent(false);
                fish.setRemoveWhenFarAway(true);
                fish.setCustomName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + amount + ChatColor.AQUA + " " + ChatColor.BOLD + "Dust");
                fish.setCustomNameVisible(true);
                fish.setInvulnerable(true);
                disableAI(fish);

                setEntityFacing(fish, bobberLocation);

                hideEntityFromOthers(player, fish);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!fish.isValid() || !event.getHook().isValid()) {
                            fish.remove();
                            cancel();
                            return;
                        }

                        Location currentLocation = fish.getLocation();
                        Vector direction = bobberLocation.toVector().subtract(currentLocation.toVector()).normalize();
                        currentLocation.add(direction.multiply(0.2));

                        fish.teleport(currentLocation);

                        if (currentLocation.distance(bobberLocation) <= 2) {
                            fish.remove();
                            awardPlayer(player, amount);
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, 0L, 5L);
            }
        }.runTaskTimer(plugin, 40L, random.nextInt(120) + 120); // Initial delay of 2 seconds (40 ticks)
    }

    private Location calculateSpawnLocation(Location bobberLocation, Player player) {
        Location playerLocation = player.getLocation();
        Vector playerDirection = playerLocation.getDirection();

        double randomX = 5 + random.nextDouble() * 5;
        double randomZ = 5 + random.nextDouble() * 5;

        if (random.nextBoolean()) randomX *= -1;
        if (random.nextBoolean()) randomZ *= -1;

        Location spawnLocation = bobberLocation.clone().add(randomX, 0, randomZ);

        // Ensure Silverfish spawns in front of the player
        Vector toSpawn = spawnLocation.toVector().subtract(playerLocation.toVector()).normalize();
        if (toSpawn.dot(playerDirection) < 0) {
            spawnLocation = bobberLocation.clone().add(-randomX, 0, -randomZ);
        }

        spawnLocation.setY(bobberLocation.getY()); // Align to the bobber's Y level
        return spawnLocation;
    }

    private void disableAI(TropicalFish silverfish) {
        silverfish.setAI(false); // Disable default AI
    }

    private void setEntityFacing(TropicalFish silverfish, Location target) {
        Location currentLocation = silverfish.getLocation();
        Vector direction = target.toVector().subtract(currentLocation.toVector()).normalize();
        currentLocation.setDirection(direction);
        silverfish.teleport(currentLocation);
    }

    private void hideEntityFromOthers(Player player, TropicalFish silverfish) {
        int entityId = silverfish.getEntityId();

        Bukkit.getOnlinePlayers().forEach(p -> {
            if (!p.equals(player)) {
                try {
                    PacketContainer destroyPacket = ProtocolLibrary.getProtocolManager()
                            .createPacket(PacketType.Play.Server.ENTITY_DESTROY);
                    destroyPacket.getIntLists().write(0, Collections.singletonList(entityId));
                    ProtocolLibrary.getProtocolManager().sendServerPacket(p, destroyPacket);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void awardPlayer(Player player, int amount) {
        String command = String.format("mysterydust add %s %d", player.getName(), amount);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
