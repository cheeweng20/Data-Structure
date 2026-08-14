package WalkInRegistrationAndReservation.entity;

public enum LoyaltyTier {
    DIAMOND(4),
    PLATINUM(3),
    ELITE(2),
    GOLD(1),
    STANDARD(0);

    private final int priorityScore;

    LoyaltyTier(int priorityScore) {
        this.priorityScore = priorityScore;
    }

    public int getPriorityScore() {
        return priorityScore;
    }
}
