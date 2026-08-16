package LoyaltyAndRewardsService.entity;

/**
 * @author Chee Weng
 */
public class Member implements Comparable<Member> {
    private String memberId;
    private String name;
    private String passport;
    private String phoneNumber;
    private int point;
    private String tierId;
    private String lastNotifiedTierId;

    public Member(String memberId,String name, int point,String tierId) {
        this(memberId, name, "", "", point, tierId, tierId);
    }

    public Member(String memberId, String name, int point, String tierId,
            String lastNotifiedTierId) {
        this(memberId, name, "", "", point, tierId, lastNotifiedTierId);
    }

    public Member(String memberId, String name, String passport, String phoneNumber,
            int point, String tierId) {
        this(memberId, name, passport, phoneNumber, point, tierId, tierId);
    }

    public Member(String memberId, String name, String passport, String phoneNumber,
            int point, String tierId, String lastNotifiedTierId) {
        this.memberId = memberId;
        this.name = name;
        this.passport = passport;
        this.phoneNumber = phoneNumber;
        this.point = point;
        this.tierId = tierId;
        this.lastNotifiedTierId = lastNotifiedTierId;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getName() {
        return name;
    }

    public String getPassport() {
        return passport;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public int getPoint() {
        return point;
    }

    public String getTierId() {
        return tierId;
    }

    public String getLastNotifiedTierId() {
        return lastNotifiedTierId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPassport(String passport) {
        this.passport = passport;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setPoint(int point) {
        this.point = point;
    }

    public void setTierId(String tierId) {
        this.tierId = tierId;
    }

    public void setLastNotifiedTierId(String lastNotifiedTierId) {
        this.lastNotifiedTierId = lastNotifiedTierId;
    }

    @Override
    public int compareTo(Member other) {
        return Integer.compare(point, other.point);
    }

    public String toCsvLine(){
        return memberId + "," + name + "," + passport + "," + phoneNumber + ","
                + point + "," + tierId + ","
                + lastNotifiedTierId;
    }
    
}
