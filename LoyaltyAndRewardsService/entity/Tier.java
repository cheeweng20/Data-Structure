package LoyaltyAndRewardsService.entity;


public class Tier {
    private String tierId;
    private String tierLevel;
    private int minPoint;
    private int maxPoint;

    public Tier(){
        this.tierId = "";
        this.tierLevel = "";
        this.maxPoint = 0;
        this.minPoint = 0;
    }

    public Tier(String tierId,String tierLevel,int minPoint,int maxPoint){
        this.tierId = tierId;
        this.tierLevel = tierLevel;
        this.minPoint = minPoint;
        this.maxPoint = maxPoint;
    }

    public String getTierId() {
        return tierId;
    }

    public String getTierLevel() {
        return tierLevel;
    }

    public int getMaxPoint() {
        return maxPoint;
    }

    public int getMinPoint() {
        return minPoint;
    }

    public void setTierId(String tierId) {
        this.tierId = tierId;
    }

    public void setTierLevel(String tierLevel) {
        this.tierLevel = tierLevel;
    }

    public void setMaxPoint(int maxPoint) {
        this.maxPoint = maxPoint;
    }

    public void setMinPoint(int minPoint) {
        this.minPoint = minPoint;
    }

    public String toCsvLine(){
        return tierId + "," + tierLevel + "," + minPoint + "," + maxPoint;
    }
}
