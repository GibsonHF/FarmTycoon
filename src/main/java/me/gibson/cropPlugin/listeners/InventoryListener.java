package me.gibson.cropPlugin.listeners;

import me.gibson.cropPlugin.managers.SkriptManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class InventoryListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!event.getView().getTitle().equals(ChatColor.DARK_GREEN + "Confirm Transaction")) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() != Material.PLAYER_HEAD) return;

        Player player = (Player) event.getWhoClicked();
        String displayName = clickedItem.getItemMeta().getDisplayName();

        if (displayName.equals(ChatColor.GREEN + "" + ChatColor.BOLD + "Confirm Transaction")) {
            // Confirm payment
            List<String> lore = clickedItem.getItemMeta().getLore();
            if (lore == null || lore.size() < 4) {
                player.sendMessage(ChatColor.RED + "Invalid transaction details.");
                player.closeInventory();
                return;
            }

            String amountLine = ChatColor.stripColor(lore.get(3));
            String balanceLine = ChatColor.stripColor(lore.get(4));

            // Extract amount from lore
            int amount;
            try {
                amount = Integer.parseInt(amountLine.split(":")[1].trim().split(" ")[0].replace("-", ""));
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Invalid amount format.");
                player.closeInventory();
                return;
            }

            // Extract target player from lore
            String targetPlayerName = ChatColor.stripColor(lore.get(1)).replace("Payment for ", "").trim();
            Player targetPlayer = Bukkit.getPlayer(targetPlayerName);

            if (targetPlayer == null) {
                player.sendMessage(ChatColor.RED + "Player " + targetPlayerName + " not found.");
                player.closeInventory();
                return;
            }

            int payerGems = SkriptManager.getGems(player);
            if (payerGems >= amount) {
                SkriptManager.addGems(targetPlayer, amount);
                SkriptManager.setGems(player, payerGems - amount);

                player.sendMessage(ChatColor.GREEN + "You have successfully paid " + ChatColor.GOLD + amount + ChatColor.GREEN + " gems to " + targetPlayer.getName() + ".");
                targetPlayer.sendMessage(ChatColor.GREEN + "You have received " + ChatColor.GOLD + amount + ChatColor.GREEN + " gems from " + player.getName() + ".");
            } else {
                player.sendMessage(ChatColor.RED + "You do not have enough gems to complete the transaction.");
            }
        } else if (displayName.equals(ChatColor.RED + "" + ChatColor.BOLD + "Decline Transaction")) {
            // Cancel payment
            player.sendMessage(ChatColor.RED + "Payment canceled.");
        }

        player.closeInventory();
    }
}
