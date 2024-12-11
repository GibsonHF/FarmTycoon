package me.gibson.cropPlugin.GUI;

import ch.njol.skript.util.chat.ChatCode;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import me.gibson.cropPlugin.managers.SkriptManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.UUID;

public class ConfirmPayGUI {

    public static void openConfirmPayGUI(Player player, Player targetPlayer, int amount) {
        Inventory confirmGUI = Bukkit.createInventory(null, 27, ChatColor.DARK_GREEN + "Confirm Transaction");

        int playerGems = SkriptManager.getGems(player);
        int balanceAfterTransaction = playerGems - amount;

        // CONFIRM button
        ItemStack confirmItem = getSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDk5ODBjMWQyMTE4MDlhOWI2NTY1MDg4ZjU2YTM4ZjJlZjQ5MTE1YzEwNTRmYTY2MjQ1MTIyZTllZWVkZWNjMiJ9fX0=");
        ItemMeta confirmMeta = confirmItem.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Confirm Transaction");
        confirmMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Click to purchase the following:",
                ChatColor.WHITE + "Payment for " + ChatColor.WHITE + targetPlayer.getName(),
                "",
                ChatColor.DARK_GREEN + " * " + ChatColor.GREEN +"Amount: " + ChatColor.RED + "-" + amount + " Gems",
                ChatColor.DARK_GREEN + " * " + ChatColor.GREEN + "Balance: " + ChatColor.WHITE + balanceAfterTransaction + ChatColor.GRAY + " (After Transaction)",
                "",
                ChatColor.GREEN + "" + ChatColor.BOLD + "CLICK HERE",
                ChatColor.DARK_GREEN + "Click this to purchase!"
        ));
        confirmItem.setItemMeta(confirmMeta);

        // CANCEL button
        ItemStack cancelItem = getSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmViNTg4YjIxYTZmOThhZDFmZjRlMDg1YzU1MmRjYjA1MGVmYzljYWI0MjdmNDYwNDhmMThmYzgwMzQ3NWY3In19fQ==");
        ItemMeta cancelMeta = cancelItem.getItemMeta();
        cancelMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Decline Transaction");
        cancelMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Cancel this action and do nothing",
                "",
                ChatColor.RED + "" + ChatColor.BOLD + "CLICK HERE",
                ChatColor.GRAY + "Click this to cancel purchase!"
        ));
        cancelItem.setItemMeta(cancelMeta);

        // WARNING item
        ItemStack warningItem = getSkull("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMzA3ZjQ3OTU4NGZjY2FlNjg2MDAzYTYwODAwZGRmZWU3MmFmZmUxMGU0YmIyNmE3ZDRhMDBjY2I5OTc5N2QyIn19fQ==");
        ItemMeta warningMeta = warningItem.getItemMeta();
        warningMeta.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Are You Sure?");
        warningMeta.setLore(Arrays.asList(
                ChatColor.GRAY + "Click the green button to purchase",
                ChatColor.GRAY + "or the red button to cancel transaction!"
        ));
        warningItem.setItemMeta(warningMeta);

        // Add items to the GUI
        confirmGUI.setItem(11, confirmItem);
        confirmGUI.setItem(13, warningItem);
        confirmGUI.setItem(15, cancelItem);

        // Filler items for the rest of the GUI
        ItemStack fillerItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
        ItemMeta fillerMeta = fillerItem.getItemMeta();
        fillerMeta.setDisplayName(ChatColor.BLACK + "");
        fillerItem.setItemMeta(fillerMeta);

        // Set filler for empty slots
        for (int i = 0; i < confirmGUI.getSize(); i++) {
            if (confirmGUI.getItem(i) == null) {
                confirmGUI.setItem(i, fillerItem);
            }
        }

        // Open the GUI for the player
        player.openInventory(confirmGUI);
    }

    // Utility method to create custom skull items with a specific texture
    public static ItemStack getSkull(String textures) {
        final ItemStack head = new ItemStack(Material.PLAYER_HEAD);

        head.editMeta(SkullMeta.class, skullMeta -> {
            final UUID uuid = UUID.randomUUID();
            final PlayerProfile playerProfile = Bukkit.createProfile(uuid, uuid.toString().substring(0, 16));
            playerProfile.setProperty(new ProfileProperty("textures", textures));

            skullMeta.setPlayerProfile(playerProfile);
        });
        return head;
    }
}
