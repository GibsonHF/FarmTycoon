package me.gibson.cropPlugin.utils;

import java.util.HashMap;
import java.util.Map;

public class PrestigeRequirement {

    private static final Map<Integer, PrestigeRequirement> requirements = new HashMap<>();

    static {
        // Base values for the first prestige
        int baseLevel = 40;           // Starting level for Prestige 1
        int baseTokens = 500000;      // Tokens required for Prestige 1
        double baseMoney = 1000000.0; // Money required for Prestige 1

        // Scaling factors
        int levelIncrement = 5;      // Increase level by 5 per prestige
        double tokenScale = 0.25;    // Increase tokens by 25% per prestige
        double moneyScale = 0.25;    // Increase money by 25% per prestige

        // Generate requirements for prestiges 1 to 400
        for (int i = 1; i <= 400; i++) {
            // Calculate requirements
            int requiredLevel = baseLevel + (i - 1) * levelIncrement; // Level increases by 5 per prestige
            int requiredTokens = (int) (baseTokens + (baseTokens * (i - 1) * tokenScale)); // Tokens scale by 25% per prestige
            double requiredMoney = baseMoney + (baseMoney * (i - 1) * moneyScale); // Money scales by 25% per prestige

            // Add to prestige requirements map
            requirements.put(i, new PrestigeRequirement(requiredLevel, requiredTokens, requiredMoney));
        }
    }

    private final int requiredLevel;
    private final int requiredTokens;
    private final double requiredMoney;

    public PrestigeRequirement(int requiredLevel, int requiredTokens, double requiredMoney) {
        this.requiredLevel = requiredLevel;
        this.requiredTokens = requiredTokens;
        this.requiredMoney = requiredMoney;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public int getRequiredTokens() {
        return requiredTokens;
    }

    public double getRequiredMoney() {
        return requiredMoney;
    }

    public static PrestigeRequirement getRequirement(int prestigeLevel) {
        return requirements.get(prestigeLevel);
    }
}
