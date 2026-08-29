package LoyaltyAndRewardsService.entity;

/**
 * @author Chee Weng
 */
public class PromotionOffer {
    private final int completedStayCount;
    private final int weekendStayCount;
    private final double pointMultiplier;

    public PromotionOffer(int completedStayCount, int weekendStayCount,
            double pointMultiplier) {
        this.completedStayCount = completedStayCount;
        this.weekendStayCount = weekendStayCount;
        this.pointMultiplier = pointMultiplier;
    }

    public int getCompletedStayCount() {
        return completedStayCount;
    }

    public int getWeekendStayCount() {
        return weekendStayCount;
    }

    public double getPointMultiplier() {
        return pointMultiplier;
    }
}
