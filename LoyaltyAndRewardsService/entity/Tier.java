package LoyaltyAndRewardsService.entity;

/**
 * Stores one loyalty tier and the four available tier definitions.
 *
 * @author Chee Weng
 */
public final class Tier {
    public static final Tier CLASSIC = new Tier("T003", "Classic", 0, 199, 0);
    public static final Tier SILVER = new Tier("T002", "Silver", 200, 499, 1);
    public static final Tier GOLD = new Tier("T001", "Gold", 500, 700, 2);
    public static final Tier PLATINUM = new Tier("T004", "Platinum", 701, 0, 3);

    private static final Tier[] TIERS = {CLASSIC, SILVER, GOLD, PLATINUM};

    private final String tierId;
    private final String tierLevel;
    private final int minPoint;
    private final int maxPoint;
    private final int priorityScore;

    private Tier(String tierId, String tierLevel, int minPoint, int maxPoint,
            int priorityScore) {
        this.tierId = tierId;
        this.tierLevel = tierLevel;
        this.minPoint = minPoint;
        this.maxPoint = maxPoint;
        this.priorityScore = priorityScore;
    }

    public String getTierId() {
        return tierId;
    }

    public String getTierLevel() {
        return tierLevel;
    }

    public int getMinPoint() {
        return minPoint;
    }

    public int getMaxPoint() {
        return maxPoint;
    }

    public int getPriorityScore() {
        return priorityScore;
    }

    public Tier getNextTier() {
        return priorityScore < TIERS.length - 1
                ? TIERS[priorityScore + 1] : null;
    }

    public static Tier fromPoints(int totalExpenses) {
        if (totalExpenses >= PLATINUM.minPoint) {
            return PLATINUM;
        }
        if (totalExpenses >= GOLD.minPoint) {
            return GOLD;
        }
        if (totalExpenses >= SILVER.minPoint) {
            return SILVER;
        }
        return CLASSIC;
    }

    public static Tier fromTierName(String tierName) {
        if (tierName == null || tierName.isBlank()) {
            return CLASSIC;
        }

        for (Tier tier : TIERS) {
            if (tier.tierLevel.equalsIgnoreCase(tierName.trim())) {
                return tier;
            }
        }
        return CLASSIC;
    }

    public static Tier[] getTiers() {
        return TIERS.clone();
    }

    @Override
    public String toString() {
        return tierLevel.toUpperCase();
    }
}
