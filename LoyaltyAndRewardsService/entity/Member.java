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
    private int totalExpenses;

    public Member(String memberId, String name, String passport, String phoneNumber,
            int point, int totalExpenses) {
        this.memberId = memberId;
        this.name = name;
        this.passport = passport;
        this.phoneNumber = phoneNumber;
        this.point = point;
        this.totalExpenses = totalExpenses;
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

    public int getTotalExpenses() {
        return totalExpenses;
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

    public void setTotalExpenses(int totalExpenses) {
        this.totalExpenses = Math.max(totalExpenses, 0);
    }

    public void addTotalExpenses(int expenseAmount) {
        if (expenseAmount > 0) {
            totalExpenses += expenseAmount;
        }
    }

    @Override
    public int compareTo(Member other) {
        return Integer.compare(point, other.point);
    }

    public String toCsvLine(){
        return memberId + "," + name + "," + passport + "," + phoneNumber + ","
                + point + "," + totalExpenses;
    }
    
}
