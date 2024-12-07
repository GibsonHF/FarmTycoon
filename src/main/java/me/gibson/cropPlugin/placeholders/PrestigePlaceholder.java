package me.gibson.cropPlugin.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.gibson.cropPlugin.managers.PrestigeManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PrestigePlaceholder extends PlaceholderExpansion {

    private final PrestigeManager prestigeManager;

    public PrestigePlaceholder(PrestigeManager prestigeManager) {
        this.prestigeManager = prestigeManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "prestige";
    }

    @Override
    public @NotNull String getAuthor() {
        return "GibsonHF";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        if (identifier.equals("level")) {
            return String.valueOf(PrestigeManager.getPlayerPrestige(player));
        }

        return null;
    }
}