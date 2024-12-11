package me.gibson.cropPlugin.utils;

import java.util.HashMap;
import java.util.Map;

public class PrestigeRequirement {

    private static final Map<Integer, PrestigeRequirement> requirements = new HashMap<>();

    static {
        // Base values for the first prestige
        int baseLevel = 40;           // Starting level for Prestige 1
        double baseTokens = 100000.0; // Tokens required for Prestige 1
        double baseMoney = 100000.0;  // Money required for Prestige 1

        // Growth factors based on observed values
        double tokenGrowth = Math.pow(18_640_000_000.0 / 100000.0, 1.0 / 220.0); // Growth factor for tokens
        double moneyGrowth = Math.pow(474_140_000_000.0 / 100000.0, 1.0 / 220.0); // Growth factor for money

        int levelIncrement = 5;       // Level increment per prestige

        // Caps for tokens and money
        double maxTokens = 50_000_000_000_000.0; // 50T tokens
        double maxMoney = 400_000_000_000_000.0; // 400T money

        // Generate requirements for prestiges 1 to 1500
        for (int i = 0; i <= 1500; i++) {
            // Calculate required level
            int requiredLevel = baseLevel + (i - 1) * levelIncrement;

            // Apply growth rates exponentially
            double requiredTokens = baseTokens * Math.pow(tokenGrowth, i);
            double requiredMoney = baseMoney * Math.pow(moneyGrowth, i);

            // Apply caps if limits are exceeded
            if (requiredTokens > maxTokens) {
                requiredTokens = maxTokens;
            }
            if (requiredMoney > maxMoney) {
                requiredMoney = maxMoney;
            }

            // Add to prestige requirements map
            requirements.put(i, new PrestigeRequirement(requiredLevel, requiredTokens, requiredMoney));
        }
    }




    private final int requiredLevel;
    private final double requiredTokens;
    private final double requiredMoney;

    public PrestigeRequirement(int requiredLevel, double requiredTokens, double requiredMoney) {
        this.requiredLevel = requiredLevel;
        this.requiredTokens = requiredTokens;
        this.requiredMoney = requiredMoney;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public double getRequiredTokens() {
        return requiredTokens;
    }

    public double getRequiredMoney() {
        return requiredMoney;
    }

    public static PrestigeRequirement getRequirement(int prestigeLevel) {
        return requirements.get(prestigeLevel);
    }
}
