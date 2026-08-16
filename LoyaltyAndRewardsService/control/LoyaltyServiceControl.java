package LoyaltyAndRewardsService.control;

import java.time.LocalDate;
import java.util.Iterator;

import adt.ArrayList;
import adt.LinkedList;
import adt.LinkedQueue;
import adt.QueueInterface;
import adt.SortedArrayList;

import LoyaltyAndRewardsService.dao.MemberDao;
import LoyaltyAndRewardsService.dao.PointTransactionDao;
import LoyaltyAndRewardsService.dao.RequestDao;
import LoyaltyAndRewardsService.dao.RewardDao;
import LoyaltyAndRewardsService.dao.TierDao;
import LoyaltyAndRewardsService.entity.Member;
import LoyaltyAndRewardsService.entity.PointTransaction;
import LoyaltyAndRewardsService.entity.RedemptionRequest;
import LoyaltyAndRewardsService.entity.Reward;
import LoyaltyAndRewardsService.entity.Tier;
import LoyaltyAndRewardsService.utility.MessageUI;

/**
 * Initializes and coordinates the Loyalty and Rewards subsystem.
 *
 * @author Chee Weng
 */
public class LoyaltyServiceControl {
    private LinkedList<Tier> tierLinkedList;
    private LinkedList<Member> memberList;
    private LinkedList<PointTransaction> transactionList;
    private QueueInterface<RedemptionRequest> requestQueue;
    private LinkedList<RedemptionRequest> requestHistory;
    private LinkedList<Reward> rewardList;
    private int nextRequestNumber;
    private int recentlyExpiredPointTotal;

    // ==================== Initialization and Persistence Control ====================

    public LoyaltyServiceControl() {
        tierLinkedList = new LinkedList<>();
        memberList = new LinkedList<>();
        transactionList = new LinkedList<>();
        requestQueue = new LinkedQueue<>();
        requestHistory = new LinkedList<>();
        rewardList = new LinkedList<>();
        nextRequestNumber = 1;

        TierDao.loadFromTierFile(this);
        MemberDao.loadFromMemberFile(this);
        PointTransactionDao.loadFromTransactionFile(this);
        RequestDao.loadFromRequestFile(this);
        RewardDao.loadFromRewardFile(this);
        recentlyExpiredPointTotal = expirePointsAndSave();
    }

    public void saveAll() {
        saveMembers();
        TierDao.saveToTierFile(this);
        saveTransactions();
        saveRequests();
        saveRewards();
    }

    // ==================== Tier Control ====================

    public void addTierLevel(Tier tier) {
        tierLinkedList.add(tier);
        organizeTierRanges();
    }

    public boolean createTier(String tierLevelName, int minPoint) {
        if ((isTierEmpty() && minPoint != 0) || !isMinimumPointAvailable(minPoint, null)) {
            MessageUI.displayError("Tier level could not be added.");
            return false;
        }

        String tierId = generateTierId();
        addTierLevel(new Tier(tierId, tierLevelName, minPoint, 0));
        int updatedMembers = persistTierChanges();
        MessageUI.displayTierAdded(tierId);
        MessageUI.displayTierRecalculation(updatedMembers);
        return true;
    }

    public int getTierCount() {
        return tierLinkedList.size();
    }

    public boolean isTierEmpty() {
        return tierLinkedList.isEmpty();
    }

    public boolean findTier(String tierId) {
        return getExistTierById(tierId) != null;
    }

    public Tier getTierEntry(int position) {
        return tierLinkedList.getEntry(position);
    }

    public String getTierTable() {
        if (tierLinkedList.isEmpty()) {
            return "";
        }

        StringBuilder output = new StringBuilder();
        String border = "+------------+----------------------+------------+------------+";
        output.append(border).append(System.lineSeparator());
        output.append(String.format("| %-10s | %-20s | %10s | %10s |%n",
                "Tier ID", "Tier Level", "Min Points", "Max Points"));
        output.append(border).append(System.lineSeparator());
        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier tier = tierLinkedList.getEntry(i);
            String maxPoints = tier.getMaxPoint() == 0 ? "No limit" : String.valueOf(tier.getMaxPoint());
            output.append(String.format("| %-10.10s | %-20.20s | %10d | %10s |%n",
                    tier.getTierId(), tier.getTierLevel(), tier.getMinPoint(), maxPoints));
        }
        output.append(border);
        return output.toString();
    }

    public boolean removeTier(String tierId) {
        Tier tier = getExistTierById(tierId);
        if (tier == null || (getTierCount() > 1 && tier.getMinPoint() == 0)) {
            MessageUI.displayError("Tier level could not be deleted.");
            return false;
        }

        for (int i = 1; i <= tierLinkedList.size(); i++) {
            if (tierLinkedList.getEntry(i).getTierId().equalsIgnoreCase(tierId)) {
                tierLinkedList.remove(i);
                break;
            }
        }
        organizeTierRanges();
        int updatedMembers = persistTierChanges();
        MessageUI.displayTierDeleted();
        MessageUI.displayTierRecalculation(updatedMembers);
        return true;
    }

    public boolean updateTier(String tierId, String tierLevelName, int minPoint) {
        Tier existing = getExistTierById(tierId);
        if (existing == null
                || (existing.getMinPoint() == 0 && minPoint != 0)
                || !isMinimumPointAvailable(minPoint, tierId)) {
            MessageUI.displayError("Tier level could not be updated.");
            return false;
        }

        existing.setTierLevel(tierLevelName);
        existing.setMinPoint(minPoint);
        existing.setMaxPoint(0);
        organizeTierRanges();
        int updatedMembers = persistTierChanges();
        MessageUI.displayTierUpdated();
        MessageUI.displayTierRecalculation(updatedMembers);
        return true;
    }

    public Tier getExistTierById(String tierId) {
        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier current = tierLinkedList.getEntry(i);
            if (current.getTierId().equalsIgnoreCase(tierId)) {
                return current;
            }
        }
        return null;
    }

    public String getTierIdByPoint(int point) {
        Tier matchingTier = null;

        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier current = tierLinkedList.getEntry(i);
            if (point >= current.getMinPoint()) {
                matchingTier = current;
            } else {
                break;
            }
        }

        return matchingTier == null ? null : matchingTier.getTierId();
    }

    public String getTierNameById(String tierId) {
        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier current = tierLinkedList.getEntry(i);
            if (current.getTierId().equalsIgnoreCase(tierId)) {
                return current.getTierLevel();
            }
        }
        return "Unknown";
    }

    public String getTierName(String tierId) {
        return getTierNameById(tierId);
    }

    public Tier getNextTier(String tierId) {
        for (int i = 1; i <= tierLinkedList.size(); i++) {
            if (tierLinkedList.getEntry(i).getTierId().equalsIgnoreCase(tierId)) {
                return i < tierLinkedList.size() ? tierLinkedList.getEntry(i + 1) : null;
            }
        }
        return null;
    }

    public String generateTierId() {
        int highestNumber = 0;
        for (int i = 1; i <= tierLinkedList.size(); i++) {
            String tierId = tierLinkedList.getEntry(i).getTierId();
            if (tierId.length() > 1 && (tierId.charAt(0) == 'T' || tierId.charAt(0) == 't')) {
                try {
                    highestNumber = Math.max(highestNumber, Integer.parseInt(tierId.substring(1)));
                } catch (NumberFormatException ignored) {
                    // Ignore non-standard IDs while finding the next generated ID.
                }
            }
        }
        return String.format("T%03d", highestNumber + 1);
    }

    public boolean isMinimumPointAvailable(int minPoint, String excludedTierId) {
        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier current = tierLinkedList.getEntry(i);
            boolean isExcludedTier = excludedTierId != null
                    && current.getTierId().equalsIgnoreCase(excludedTierId);
            if (!isExcludedTier && current.getMinPoint() == minPoint) {
                return false;
            }
        }
        return true;
    }

    public int getMinimumPoint(String tierId) {
        Tier tier = getExistTierById(tierId);
        return tier == null ? -1 : tier.getMinPoint();
    }

    public boolean isBaseTier(String tierId) {
        return getMinimumPoint(tierId) == 0;
    }

    // ==================== Member Control ====================

    public int getMemberCount() {
        return memberList.size();
    }

    public boolean isMemberEmpty() {
        return memberList.isEmpty();
    }

    public void addMember(Member member) {
        promoteTierIfEligible(member);
        String tierId = member.getTierId();
        if (member.getLastNotifiedTierId() == null
                || member.getLastNotifiedTierId().isBlank()) {
            member.setLastNotifiedTierId(tierId);
        }
        memberList.add(member);
    }

    public String createMember(String name, String passport, String phoneNumber, int point) {
        String memberId = generateMemberId();
        addMember(new Member(memberId, name, passport, phoneNumber, point,
                getTierIdByPoint(point)));
        saveMembers();
        return memberId;
    }

    public boolean findMember(String memberId) {
        return getMemberById(memberId) != null;
    }

    public Member getMemberEntry(int position) {
        return memberList.getEntry(position);
    }

    public boolean removeMember(String memberId) {
        for (int i = 1; i <= memberList.size(); i++) {
            Member member = memberList.getEntry(i);
            if (member.getMemberId().equals(memberId)) {
                memberList.remove(i);
                saveMembers();
                return true;
            }
        }
        return false;
    }

    public boolean updateMember(String memberId, String name, int point) {
        Member member = getMemberById(memberId);
        if (member == null) {
            return false;
        }
        return updateMember(memberId, name, member.getPassport(), member.getPhoneNumber(), point);
    }

    public boolean updateMember(String memberId, String name, String passport,
            String phoneNumber, int point) {
        Member member = getMemberById(memberId);
        if (member == null) {
            return false;
        }

        member.setName(name);
        member.setPassport(passport);
        member.setPhoneNumber(phoneNumber);
        member.setPoint(point);
        promoteTierIfEligible(member);
        saveMembers();
        return true;
    }

    public int redeemPoint(String memberId, int pointRedeem) {
        Member member = getMemberById(memberId);
        if (member == null || pointRedeem < 0 || member.getPoint() < pointRedeem) {
            return -1;
        }

        int newPoint = member.getPoint() - pointRedeem;
        member.setPoint(newPoint);
        return newPoint;
    }

    public int addPoints(String memberId, int points) {
        Member member = getMemberById(memberId);
        if (member == null) {
            MessageUI.displayError("Member not found.");
            return -1;
        }
        if (points <= 0) {
            MessageUI.displayError("Points to add must be greater than zero.");
            return -1;
        }

        String previousTierId = member.getTierId();
        int newPoint = member.getPoint() + points;
        member.setPoint(newPoint);
        promoteTierIfEligible(member);
        addTransaction(memberId, points);
        saveMembers();
        saveTransactions();

        String newTierId = member.getTierId();
        boolean tierChanged = previousTierId == null
                ? newTierId != null
                : !previousTierId.equalsIgnoreCase(newTierId);
        MessageUI.displayPointsAdded(points, newPoint);
        if (tierChanged) {
            MessageUI.displayTierChange(getTierNameById(previousTierId), getTierNameById(newTierId));
        }
        return newPoint;
    }

    public int recalculateAllMemberTiers() {
        int changedCount = 0;

        for (int i = 1; i <= memberList.size(); i++) {
            Member member = memberList.getEntry(i);
            if (promoteTierIfEligible(member)) {
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
        String border = "+------------+----------------------+------------------+------------------+----------+----------------+";
        output.append(border).append(System.lineSeparator());
        output.append(String.format("| %-10s | %-20s | %-16s | %-16s | %8s | %-14s |%n",
                "Member ID", "Name", "Passport", "Phone Number", "Points", "Tier"));
        output.append(border).append(System.lineSeparator());
        for (int i = 1; i <= memberList.size(); i++) {
            Member member = memberList.getEntry(i);
            Tier tier = getExistTierById(member.getTierId());
            String tierName = tier == null ? "Unknown" : tier.getTierLevel();
            output.append(String.format(
                    "| %-10.10s | %-20.20s | %-16.16s | %-16.16s | %8d | %-14.14s |%n",
                    member.getMemberId(), member.getName(), member.getPassport(),
                    member.getPhoneNumber(), member.getPoint(), tierName));
        }
        output.append(border);
        return output.toString();
    }

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
        int number = Integer.parseInt(lastMemberId.getMemberId().substring(1));
        return String.format("M%03d", number + 1);
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

    public String generatePersonalizedPromotion(String memberId) {
        Member member = getMemberById(memberId);
        if (member == null) {
            return "Member Not Found";
        }

        promoteTierIfEligible(member);
        String currentTierId = member.getTierId();

        if (currentTierId == null) {
            return "No tier is configured for the member's current point balance.";
        }

        Tier nextTier = getNextTier(currentTierId);
        if (nextTier == null) {
            return "Congratulations! You are already at our highest membership tier.";
        }

        int pointNeeded = nextTier.getMinPoint() - member.getPoint();
        return "Earn " + pointNeeded + " more points to reach " + nextTier.getTierLevel() + ".";
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
        for (Member member : memberList) {
            if (hasUnreadTierUpgrade(member)) {
                count++;
            }
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

    // ==================== Point Transaction Control ====================

    public PointTransaction getTransactionEntry(int position) {
        return transactionList.getEntry(position);
    }

    public int getTransactionCount() {
        return transactionList.size();
    }

    public Iterator<PointTransaction> getTransactionIterator() {
        return transactionList.iterator();
    }

    public void addTransaction(PointTransaction transaction) {
        transactionList.add(transaction);
    }

    public void addTransaction(String memberId, int points) {
        LocalDate earnedDate = LocalDate.now();
        LocalDate expiryDate = earnedDate.plusYears(1);
        String transactionId = generateTransactionId();
        transactionList.add(new PointTransaction(transactionId, memberId, points, earnedDate, expiryDate));
    }

    public ArrayList<PointTransaction> generateExpiringReport(int withinDays) {
        SortedArrayList<PointTransaction> sortedResult = new SortedArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(withinDays);

        Iterator<PointTransaction> iterator = transactionList.iterator();
        while (iterator.hasNext()) {
            PointTransaction current = iterator.next();
            boolean matchesCriteria = current.getPointsRemaining() > 0
                    && !current.getExpiryDate().isBefore(today)
                    && !current.getExpiryDate().isAfter(cutoff);

            if (matchesCriteria) {
                sortedResult.add(current);
            }
        }

        return copyToArrayList(sortedResult);
    }

    public int redeemPointsFromOldestTransactions(String memberId, int pointsToRedeem) {
        int remainingToRedeem = pointsToRedeem;

        while (remainingToRedeem > 0) {
            PointTransaction oldest = findOldestAvailableTransaction(memberId);
            if (oldest == null) {
                break;
            }

            int deducted = Math.min(oldest.getPointsRemaining(), remainingToRedeem);
            oldest.setPointsRemaining(oldest.getPointsRemaining() - deducted);
            remainingToRedeem -= deducted;
        }

        return pointsToRedeem - remainingToRedeem;
    }

    /**
     * Expires unused transaction points after their expiry date and deducts the
     * available amount from the member's spendable balance without changing the
     * member's achieved tier.
     *
     * @return the total number of unused transaction points that expired
     */
    public int expirePointsAndSave() {
        int expiredPoints = expirePoints(LocalDate.now());
        if (expiredPoints > 0) {
            saveMembers();
            saveTransactions();
        }
        return expiredPoints;
    }

    public int getRecentlyExpiredPointTotal() {
        return recentlyExpiredPointTotal;
    }

    public int getExpiringTransactionCount(int withinDays) {
        return generateExpiringReport(withinDays).getNumberOfEntries();
    }

    public int getExpiringPointTotal(int withinDays) {
        int total = 0;
        Iterator<PointTransaction> iterator = generateExpiringReport(withinDays).iterator();
        while (iterator.hasNext()) {
            total += iterator.next().getPointsRemaining();
        }
        return total;
    }

    public void saveTransactions() {
        PointTransactionDao.saveToTransactionFile(this);
    }

    public String generateTransactionId() {
        if (transactionList.isEmpty()) {
            return "TS001";
        }

        PointTransaction lastTransactionId = transactionList.getEntry(transactionList.size());
        String lastId = lastTransactionId.getTransactionId();
        int prefixLength = lastId.startsWith("TS") ? 2 : 1;
        int number = Integer.parseInt(lastId.substring(prefixLength));
        return String.format("TS%03d", number + 1);
    }

    // ==================== Redemption Request Control ====================

    public boolean submitRewardRequest(String memberId, String rewardId) {
        expirePointsAndSave();
        if (!findMember(memberId)) {
            MessageUI.displayError("Member not found.");
            return false;
        }

        Reward reward = getRewardById(rewardId);
        if (reward == null || reward.getPointRequired() <= 0) {
            MessageUI.displayError("Reward not found.");
            return false;
        }

        if (!createPendingRequest(memberId, rewardId, reward.getPointRequired())) {
            MessageUI.displayError("Insufficient available points; request not accepted.");
            return false;
        }

        saveRequests();
        MessageUI.displayRequestSubmitted(reward.getRewardName());
        return true;
    }

    private RedemptionRequest peekNextRequest() {
        return requestQueue.getFront();
    }

    public RedemptionRequest processNextRequestAndSave(boolean approve) {
        expirePointsAndSave();
        RedemptionRequest next = peekNextRequest();
        if (next == null) {
            MessageUI.displayInfo("No pending requests.");
            return null;
        }

        Member member = getMemberById(next.getMemberId());
        String previousTierId = member == null ? null : member.getTierId();
        RedemptionRequest processed = requestQueue.dequeue();

        if (approve) {
            int newPoint = redeemPoint(processed.getMemberId(), processed.getPointsRequested());
            if (newPoint >= 0) {
                redeemPointsFromOldestTransactions(
                        processed.getMemberId(), processed.getPointsRequested());
                processed.setStatus("Approved");
            } else {
                processed.setStatus("Rejected - insufficient points");
            }
        } else {
            processed.setStatus("Rejected");
        }

        saveRequests();

        if ("Approved".equalsIgnoreCase(processed.getStatus())) {
            saveMembers();
            saveTransactions();
        }

        String newTierId = member == null ? null : member.getTierId();
        boolean tierChanged = previousTierId == null
                ? newTierId != null
                : !previousTierId.equalsIgnoreCase(newTierId);
        MessageUI.displayRequestProcessed(processed.getStatus());
        if (tierChanged) {
            MessageUI.displayTierChange(
                    getTierNameById(previousTierId), getTierNameById(newTierId));
        }
        return processed;
    }

    public void addRequest(RedemptionRequest request) {
        requestHistory.add(request);
        if ("Pending".equalsIgnoreCase(request.getStatus())) {
            requestQueue.enqueue(request);
        }
        updateNextRequestNumber(request.getRequestId());
    }

    public Iterator<RedemptionRequest> getRequestIterator() {
        return requestHistory.iterator();
    }

    public Iterator<RedemptionRequest> getPendingRequestIterator() {
        return requestQueue.getIterator();
    }

    public int getPendingRequestCount() {
        int count = 0;
        Iterator<RedemptionRequest> iterator = requestQueue.getIterator();
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }

    public String getNextRequestTable() {
        RedemptionRequest request = peekNextRequest();
        if (request == null) {
            return "";
        }
        return buildRequestTable("Next Redemption Request", singleRequestIterator(request));
    }

    public String getPendingRequestTable() {
        return buildRequestTable("Pending Redemption Requests", getPendingRequestIterator());
    }

    public String getRequestHistoryTable() {
        return buildRequestTable("Redemption Request History", getRequestIterator());
    }

    public void saveRequests() {
        RequestDao.saveToRequestFile(this);
    }

    // ==================== Reward Control ====================

    public int getRewardCount() {
        return rewardList.size();
    }

    public boolean isRewardEmpty() {
        return rewardList.isEmpty();
    }

    public void addReward(Reward reward) {
        rewardList.add(reward);
    }

    public String createReward(String rewardName, int pointRequired) {
        String rewardId = generateRewardId();
        addReward(new Reward(rewardId, rewardName, pointRequired));
        saveRewards();
        return rewardId;
    }

    public Reward getRewardEntry(int position) {
        return rewardList.getEntry(position);
    }

    public boolean findReward(String rewardId) {
        return getRewardById(rewardId) != null;
    }

    public boolean removeReward(String rewardId) {
        for (int i = 1; i <= rewardList.size(); i++) {
            if (rewardList.getEntry(i).getRewardId().equalsIgnoreCase(rewardId)) {
                rewardList.remove(i);
                saveRewards();
                return true;
            }
        }
        return false;
    }

    public boolean updateReward(String rewardId, String rewardName, int pointRequired) {
        Reward reward = getRewardById(rewardId);
        if (reward == null) {
            return false;
        }

        reward.setRewardName(rewardName);
        reward.setPointRequired(pointRequired);
        saveRewards();
        return true;
    }

    public Reward getRewardById(String rewardId) {
        for (int i = 1; i <= rewardList.size(); i++) {
            Reward reward = rewardList.getEntry(i);
            if (reward.getRewardId().equalsIgnoreCase(rewardId)) {
                return reward;
            }
        }
        return null;
    }

    public String getRewardTable() {
        if (rewardList.isEmpty()) {
            return "";
        }

        StringBuilder output = new StringBuilder();
        String border = "+------------+--------------------------------+-----------------+";
        output.append(border).append(System.lineSeparator());
        output.append(String.format("| %-10s | %-30s | %15s |%n",
                "Reward ID", "Reward Name", "Points Required"));
        output.append(border).append(System.lineSeparator());
        for (int i = 1; i <= rewardList.size(); i++) {
            Reward reward = rewardList.getEntry(i);
            output.append(String.format("| %-10.10s | %-30.30s | %15d |%n",
                    reward.getRewardId(), reward.getRewardName(), reward.getPointRequired()));
        }
        output.append(border);
        return output.toString();
    }

    public String generateRewardId() {
        int highestNumber = 0;

        for (int i = 1; i <= rewardList.size(); i++) {
            String rewardId = rewardList.getEntry(i).getRewardId();
            if (rewardId.length() > 1 && (rewardId.charAt(0) == 'R' || rewardId.charAt(0) == 'r')) {
                try {
                    highestNumber = Math.max(highestNumber, Integer.parseInt(rewardId.substring(1)));
                } catch (NumberFormatException ignored) {
                    // Ignore non-standard IDs while finding the next generated ID.
                }
            }
        }

        return String.format("R%03d", highestNumber + 1);
    }

    public void saveRewards() {
        RewardDao.saveToRewardFile(this);
    }

    // ==================== Report Control ====================

    public String generateMemberRankingReport(int minimumPoint, String tierId) {
        ArrayList<Member> members = generateRankingReport(minimumPoint, tierId);
        if (members.isEmpty()) {
            return "";
        }

        StringBuilder output = new StringBuilder();
        String border = "+----------------+----------------------+------------+----------+";
        output.append("=== Member Point Ranking Report ===").append("\n");
        output.append(border).append("\n");
        output.append(String.format("| %-14s | %-20s | %-10s | %8s |%n",
                "Tier Name", "Member Name", "Member ID", "Points"));
        output.append(border).append("\n");

        Iterator<Member> iterator = members.iterator();
        while (iterator.hasNext()) {
            Member member = iterator.next();
            output.append(String.format("| %-14.14s | %-20.20s | %-10.10s | %8d |%n",
                    getTierNameById(member.getTierId()), member.getName(),
                    member.getMemberId(), member.getPoint()));
        }
        output.append(border);
        return output.toString();
    }

    public String generateExpiringPointsReport(int withinDays) {
        ArrayList<PointTransaction> transactions = generateExpiringReport(withinDays);
        if (transactions.isEmpty()) {
            return "";
        }

        StringBuilder output = new StringBuilder();
        String border = "+----------------+------------+-----------------+--------------+";
        output.append("=== Expiring Points Alert ===").append("\n");
        output.append(border).append("\n");
        output.append(String.format("| %-14s | %-10s | %15s | %-12s |%n",
                "Transaction ID", "Member ID", "Points Expiring", "Expiry Date"));
        output.append(border).append("\n");

        Iterator<PointTransaction> iterator = transactions.iterator();
        while (iterator.hasNext()) {
            PointTransaction transaction = iterator.next();
            output.append(String.format("| %-14.14s | %-10.10s | %15d | %-12s |%n",
                    transaction.getTransactionId(), transaction.getMemberId(),
                    transaction.getPointsRemaining(), transaction.getExpiryDate()));
        }
        output.append(border);
        return output.toString();
    }

    public String generateBusinessCycleSummary(LocalDate startDate, LocalDate endDate,
            String tierId, int minimumPoint) {
        ArrayList<Member> rankedMembers = generateRankingReport(minimumPoint, tierId);
        ArrayList<PointTransaction> transactions = filterTransactionsByDateRange(startDate, endDate);
        ArrayList<RedemptionRequest> requests = filterRequestsByDateRange(startDate, endDate);

        StringBuilder output = new StringBuilder();
        output.append("=== Business Cycle Summary Report ===").append("\n");
        output.append("Cycle Period: ").append(startDate).append(" to ").append(endDate).append("\n");
        output.append("Applied Filters: tier=")
                .append(tierId == null || tierId.isBlank() ? "All" : tierId)
                .append(", minimum point=").append(minimumPoint)
                .append("\n").append("\n");

        appendMemberSummary(output, rankedMembers);
        appendTransactionSummary(output, transactions);
        appendRequestSummary(output, requests);
        return output.toString();
    }

    // ==================== Tier and Member Helpers ====================

    private int persistTierChanges() {
        int updatedMembers = recalculateAllMemberTiers();
        TierDao.saveToTierFile(this);
        MemberDao.saveToMemberFile(this);
        return updatedMembers;
    }

    private void organizeTierRanges() {
        sortByMinimumPoint();

        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier current = tierLinkedList.getEntry(i);
            if (i < tierLinkedList.size()) {
                Tier next = tierLinkedList.getEntry(i + 1);
                current.setMaxPoint(next.getMinPoint() - 1);
            } else {
                current.setMaxPoint(0);
            }
        }
    }

    private void sortByMinimumPoint() {
        SortedArrayList<Tier> sortedTiers = new SortedArrayList<>();
        for (Tier tier : tierLinkedList) {
            sortedTiers.add(tier);
        }

        tierLinkedList.clear();
        for (Tier tier : sortedTiers) {
            tierLinkedList.add(tier);
        }
    }

    private boolean hasUnreadTierUpgrade(Member member) {
        String previousTierId = member.getLastNotifiedTierId();
        String currentTierId = member.getTierId();
        if (previousTierId == null || currentTierId == null
                || previousTierId.equalsIgnoreCase(currentTierId)) {
            return false;
        }

        int previousMinimumPoint = getMinimumPoint(previousTierId);
        int currentMinimumPoint = getMinimumPoint(currentTierId);
        return previousMinimumPoint >= 0 && currentMinimumPoint > previousMinimumPoint;
    }

    /**
     * Promotes a member when the current point balance qualifies for a higher
     * tier. A valid achieved tier is never replaced by a lower tier when points
     * are redeemed, expire, or are manually reduced.
     */
    private boolean promoteTierIfEligible(Member member) {
        String eligibleTierId = getTierIdByPoint(member.getPoint());
        if (eligibleTierId == null) {
            return false;
        }

        String currentTierId = member.getTierId();
        int currentMinimumPoint = getMinimumPoint(currentTierId);
        int eligibleMinimumPoint = getMinimumPoint(eligibleTierId);
        boolean hasNoValidCurrentTier = currentTierId == null || currentMinimumPoint < 0;

        if (hasNoValidCurrentTier || eligibleMinimumPoint > currentMinimumPoint) {
            assignTier(member, eligibleTierId);
            return true;
        }
        return false;
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

        int previousMinimumPoint = getMinimumPoint(previousTierId);
        int currentMinimumPoint = getMinimumPoint(newTierId);
        if (previousMinimumPoint < 0 || currentMinimumPoint < previousMinimumPoint) {
            member.setLastNotifiedTierId(newTierId);
        }
    }

    // ==================== Point Transaction Helpers ====================

    private PointTransaction findOldestAvailableTransaction(String memberId) {
        PointTransaction oldest = null;
        LocalDate today = LocalDate.now();
        Iterator<PointTransaction> iterator = transactionList.iterator();

        while (iterator.hasNext()) {
            PointTransaction current = iterator.next();
            boolean belongsToMember = current.getMemberId().equalsIgnoreCase(memberId);
            if (belongsToMember && current.getPointsRemaining() > 0
                    && !current.getExpiryDate().isBefore(today)
                    && (oldest == null || current.compareTo(oldest) < 0)) {
                oldest = current;
            }
        }

        return oldest;
    }

    private int expirePoints(LocalDate today) {
        int totalExpired = 0;
        Iterator<PointTransaction> iterator = transactionList.iterator();

        while (iterator.hasNext()) {
            PointTransaction transaction = iterator.next();
            if (transaction.getPointsRemaining() <= 0
                    || !transaction.getExpiryDate().isBefore(today)) {
                continue;
            }

            int expiringPoints = transaction.getPointsRemaining();
            transaction.setPointsRemaining(0);
            totalExpired += expiringPoints;

            Member member = getMemberById(transaction.getMemberId());
            if (member != null) {
                int deducted = Math.min(member.getPoint(), expiringPoints);
                member.setPoint(member.getPoint() - deducted);
            }
        }

        return totalExpired;
    }

    // ==================== Redemption Request Helpers ====================

    private boolean createPendingRequest(String memberId, String rewardId, int pointsRequested) {
        Member currentMember = getMemberById(memberId);
        if (currentMember == null || pointsRequested <= 0) {
            return false;
        }

        int availablePoints = currentMember.getPoint() - getPendingPointsForMember(memberId);
        if (availablePoints < pointsRequested) {
            return false;
        }

        String requestId = generateRequestId();
        RedemptionRequest request = new RedemptionRequest(
                requestId, memberId, rewardId, pointsRequested, LocalDate.now(), "Pending");
        requestQueue.enqueue(request);
        requestHistory.add(request);
        return true;
    }

    private int getPendingPointsForMember(String memberId) {
        int pendingPoints = 0;
        Iterator<RedemptionRequest> iterator = requestQueue.getIterator();
        while (iterator.hasNext()) {
            RedemptionRequest request = iterator.next();
            if (request.getMemberId().equalsIgnoreCase(memberId)
                    && "Pending".equalsIgnoreCase(request.getStatus())) {
                pendingPoints += request.getPointsRequested();
            }
        }
        return pendingPoints;
    }

    private String generateRequestId() {
        return String.format("R%03d", nextRequestNumber++);
    }

    private void updateNextRequestNumber(String requestId) {
        int number = Integer.parseInt(requestId.substring(1));
        if (number >= nextRequestNumber) {
            nextRequestNumber = number + 1;
        }
    }

    private Iterator<RedemptionRequest> singleRequestIterator(RedemptionRequest request) {
        LinkedList<RedemptionRequest> singleRequest = new LinkedList<>();
        singleRequest.add(request);
        return singleRequest.iterator();
    }

    private String buildRequestTable(String title, Iterator<RedemptionRequest> iterator) {
        if (!iterator.hasNext()) {
            return "";
        }

        StringBuilder output = new StringBuilder();
        String border = "+------------+------------+------------+------------------+------------+--------------------------------+";
        output.append("=== ").append(title).append(" ===").append(System.lineSeparator());
        output.append(border).append(System.lineSeparator());
        output.append(String.format("| %-10s | %-10s | %-10s | %16s | %-10s | %-30s |%n",
                "Request ID", "Member ID", "Reward ID", "Points", "Date", "Status"));
        output.append(border).append(System.lineSeparator());

        while (iterator.hasNext()) {
            RedemptionRequest request = iterator.next();
            String rewardId = request.getRewardId() == null || request.getRewardId().isBlank()
                    ? "Legacy"
                    : request.getRewardId();
            output.append(String.format("| %-10.10s | %-10.10s | %-10.10s | %16d | %-10s | %-30.30s |%n",
                    request.getRequestId(), request.getMemberId(), rewardId,
                    request.getPointsRequested(), request.getRequestDate(), request.getStatus()));
        }

        output.append(border);
        return output.toString();
    }

    // ==================== Report Helpers ====================

    private ArrayList<PointTransaction> filterTransactionsByDateRange(
            LocalDate startDate, LocalDate endDate) {
        SortedArrayList<PointTransaction> sortedResult = new SortedArrayList<>(
                (left, right) -> left.getEarnedDate().compareTo(right.getEarnedDate()));
        Iterator<PointTransaction> iterator = getTransactionIterator();
        while (iterator.hasNext()) {
            PointTransaction current = iterator.next();
            if (!current.getEarnedDate().isBefore(startDate)
                    && !current.getEarnedDate().isAfter(endDate)) {
                sortedResult.add(current);
            }
        }
        return copyToArrayList(sortedResult);
    }

    private ArrayList<RedemptionRequest> filterRequestsByDateRange(
            LocalDate startDate, LocalDate endDate) {
        SortedArrayList<RedemptionRequest> sortedResult = new SortedArrayList<>();
        Iterator<RedemptionRequest> iterator = getRequestIterator();
        while (iterator.hasNext()) {
            RedemptionRequest current = iterator.next();
            if (!current.getRequestDate().isBefore(startDate)
                    && !current.getRequestDate().isAfter(endDate)) {
                sortedResult.add(current);
            }
        }
        return copyToArrayList(sortedResult);
    }

    private void appendMemberSummary(StringBuilder output, ArrayList<Member> members) {
        String border = "+------------+----------------------+------------+----------+";
        output.append("=== Top Members by Current Points ===").append("\n");
        output.append(border).append("\n");
        output.append(String.format("| %-10s | %-20s | %-10s | %8s |%n",
                "Member ID", "Member Name", "Tier", "Points"));
        output.append(border).append("\n");

        if (members.isEmpty()) {
            output.append(String.format("| %-10s | %-20s | %-10s | %8s |%n",
                    "-", "No matching members", "-", "-"));
        } else {
            Iterator<Member> iterator = members.iterator();
            while (iterator.hasNext()) {
                Member member = iterator.next();
                output.append(String.format("| %-10.10s | %-20.20s | %-10.10s | %8d |%n",
                        member.getMemberId(), member.getName(),
                        getTierNameById(member.getTierId()), member.getPoint()));
            }
        }
        output.append(border).append("\n");
    }

    private void appendTransactionSummary(StringBuilder output,
            ArrayList<PointTransaction> transactions) {
        int totalPointsEarned = 0;
        Iterator<PointTransaction> totalIterator = transactions.iterator();
        while (totalIterator.hasNext()) {
            totalPointsEarned += totalIterator.next().getPointsEarned();
        }

        output.append("\n").append("=== Transaction Summary ===").append("\n");
        output.append("Transactions in cycle: ").append(transactions.getNumberOfEntries()).append("\n");
        output.append("Total points earned in cycle: ").append(totalPointsEarned).append("\n");

        String border = "+----------------+------------+----------------+-------------+";
        output.append(border).append("\n");
        output.append(String.format("| %-14s | %-10s | %-14s | %-11s |%n",
                "Transaction ID", "Member ID", "Points Earned", "Earned Date"));
        output.append(border).append("\n");

        Iterator<PointTransaction> iterator = transactions.iterator();
        while (iterator.hasNext()) {
            PointTransaction transaction = iterator.next();
            output.append(String.format("| %-14.14s | %-10.10s | %14d | %-11s |%n",
                    transaction.getTransactionId(), transaction.getMemberId(),
                    transaction.getPointsEarned(), transaction.getEarnedDate()));
        }
        output.append(border).append("\n");
    }

    private void appendRequestSummary(StringBuilder output,
            ArrayList<RedemptionRequest> requests) {
        int pending = 0;
        int approved = 0;
        int rejected = 0;

        Iterator<RedemptionRequest> statusIterator = requests.iterator();
        while (statusIterator.hasNext()) {
            String status = statusIterator.next().getStatus();
            if ("Pending".equalsIgnoreCase(status)) {
                pending++;
            } else if ("Approved".equalsIgnoreCase(status)) {
                approved++;
            } else {
                rejected++;
            }
        }

        output.append("\n").append("=== Redemption Request Summary ===").append("\n");
        output.append("Requests in cycle: ").append(requests.getNumberOfEntries()).append("\n");
        output.append("Pending: ").append(pending)
                .append(", Approved: ").append(approved)
                .append(", Rejected: ").append(rejected).append("\n");

        String border =
                "+------------+------------+------------+------------------+--------------------+--------------------------------+";
        output.append(border).append("\n");
        output.append(String.format("| %-10s | %-10s | %-10s | %-16s | %-18s | %-30s |%n",
                "Request ID", "Member ID", "Reward ID", "Points Requested",
                "Request Date", "Status"));
        output.append(border).append("\n");

        Iterator<RedemptionRequest> iterator = requests.iterator();
        while (iterator.hasNext()) {
            RedemptionRequest request = iterator.next();
            String rewardId = request.getRewardId() == null || request.getRewardId().isBlank()
                    ? "Legacy"
                    : request.getRewardId();
            output.append(String.format(
                    "| %-10.10s | %-10.10s | %-10.10s | %16d | %-18s | %-30.30s |%n",
                    request.getRequestId(), request.getMemberId(), rewardId,
                    request.getPointsRequested(), request.getRequestDate(), request.getStatus()));
        }
        output.append(border);
    }

    private <T extends Comparable<T>> ArrayList<T> copyToArrayList(SortedArrayList<T> sortedResult) {
        ArrayList<T> result = new ArrayList<>();
        for (T entry : sortedResult) {
            result.add(entry);
        }
        return result;
    }
}
