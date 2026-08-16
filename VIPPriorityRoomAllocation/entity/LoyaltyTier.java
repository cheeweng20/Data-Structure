package VIPPriorityRoomAllocation.entity;

public enum LoyaltyTier {
    PLATINUM(3),
    GOLD(2),
    SILVER(1),
    CLASSIC(0);

    private final int priorityScore;

    LoyaltyTier(int priorityScore) {
        this.priorityScore = priorityScore;
    }

    public int getPriorityScore() {
        return priorityScore;
    }

    public static LoyaltyTier fromTierName(String tierName) {
        if (tierName == null || tierName.trim().isEmpty()) {
            return CLASSIC;
        }

        try {
            return LoyaltyTier.valueOf(tierName.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return CLASSIC;
        }
    }
}
