package LoyaltyAndRewardsService.control;

import adt.ArrayList;
import adt.LinkedList;

import LoyaltyAndRewardsService.entity.Member;
import LoyaltyAndRewardsService.entity.Tier;

public class MemberControl {
    private LinkedList<Member> memberList;
    private TierControl tierControl;

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

    public Member getEntry(int position) {
        return memberList.getEntry(position);
    }

    public boolean deleteMemberById(String memberId) {
        for (int i = 1; i <= memberList.size(); i++) {
            Member member = memberList.getEntry(i);
            if (member.getMemberId().equals(memberId)) {
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
        member.setTierId(tierControl.getTierIdByPoint(point));

        return true;
    }

    public int addMemberPoint(String memberId, int point) {
        Member member = getMemberById(memberId);

        if (member == null) {
            System.out.println("Member Not Found");
            return -1;
        }

        int newPoint = member.getPoint() + point;
        member.setPoint(newPoint);

        String newTierId = tierControl.getTierIdByPoint(newPoint);
        member.setTierId(newTierId);

        return newPoint;
    }

    public int redeemPoint(String memberId, int pointRedeem) {
        Member member = getMemberById(memberId);

        if (member == null) {
            System.out.println("Member Not Found");
            return -1;
        }

        int newPoint = member.getPoint() - pointRedeem;
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

    public Member getMemberById(String memberId) {
        for (int i = 1; i <= memberList.size(); i++) {
            Member member = memberList.getEntry(i);
            if (member.getMemberId().equalsIgnoreCase(memberId)) {
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

    public ArrayList<Member> generateRankingReport(int minPoint, String targetTierId) {
        ArrayList<Member> filteredResult = new ArrayList<>();
        boolean hasTargetTier = targetTierId != null && !targetTierId.isEmpty();

        for (int i = 1; i <= memberList.size(); i++) {
            Member current = memberList.getEntry(i);
            boolean matchesCriteria = current.getPoint() >= minPoint
                    && (!hasTargetTier || current.getTierId().equalsIgnoreCase(targetTierId));
            if (matchesCriteria) {
                filteredResult.add(current);
            }
        }

        selectionSortByPoint(filteredResult, false);
        return filteredResult;
    }

    public ArrayList<Member> generateLowPointReport(int maxPoint, String excludeTierId) {
        ArrayList<Member> filteredResult = new ArrayList<>();
        boolean hasExcludedTier = excludeTierId != null && !excludeTierId.isEmpty();

        for (int i = 1; i <= memberList.size(); i++) {
            Member current = memberList.getEntry(i);

            boolean matchesCriteria = current.getPoint() <= maxPoint
                    && (!hasExcludedTier || !current.getTierId().equalsIgnoreCase(excludeTierId));

            if (matchesCriteria) {
                filteredResult.add(current);
            }
        }

        selectionSortByPoint(filteredResult, true);
        return filteredResult;
    }

    private void selectionSortByPoint(ArrayList<Member> list, boolean ascending) {
        for (int i = 1; i <= list.getNumberOfEntries() - 1; i++) {
            int targetPosition = i;
            Member targetValue = list.getEntry(i);

            for (int j = i + 1; j <= list.getNumberOfEntries(); j++) {
                Member current = list.getEntry(j);

                boolean shouldSwap;
                if (ascending) {
                    shouldSwap = current.getPoint() < targetValue.getPoint();
                } else {
                    shouldSwap = current.getPoint() > targetValue.getPoint();
                }

                if (shouldSwap) {
                    targetValue = current;
                    targetPosition = j;
                }
            }

            if (targetPosition != i) {
                Member temp = list.getEntry(i);
                list.replace(i, targetValue);
                list.replace(targetPosition, temp);
            }
        }
    }


    // Promotion Control
    public String generatePersonalizedPromotion(String memberId){
        Member member = getMemberById(memberId);

        if(member == null) return "Member Not Found";

        String currentTierId = member.getTierId();
        if (currentTierId == null || tierControl.getExistTierById(currentTierId) == null) {
            currentTierId = tierControl.getTierIdByPoint(member.getPoint());
            member.setTierId(currentTierId);
        }

        Tier nextTier = tierControl.getNextTier(currentTierId);

        if(nextTier == null) return "Congratulations! You are already at our highest membership tier.";
        

        int pointNeeded = nextTier.getMinPoint() - member.getPoint();

        String message = "To achieve " + nextTier.getTierLevel() + " You Require " + pointNeeded + " points.";
        return message;
    }
}
