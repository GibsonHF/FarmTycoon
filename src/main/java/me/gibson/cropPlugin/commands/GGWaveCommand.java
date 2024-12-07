package me.gibson.cropPlugin.commands;

import me.gibson.cropPlugin.managers.SkriptManager;
import me.gibson.cropPlugin.utils.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class GGWaveCommand implements CommandExecutor, Listener {

    private final JavaPlugin plugin;
    private final Set<String> playersWhoSaidGG = new HashSet<>();
    private boolean waveActive = false;

    public GGWaveCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ggwave.start")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to start a GG Wave.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /ggwave <playername>");
            return true;
        }

        String playerName = args[0];
        broadcastGGWave(playerName);

        return true;
    }

    private void broadcastGGWave(String playerName) {
        if (waveActive) {
            Bukkit.broadcastMessage(ChatColor.RED + "A GG Wave is already active!");
            return;
        }

        waveActive = true;
        playersWhoSaidGG.clear();

        Bukkit.broadcastMessage(ColorUtil.color("&b[CraftNova] &e" + playerName + "&b has made a purchase on our store!"));
        Bukkit.broadcastMessage(ColorUtil.color("&a&lstore.craftnova.com &6- &d&lTHANK YOU"));
        Bukkit.broadcastMessage("");
        Bukkit.broadcastMessage(ColorUtil.color("&e&lShow some love by typing &a&l'gg' &e&lin chat!"));

        new BukkitRunnable() {
            @Override
            public void run() {
                endGGWave();
            }
        }.runTaskLater(plugin, 100L); // Wave ends after 5 seconds (100 ticks)
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!waveActive) return;

        Player player = event.getPlayer();
        String message = event.getMessage().toLowerCase();

        // Check if the player says "gg"
        if (message.equals("gg")) {
            event.setCancelled(true);

            // Prevent duplicate GG messages
            if (playersWhoSaidGG.contains(player.getName())) {
                player.sendMessage(ColorUtil.color("&cYou have already participated in the GG Wave!"));
                return;
            }

            // Add the player to the list of participants
            playersWhoSaidGG.add(player.getName());

            // Generate gradients for the player name and "GG"
            String nameGradient = applyGradient(player.getName(), getRandomHexCode(), getRandomHexCode());
            String ggGradient = applyGradient("GG", getRandomHexCode(), getRandomHexCode());

            // Combine gradients into the final bold message
            String broadcastMessage = ColorUtil.color("&6&l> ") + nameGradient + ColorUtil.color("&f&l: ") + ggGradient;

            // Broadcast the message
            Bukkit.broadcastMessage(broadcastMessage);
        }
    }

    private void endGGWave() {
        waveActive = false;

        if (playersWhoSaidGG.isEmpty()) {
            Bukkit.broadcastMessage(ColorUtil.color("&cNo one said 'gg' during the GG Wave!"));
        } else {
            Bukkit.broadcastMessage(ColorUtil.color("&a&lThe GG Wave has ended! Thanks for spreading positivity!"));
            for (String playerName : playersWhoSaidGG) {
                Player player = Bukkit.getPlayer(playerName);
                if (player != null && player.isOnline()) {
                    SkriptManager.addGems(player, 1);
                    player.sendMessage(ColorUtil.color("&6You have been awarded 1 gem for participating in the GG Wave!"));
                }
            }
        }

        playersWhoSaidGG.clear();
    }

    private String applyGradient(String text, String startColor, String endColor) {
        StringBuilder result = new StringBuilder();
        int length = text.length();

        for (int i = 0; i < length; i++) {
            float ratio = (float) i / (length - 1);
            String blendedColor = interpolateHexColor(startColor, endColor, ratio);
            result.append("&#").append(blendedColor).append("&l").append(text.charAt(i));
        }

        return ColorUtil.color(result.toString());
    }

    private String interpolateHexColor(String startColor, String endColor, float ratio) {
        int startRed = Integer.parseInt(startColor.substring(0, 2), 16);
        int startGreen = Integer.parseInt(startColor.substring(2, 4), 16);
        int startBlue = Integer.parseInt(startColor.substring(4, 6), 16);

        int endRed = Integer.parseInt(endColor.substring(0, 2), 16);
        int endGreen = Integer.parseInt(endColor.substring(2, 4), 16);
        int endBlue = Integer.parseInt(endColor.substring(4, 6), 16);

        int red = (int) (startRed + (endRed - startRed) * ratio);
        int green = (int) (startGreen + (endGreen - startGreen) * ratio);
        int blue = (int) (startBlue + (endBlue - startBlue) * ratio);

        return String.format("%02X%02X%02X", red, green, blue);
    }

    private String getRandomHexCode() {
        int red = (int) (Math.random() * 256);
        int green = (int) (Math.random() * 256);
        int blue = (int) (Math.random() * 256);
        return String.format("%02X%02X%02X", red, green, blue);
    }
}
