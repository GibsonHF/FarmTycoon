package me.gibson.cropPlugin.types;

public enum EnchantmentType {
    CROP_FORTUNE("Crop Fortune", 5, 100000, 1.2),
    TOKEN_FINDER("Token Finder", 10, 50000, 1.1),
    EXP_FINDER("Exp Finder", 10, 75000, 1.15),
    REGEN("Regrow", 5, 200000, 1.3);

    private final String displayName;
    private final int maxLevel;
    private final double baseCost;
    private final double costMultiplier;

    EnchantmentType(String displayName, int maxLevel, double baseCost, double costMultiplier) {
        this.displayName = displayName;
        this.maxLevel = maxLevel;
        this.baseCost = baseCost;
        this.costMultiplier = costMultiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public double getBaseCost() {
        return baseCost;
    }

    public double getCostMultiplier() {
        return costMultiplier;
    }

    public double calculateCost(int currentLevel) {
        return baseCost * Math.pow(costMultiplier, currentLevel);
    }
}
