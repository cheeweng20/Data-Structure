package LoyaltyAndRewardsService;
import adt.LinkedList;
import java.io.*;


public class MemberControl {
    private LinkedList<Member> memberList;
    private TierControl tierControl;

    // File Variable
    private static final String FILE_NAME = "src/member.csv";

    public MemberControl(TierControl tierControl) {
        memberList = new LinkedList<>();
        this.tierControl = tierControl;
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

        String newTierId = tierControl.getTierIdByPoint(newPoint);
        member.setTierId(newTierId);

        return newPoint;
    }

    public void displayAllMember() {
        System.out.printf("%-10s %-15s %-8s %-10s\n", "Member Id", "Name", "Point", "Tier Level");
        for (int i = 1; i <= memberList.size(); i++) {
            Member member = memberList.getEntry(i);
            Tier tier = tierControl.getExistTierById(member.getTierId());
            System.out.printf("%-10s %-15s %-8d %-10s\n", member.getMemberId(), member.getName(), member.getPoint(),
                    tier.getTierLevel());
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

    // CSV File Reader and Writer
    public void loadFromMemberFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            reader.readLine(); // Skip file header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");

                String memberId = fields[0];
                String name = fields[1];
                int point = Integer.parseInt(fields[2]);
                String tierId = fields[3];

                memberList.add(new Member(memberId, name, point, tierId));
            }
        } catch (FileNotFoundException e) {
            System.out.println("No existing member data found, starting fresh.");
            createMemberCSVFile();

        } catch (IOException e) {
            System.out.println("Error reading member file: " + e.getMessage());
        }
    }

    public void saveToMemberFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            writer.println("MemberId,Name,Email,TierId");
            for (int i = 1; i <= memberList.size(); i++) {
                Member member = memberList.getEntry(i);
                writer.println(member.toCsvLine());
            }
        } catch (IOException e) {
            System.out.println("Error saving member file: " + e.getMessage());
        }
    }

    public void createMemberCSVFile() {
        try (PrintWriter writer = new PrintWriter(FILE_NAME)) {
            writer.println("MemberId,Name,Email,TierId");

            System.out.println("CSV File created success !");
        } catch (IOException e) {
            System.out.println("Error creating member file: " + e.getMessage());
        }
    }
}
