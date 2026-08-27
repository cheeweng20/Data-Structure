package common.domain.loyalty;

public enum LoyaltyTier {
    PLATINUM(3), // highest priority in the heap
    GOLD(2),
    SILVER(1),
    CLASSIC(0); // default tier for non-members or unknown loyalty records

    private final int priorityScore; // higher score means higher room allocation priority

    LoyaltyTier(int priorityScore) {
        this.priorityScore = priorityScore;
    }

    public int getPriorityScore() {
        return priorityScore; // used by Reservation.compareTo()
    }

    public static LoyaltyTier fromTierName(String tierName) {
        if (tierName == null || tierName.trim().isEmpty()) {
            return CLASSIC; // empty tier :normal guest
        }

        try {
            return LoyaltyTier.valueOf(tierName.trim().toUpperCase()); // convert CSV text to enum
        } catch (IllegalArgumentException ex) {
            return CLASSIC; // unknown tier name :normal guest
        }
    }
}
