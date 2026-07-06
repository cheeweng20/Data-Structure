public class Member {
    private String memberId;
    private String name;
    private int point;

    public Member(String memberId,String name, int point) {
        this.memberId = memberId;
        this.name = name;
        this.point = point;
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

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPoint(int point) {
        this.point = point;
    }
}
