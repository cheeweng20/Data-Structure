package LoyaltyAndRewardsService;

public class Member {
    private String memberId;
    private String name;
    private int point;
    private String tierId;

    public Member(String memberId,String name, int point,String tierId) {
        this.memberId = memberId;
        this.name = name;
        this.point = point;
        this.tierId = tierId;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getName() {
        return name;
    }

    public int getPoint() {
        return point;
    }

    public String getTierId() {
        return tierId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPoint(int point) {
        this.point = point;
    }

    public void setTierId(String tierId) {
        this.tierId = tierId;
    }

    public String toCsvLine(){
        return memberId + "," + name + "," + point + "," + tierId;
    }
    
}
