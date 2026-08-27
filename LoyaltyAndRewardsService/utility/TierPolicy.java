package LoyaltyAndRewardsService.utility;

/** Fixed loyalty-tier thresholds shared by loyalty and reservation modules. */
public final class TierPolicy {
    public static final String CLASSIC_ID = "T003";
    public static final String SILVER_ID = "T002";
    public static final String GOLD_ID = "T001";
    public static final String PLATINUM_ID = "T004";

    public static final int SILVER_MINIMUM = 200;
    public static final int GOLD_MINIMUM = 500;
    public static final int PLATINUM_MINIMUM = 701;

    private static final String[] TIER_IDS = {
        CLASSIC_ID, SILVER_ID, GOLD_ID, PLATINUM_ID
    };

    private TierPolicy() {
    }

    public static String getTierId(int lifetimePointsEarned) {
        if (lifetimePointsEarned >= PLATINUM_MINIMUM) {
            return PLATINUM_ID;
        }
        if (lifetimePointsEarned >= GOLD_MINIMUM) {
            return GOLD_ID;
        }
        if (lifetimePointsEarned >= SILVER_MINIMUM) {
            return SILVER_ID;
        }
        return CLASSIC_ID;
    }

    public static String getTierName(int lifetimePointsEarned) {
        return getTierNameById(getTierId(lifetimePointsEarned));
    }

    public static String getTierNameById(String tierId) {
        if (PLATINUM_ID.equalsIgnoreCase(tierId)) {
            return "Platinum";
        }
        if (GOLD_ID.equalsIgnoreCase(tierId)) {
            return "Gold";
        }
        if (SILVER_ID.equalsIgnoreCase(tierId)) {
            return "Silver";
        }
        return "Classic";
    }

    public static int getMinimumPoints(String tierId) {
        if (PLATINUM_ID.equalsIgnoreCase(tierId)) {
            return PLATINUM_MINIMUM;
        }
        if (GOLD_ID.equalsIgnoreCase(tierId)) {
            return GOLD_MINIMUM;
        }
        if (SILVER_ID.equalsIgnoreCase(tierId)) {
            return SILVER_MINIMUM;
        }
        return 0;
    }

    public static int getMaximumPoints(String tierId) {
        if (CLASSIC_ID.equalsIgnoreCase(tierId)) {
            return SILVER_MINIMUM - 1;
        }
        if (SILVER_ID.equalsIgnoreCase(tierId)) {
            return GOLD_MINIMUM - 1;
        }
        if (GOLD_ID.equalsIgnoreCase(tierId)) {
            return PLATINUM_MINIMUM - 1;
        }
        return 0;
    }

    public static int getNextTierMinimum(int lifetimePointsEarned) {
        if (lifetimePointsEarned < SILVER_MINIMUM) {
            return SILVER_MINIMUM;
        }
        if (lifetimePointsEarned < GOLD_MINIMUM) {
            return GOLD_MINIMUM;
        }
        if (lifetimePointsEarned < PLATINUM_MINIMUM) {
            return PLATINUM_MINIMUM;
        }
        return -1;
    }

    public static String getNextTierName(int lifetimePointsEarned) {
        if (lifetimePointsEarned < SILVER_MINIMUM) {
            return "Silver";
        }
        if (lifetimePointsEarned < GOLD_MINIMUM) {
            return "Gold";
        }
        if (lifetimePointsEarned < PLATINUM_MINIMUM) {
            return "Platinum";
        }
        return null;
    }

    public static String[] getTierIds() {
        return TIER_IDS.clone();
    }
}
