package me.gibson.cropPlugin.utils;

import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final char COLOR_CHAR = ChatColor.COLOR_CHAR;

    // Converts text with &#RRGGBB hex color codes and & formatting codes
    public static String color(String s) {
        String message = s;

        // First translate hexadecimal color codes
        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);
        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, COLOR_CHAR + "x"
                    + COLOR_CHAR + group.charAt(0) + COLOR_CHAR + group.charAt(1)
                    + COLOR_CHAR + group.charAt(2) + COLOR_CHAR + group.charAt(3)
                    + COLOR_CHAR + group.charAt(4) + COLOR_CHAR + group.charAt(5)
            );
        }
        message = matcher.appendTail(buffer).toString();

        // Then translate traditional color and formatting codes
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // Converts only standard & color codes
    public static String normalColor(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }
}
