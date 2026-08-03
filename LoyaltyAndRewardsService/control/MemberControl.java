package LoyaltyAndRewardsService.control;

import java.util.Iterator;

import adt.ArrayList;
import adt.LinkedList;
import adt.SortedArrayList;

import LoyaltyAndRewardsService.dao.MemberDao;
import LoyaltyAndRewardsService.entity.Member;
import LoyaltyAndRewardsService.entity.Tier;

/**
 * @author Chee Weng
 */
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

    public boolean isEmpty() {
        return memberList.isEmpty();
    }

    public void addMember(Member member) {
        String tierId = tierControl.getTierIdByPoint(member.getPoint());
        member.setTierId(tierId);
        if (member.getLastNotifiedTierId() == null
                || member.getLastNotifiedTierId().isBlank()) {
            member.setLastNotifiedTierId(tierId);
        }
        memberList.add(member);
    }

    public String createMember(String name, int point) {
        String memberId = generateMemberId();
        addMember(new Member(memberId, name, point, tierControl.getTierIdByPoint(point)));
        saveMembers();
        return memberId;
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

    public boolean removeMember(String memberId) {
        boolean removed = deleteMemberById(memberId);
        if (removed) {
            saveMembers();
        }
        return removed;
    }

    public boolean updateMemberById(String memberId, String name, int point) {
        Member member = getMemberById(memberId);

        if (member == null)
            return false;

        member.setName(name);
        member.setPoint(point);
        String currentTierId = tierControl.getTierIdByPoint(point);
        assignTier(member, currentTierId);

        return true;
    }

    public boolean updateMember(String memberId, String name, int point) {
        boolean updated = updateMemberById(memberId, name, point);
        if (updated) {
            saveMembers();
        }
        return updated;
    }

    public int addMemberPoint(String memberId, int point) {
        Member member = getMemberById(memberId);

        if (member == null) {
            return -1;
        }

        int newPoint = member.getPoint() + point;
        member.setPoint(newPoint);

        String newTierId = tierControl.getTierIdByPoint(newPoint);
        assignTier(member, newTierId);

        return newPoint;
    }

    public int redeemPoint(String memberId, int pointRedeem) {
        Member member = getMemberById(memberId);

        if (member == null) {
            return -1;
        }

        if (pointRedeem < 0 || member.getPoint() < pointRedeem) {
            return -1;
        }

        int newPoint = member.getPoint() - pointRedeem;
        member.setPoint(newPoint);

        String newTierId = tierControl.getTierIdByPoint(newPoint);
        assignTier(member, newTierId);

        return newPoint;
    }

    public PointUpdateResult addPoints(String memberId, int points, TransactionControl transactionControl) {
        Member member = getMemberById(memberId);
        if (member == null || points <= 0) {
            return PointUpdateResult.failure();
        }

        String previousTierId = member.getTierId();
        int newPoint = addMemberPoint(memberId, points);
        transactionControl.addTransaction(memberId, points);
        saveMembers();
        transactionControl.saveTransactions();

        String newTierId = member.getTierId();
        return PointUpdateResult.success(
                newPoint,
                tierControl.getTierNameById(previousTierId),
                tierControl.getTierNameById(newTierId),
                previousTierId == null ? newTierId != null : !previousTierId.equalsIgnoreCase(newTierId));
    }

    public int recalculateAllMemberTiers() {
        int changedCount = 0;

        for (int i = 1; i <= memberList.size(); i++) {
            Member member = memberList.getEntry(i);
            String correctTierId = tierControl.getTierIdByPoint(member.getPoint());
            String currentTierId = member.getTierId();

            boolean changed = currentTierId == null
                    ? correctTierId != null
                    : !currentTierId.equalsIgnoreCase(correctTierId);
            if (changed) {
                assignTier(member, correctTierId);
                changedCount++;
            }
        }

        return changedCount;
    }

    public String getMemberTable() {
        if (memberList.isEmpty()) {
            return "";
        }

        StringBuilder output = new StringBuilder();
        String border = "+------------+----------------------+----------+----------------+";
        output.append(border).append(System.lineSeparator());
        output.append(String.format("| %-10s | %-20s | %8s | %-14s |%n",
                "Member ID", "Name", "Points", "Tier"));
        output.append(border).append(System.lineSeparator());
        for (int i = 1; i <= memberList.size(); i++) {
            Member member = memberList.getEntry(i);
            Tier tier = tierControl.getExistTierById(member.getTierId());
            String tierName = tier == null ? "Unknown" : tier.getTierLevel();
            output.append(String.format("| %-10.10s | %-20.20s | %8d | %-14.14s |%n",
                    member.getMemberId(), member.getName(), member.getPoint(), tierName));
        }
        output.append(border);
        return output.toString();
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
        SortedArrayList<Member> sortedResult =
                new SortedArrayList<>((left, right) -> right.compareTo(left));
        boolean hasTargetTier = targetTierId != null && !targetTierId.isEmpty();

        Iterator<Member> iterator = memberList.iterator();
        while (iterator.hasNext()) {
            Member current = iterator.next();
            boolean matchesCriteria = current.getPoint() >= minPoint
                    && (!hasTargetTier || current.getTierId().equalsIgnoreCase(targetTierId));
            if (matchesCriteria) {
                sortedResult.add(current);
            }
        }

        return copyToArrayList(sortedResult);
    }

    public ArrayList<Member> generateLowPointReport(int maxPoint, String excludeTierId) {
        SortedArrayList<Member> sortedResult = new SortedArrayList<>();
        boolean hasExcludedTier = excludeTierId != null && !excludeTierId.isEmpty();

        for (int i = 1; i <= memberList.size(); i++) {
            Member current = memberList.getEntry(i);

            boolean matchesCriteria = current.getPoint() <= maxPoint
                    && (!hasExcludedTier || !current.getTierId().equalsIgnoreCase(excludeTierId));

            if (matchesCriteria) {
                sortedResult.add(current);
            }
        }

        return copyToArrayList(sortedResult);
    }

    private ArrayList<Member> copyToArrayList(SortedArrayList<Member> sortedResult) {
        ArrayList<Member> result = new ArrayList<>();
        for (Member member : sortedResult) {
            result.add(member);
        }
        return result;
    }


    // Promotion Control
    public String generatePersonalizedPromotion(String memberId){
        Member member = getMemberById(memberId);

        if(member == null) return "Member Not Found";

        String currentTierId = tierControl.getTierIdByPoint(member.getPoint());
        assignTier(member, currentTierId);

        if (currentTierId == null) {
            return "No tier is configured for the member's current point balance.";
        }

        Tier nextTier = tierControl.getNextTier(currentTierId);

        if(nextTier == null) return "Congratulations! You are already at our highest membership tier.";
        

        int pointNeeded = nextTier.getMinPoint() - member.getPoint();

        String message = "Earn " + pointNeeded + " more points to reach " + nextTier.getTierLevel() + ".";
        return message;
    }

    public String getMemberTierId(String memberId) {
        Member member = getMemberById(memberId);
        return member == null ? null : member.getTierId();
    }

    public String getTierName(String tierId) {
        return tierControl.getTierNameById(tierId);
    }

    public void saveMembers() {
        MemberDao.saveToMemberFile(this);
    }

    public Iterator<Member> getUnreadTierUpgradeIterator() {
        LinkedList<Member> upgradedMembers = new LinkedList<>();
        for (Member member : memberList) {
            if (hasUnreadTierUpgrade(member)) {
                upgradedMembers.add(member);
            }
        }
        return upgradedMembers.iterator();
    }

    public int getUnreadTierUpgradeCount() {
        int count = 0;
        Iterator<Member> iterator = getUnreadTierUpgradeIterator();
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }

    public void markTierUpgradesAsRead() {
        for (Member member : memberList) {
            if (hasUnreadTierUpgrade(member)) {
                member.setLastNotifiedTierId(member.getTierId());
            }
        }
        saveMembers();
    }

    private boolean hasUnreadTierUpgrade(Member member) {
        String previousTierId = member.getLastNotifiedTierId();
        String currentTierId = member.getTierId();
        if (previousTierId == null || currentTierId == null
                || previousTierId.equalsIgnoreCase(currentTierId)) {
            return false;
        }

        int previousMinimumPoint = tierControl.getMinimumPoint(previousTierId);
        int currentMinimumPoint = tierControl.getMinimumPoint(currentTierId);
        return previousMinimumPoint >= 0 && currentMinimumPoint > previousMinimumPoint;
    }

    private void assignTier(Member member, String newTierId) {
        String previousTierId = member.getTierId();
        member.setTierId(newTierId);

        if (newTierId == null) {
            member.setLastNotifiedTierId(null);
            return;
        }
        if (previousTierId == null || member.getLastNotifiedTierId() == null
                || member.getLastNotifiedTierId().isBlank()) {
            member.setLastNotifiedTierId(newTierId);
            return;
        }

        int previousMinimumPoint = tierControl.getMinimumPoint(previousTierId);
        int currentMinimumPoint = tierControl.getMinimumPoint(newTierId);
        if (previousMinimumPoint < 0 || currentMinimumPoint < previousMinimumPoint) {
            member.setLastNotifiedTierId(newTierId);
        }
    }

    public static final class PointUpdateResult {
        private final boolean successful;
        private final int currentPoint;
        private final String previousTierName;
        private final String currentTierName;
        private final boolean tierChanged;

        private PointUpdateResult(boolean successful, int currentPoint, String previousTierName,
                String currentTierName, boolean tierChanged) {
            this.successful = successful;
            this.currentPoint = currentPoint;
            this.previousTierName = previousTierName;
            this.currentTierName = currentTierName;
            this.tierChanged = tierChanged;
        }

        public static PointUpdateResult failure() {
            return new PointUpdateResult(false, -1, "", "", false);
        }

        public static PointUpdateResult success(int currentPoint, String previousTierName,
                String currentTierName, boolean tierChanged) {
            return new PointUpdateResult(true, currentPoint, previousTierName, currentTierName, tierChanged);
        }

        public boolean isSuccessful() {
            return successful;
        }

        public int getCurrentPoint() {
            return currentPoint;
        }

        public boolean isTierChanged() {
            return tierChanged;
        }

        public String getTierChangeMessage() {
            return "Tier changed: " + previousTierName + " -> " + currentTierName;
        }
    }
}
