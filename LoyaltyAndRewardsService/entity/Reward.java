package LoyaltyAndRewardsService.entity;

/**
 * A reward that a member may redeem using loyalty points.
 */
public class Reward {
    private String rewardId;
    private String rewardName;
    private int pointRequired;

    public Reward(String rewardId, String rewardName, int pointRequired) {
        this.rewardId = rewardId;
        this.rewardName = rewardName;
        this.pointRequired = pointRequired;
    }

    public String getRewardId() {
        return rewardId;
    }

    public String getRewardName() {
        return rewardName;
    }

    public int getPointRequired() {
        return pointRequired;
    }

    public void setRewardId(String rewardId) {
        this.rewardId = rewardId;
    }

    public void setRewardName(String rewardName) {
        this.rewardName = rewardName;
    }

    public void setPointRequired(int pointRequired) {
        this.pointRequired = pointRequired;
    }

    public String toCsvLine() {
        return rewardId + "," + rewardName + "," + pointRequired;
    }
}
