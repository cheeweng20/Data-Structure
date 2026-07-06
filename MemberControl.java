import adt.LinkedList;

public class MemberControl {
    private LinkedList<Member> memberList;

    public MemberControl() {
        memberList = new LinkedList<>();
    }

    public int size() {
        return memberList.size();
    }

    public void addMember(Member member) {
        memberList.add(member);
    }

    public boolean findMember(String memberId) {
        return getMemberById(memberId) != null;
    }

    public boolean deleteMemberById(String memberId) {
        for (int i = 1; i <= memberList.size(); i++) {

            if (getMemberById(memberId) != null) {
                memberList.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean updateMemberById(String memberId, String name, int point) {
        Member member = getMemberById(memberId);

        if (member == null)
            return false;

        member.setName(name);
        member.setPoint(point);

        return true;
    }

    public int addMemberPoint(String memberId, int point) {
        Member member = getMemberById(memberId);

        if (member == null) {
            System.out.println("Member Not Found");
        }

        int newPoint = member.getPoint() + point;
        member.setPoint(newPoint);

        return newPoint;
    }

    public void displayAllMember() {
        for (int i = 1; i <= memberList.size(); i++) {
            Member member = memberList.getEntry(i);
            System.out.printf("%s\t%s\t%s\n", "Member Id", "Name", "Point");
            System.out.printf("%s\t%s\t%05d\n", member.getMemberId(), member.getName(), member.getPoint());
        }
    }

    // Helper Function

    private Member getMemberById(String memberId) {
        for (int i = 1; i <= memberList.size(); i++) {
            Member member = memberList.getEntry(i);
            if (member.getMemberId().equals(memberId)) {
                return member;
            }
        }
        return null;
    }

    public String generateMemberId() {
        if (memberList.isEmpty()) {
            return "M001";
        }

        Member lastMemberId = memberList.getEntry(memberList.size());

        String lastId = lastMemberId.getMemberId();

        int number = Integer.parseInt(lastId.substring(1));

        number++;

        return String.format("M%03d", number);

    }
}
