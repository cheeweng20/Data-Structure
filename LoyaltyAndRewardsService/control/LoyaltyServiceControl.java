package LoyaltyAndRewardsService.control;

import java.time.LocalDate;
import java.util.Iterator;

import adt.ArrayList;
import adt.LinkedList;
import adt.LinkedQueue;
import adt.ListInterface;
import adt.QueueInterface;
import adt.SortedArrayList;

import LoyaltyAndRewardsService.dao.MemberDao;
import LoyaltyAndRewardsService.dao.PointTransactionDao;
import LoyaltyAndRewardsService.dao.RequestDao;
import LoyaltyAndRewardsService.dao.TierDao;
import LoyaltyAndRewardsService.entity.Member;
import LoyaltyAndRewardsService.entity.PointTransaction;
import LoyaltyAndRewardsService.entity.RedemptionRequest;
import LoyaltyAndRewardsService.entity.Tier;

/**
 * Initializes and coordinates the Loyalty and Rewards subsystem.
 *
 * @author Chee Weng
 */
public class LoyaltyServiceControl {
    private static final int POINTS_PER_RINGGIT = 1;

    private final MemberDao memberDao;
    private final PointTransactionDao pointTransactionDao;
    private final RequestDao requestDao;
    private final TierDao tierDao;
    private ListInterface<Tier> tierLinkedList;
    private ListInterface<Member> memberList;
    private ListInterface<PointTransaction> transactionList;
    private QueueInterface<RedemptionRequest> requestQueue;
    private ListInterface<RedemptionRequest> requestHistory;
    private int nextRequestNumber;
    private int recentlyExpiredPointTotal;

    // ==================== Initialization and Persistence Control
    // ====================

    public LoyaltyServiceControl() {
        this(new MemberDao(), new PointTransactionDao(), new RequestDao(), new TierDao());
    }

    public LoyaltyServiceControl(MemberDao memberDao,
            PointTransactionDao pointTransactionDao, RequestDao requestDao,
            TierDao tierDao) {
        this.memberDao = memberDao;
        this.pointTransactionDao = pointTransactionDao;
        this.requestDao = requestDao;
        this.tierDao = tierDao;

        tierLinkedList = tierDao.retrieveFromFile();
        organizeTierRanges();
        memberList = memberDao.retrieveFromFile();
        transactionList = pointTransactionDao.retrieveFromFile();
        requestQueue = new LinkedQueue<>();
        requestHistory = requestDao.retrieveFromFile();
        nextRequestNumber = 1;
        rebuildPendingRequestQueue();

        recentlyExpiredPointTotal = expirePointsAndSave();
    }

    public void saveAll() {
        saveMembers();
        saveTransactions();
        saveRequests();
    }

    // ==================== Tier Control ====================

    public Iterator<Tier> getTierIterator() {
        return tierLinkedList.iterator();
    }

    public Tier getExistTierById(String tierId) {
        for (int i = 1; i <= tierLinkedList.getNumberOfEntries(); i++) {
            Tier current = tierLinkedList.getEntry(i);
            if (current.getTierId().equalsIgnoreCase(tierId)) {
                return current;
            }
        }
        return null;
    }

    public String getTierIdByPoint(int point) {
        Tier matchingTier = null;

        for (int i = 1; i <= tierLinkedList.getNumberOfEntries(); i++) {
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
        for (int i = 1; i <= tierLinkedList.getNumberOfEntries(); i++) {
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
        for (int i = 1; i <= tierLinkedList.getNumberOfEntries(); i++) {
            if (tierLinkedList.getEntry(i).getTierId().equalsIgnoreCase(tierId)) {
                return i < tierLinkedList.getNumberOfEntries()
                        ? tierLinkedList.getEntry(i + 1)
                        : null;
            }
        }
        return null;
    }

    public int getMinimumPoint(String tierId) {
        Tier tier = getExistTierById(tierId);
        return tier == null ? -1 : tier.getMinPoint();
    }

    // ==================== Member Control ====================

    public int getMemberCount() {
        return memberList.getNumberOfEntries();
    }

    public boolean isMemberEmpty() {
        return memberList.isEmpty();
    }

    private void addMember(Member member) {
        promoteTierIfEligible(member);
        if (member.getLastNotifiedTierId() == null
                || member.getLastNotifiedTierId().isBlank()) {
            member.setLastNotifiedTierId(member.getTierId());
        }
        memberList.add(member);
    }

    public String createMember(String name, String passport, String phoneNumber) {
        String memberId = generateMemberId();
        String initialTierId = getTierIdByPoint(0);
        addMember(new Member(memberId, name, passport, phoneNumber, 0, 0,
                initialTierId, initialTierId));
        saveMembers();
        return memberId;
    }

    public boolean findMember(String memberId) {
        return getMemberById(memberId) != null;
    }

    public boolean isMemberNameAvailable(String memberName, String excludedMemberId) {
        if (memberName == null || memberName.isBlank()) {
            return false;
        }
        for (Member member : memberList) {
            boolean isExcludedMember = excludedMemberId != null
                    && member.getMemberId().equalsIgnoreCase(excludedMemberId);
            if (!isExcludedMember && member.getName().equalsIgnoreCase(memberName.trim())) {
                return false;
            }
        }
        return true;
    }

    public Member getMemberEntry(int position) {
        return memberList.getEntry(position);
    }

    public boolean removeMember(String memberId) {
        if (getPendingPointsForMember(memberId) > 0) {
            return false;
        }
        for (int i = 1; i <= memberList.getNumberOfEntries(); i++) {
            Member member = memberList.getEntry(i);
            if (member.getMemberId().equalsIgnoreCase(memberId)) {
                memberList.remove(i);
                saveMembers();
                return true;
            }
        }
        return false;
    }

    public boolean updateMember(String memberId, String name, String passport,
            String phoneNumber) {
        Member member = getMemberById(memberId);
        if (member == null) {
            return false;
        }

        member.setName(name);
        member.setPassport(passport);
        member.setPhoneNumber(phoneNumber);
        saveMembers();
        return true;
    }

    private int deductPointsForApprovedRedemption(String memberId, int pointsToDeduct) {
        Member member = getMemberById(memberId);
        if (member == null || pointsToDeduct <= 0 || member.getPoint() < pointsToDeduct) {
            return -1;
        }

        int newPoint = member.getPoint() - pointsToDeduct;
        member.setPoint(newPoint);
        return newPoint;
    }

    public int awardPointsForCompletedStay(String memberId, String reservationId,
            double bookingAmount) {
        Member member = getMemberById(memberId);
        if (member == null || reservationId == null || reservationId.isBlank()
                || !Double.isFinite(bookingAmount) || bookingAmount <= 0) {
            return -1;
        }
        if (hasTransactionSource(reservationId)) {
            return 0;
        }

        long calculatedPoints = (long) Math.floor(bookingAmount * POINTS_PER_RINGGIT);
        if (calculatedPoints <= 0 || calculatedPoints > Integer.MAX_VALUE) {
            return -1;
        }

        int points = (int) calculatedPoints;
        long newPoint = (long) member.getPoint() + points;
        long newLifetimePoints = (long) member.getLifetimePointsEarned() + points;
        if (newPoint > Integer.MAX_VALUE || newLifetimePoints > Integer.MAX_VALUE) {
            return -1;
        }
        member.setPoint((int) newPoint);
        member.addLifetimePointsEarned(points);
        promoteTierIfEligible(member);
        addTransaction(memberId, points, reservationId.trim());
        saveMembers();
        saveTransactions();
        return points;
    }

    public Member getMemberById(String memberId) {
        for (int i = 1; i <= memberList.getNumberOfEntries(); i++) {
            Member member = memberList.getEntry(i);
            if (member.getMemberId().equalsIgnoreCase(memberId)) {
                return member;
            }
        }
        return null;
    }

    public String generateMemberId() {
        int highestNumber = 0;
        for (Member member : memberList) {
            highestNumber = Math.max(highestNumber, parseNumericId(member.getMemberId(), "M"));
        }
        for (PointTransaction transaction : transactionList) {
            highestNumber = Math.max(highestNumber,
                    parseNumericId(transaction.getMemberId(), "M"));
        }
        for (RedemptionRequest request : requestHistory) {
            highestNumber = Math.max(highestNumber,
                    parseNumericId(request.getMemberId(), "M"));
        }
        return String.format("M%03d", highestNumber + 1);
    }

    public ArrayList<Member> generateRankingReport(int minPoint, String targetTierId) {
        SortedArrayList<Member> sortedResult = new SortedArrayList<>((left, right) -> right.compareTo(left));
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

        StringBuilder promotion = new StringBuilder();
        promotion.append("Personalized Promotion for ").append(member.getName()).append("\n")
                .append("Current tier: ").append(getTierNameById(currentTierId)).append("\n")
                .append("Available points: ").append(member.getPoint()).append("\n")
                .append("Lifetime points earned: ")
                .append(member.getLifetimePointsEarned()).append("\n");

        appendPointPaymentStatus(promotion, member);
        appendExpiringPointReminder(promotion, member.getMemberId(), 30);

        if (currentTierId == null) {
            promotion.append("Tier progress: No membership tier is currently configured.");
            return promotion.toString();
        }

        Tier nextTier = getNextTier(currentTierId);
        if (nextTier == null) {
            promotion.append("Tier progress: You have reached the highest membership tier.");
        } else {
            int pointNeeded = Math.max(
                    nextTier.getMinPoint() - member.getLifetimePointsEarned(), 0);
            promotion.append("Tier progress: Earn ").append(pointNeeded)
                    .append(" more qualifying points to reach ")
                    .append(nextTier.getTierLevel()).append(".");
        }
        return promotion.toString();
    }

    public void saveMembers() {
        memberDao.saveToFile(memberList);
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

    private void addTransaction(String memberId, int points, String sourceReference) {
        LocalDate earnedDate = LocalDate.now();
        LocalDate expiryDate = earnedDate.plusYears(1);
        String transactionId = generateTransactionId();
        transactionList.add(new PointTransaction(transactionId, memberId, points, points,
                earnedDate, expiryDate, sourceReference));
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

    private int deductPointsFromOldestTransactions(String memberId, int pointsToDeduct) {
        int remainingToDeduct = pointsToDeduct;

        while (remainingToDeduct > 0) {
            PointTransaction oldest = findOldestAvailableTransaction(memberId);
            if (oldest == null) {
                break;
            }

            int deducted = Math.min(oldest.getPointsRemaining(), remainingToDeduct);
            oldest.setPointsRemaining(oldest.getPointsRemaining() - deducted);
            remainingToDeduct -= deducted;
        }

        return pointsToDeduct - remainingToDeduct;
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
        pointTransactionDao.saveToFile(transactionList);
    }

    public String generateTransactionId() {
        int highestNumber = 0;
        for (PointTransaction transaction : transactionList) {
            highestNumber = Math.max(highestNumber,
                    parseNumericId(transaction.getTransactionId(), "TS"));
        }
        return String.format("TS%03d", highestNumber + 1);
    }

    // ==================== Redemption Request Control ====================

    public int calculatePointsForPaymentAmount(double paymentAmount) {
        if (!Double.isFinite(paymentAmount) || paymentAmount <= 0) {
            return -1;
        }
        long points = (long) Math.ceil(paymentAmount * POINTS_PER_RINGGIT);
        return points > 0 && points <= Integer.MAX_VALUE ? (int) points : -1;
    }

    public int getAvailablePointsForPayment(String memberId) {
        Member member = getMemberById(memberId);
        return member == null ? -1
                : Math.max(member.getPoint() - getPendingPointsForMember(memberId), 0);
    }

    public boolean submitPointPaymentRequest(String memberId, String confirmationNumber,
            double paymentAmount) {
        expirePointsAndSave();
        if (!findMember(memberId)) {
            return false;
        }
        if (confirmationNumber == null || confirmationNumber.isBlank()) {
            return false;
        }
        if (hasActiveRequestForReservation(confirmationNumber)) {
            return false;
        }
        int pointsRequested = calculatePointsForPaymentAmount(paymentAmount);
        if (pointsRequested <= 0) {
            return false;
        }
        if (!createPendingRequest(memberId, confirmationNumber.trim(), pointsRequested)) {
            return false;
        }

        saveRequests();
        return true;
    }

    public RedemptionRequest getNextPendingRequest() {
        return requestQueue.getFront();
    }

    public RedemptionRequest processNextRequestAndSave(boolean approve) {
        expirePointsAndSave();
        RedemptionRequest next = getNextPendingRequest();
        if (next == null) {
            return null;
        }

        RedemptionRequest processed = requestQueue.dequeue();

        if (approve) {
            int newPoint = deductPointsForApprovedRedemption(
                    processed.getMemberId(), processed.getPointsRequested());
            if (newPoint >= 0) {
                deductPointsFromOldestTransactions(
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

        return processed;
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

    public void saveRequests() {
        requestDao.saveToFile(requestHistory);
    }

    // ==================== Report Control ====================

    // ==================== Tier and Member Helpers ====================

    private void organizeTierRanges() {
        sortByMinimumPoint();

        for (int i = 1; i <= tierLinkedList.getNumberOfEntries(); i++) {
            Tier current = tierLinkedList.getEntry(i);
            if (i < tierLinkedList.getNumberOfEntries()) {
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

    private int parseNumericId(String value, String prefix) {
        if (value == null || !value.startsWith(prefix) || value.length() <= prefix.length()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.substring(prefix.length()));
        } catch (NumberFormatException exception) {
            return 0;
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
     * Promotes a member when lifetime earned points qualify for a higher tier.
     * Redeeming or expiring spendable points therefore never reduces tier
     * progress or the member's achieved tier.
     */
    private boolean promoteTierIfEligible(Member member) {
        String eligibleTierId = getTierIdByPoint(member.getLifetimePointsEarned());
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

    private boolean hasTransactionSource(String sourceReference) {
        for (PointTransaction transaction : transactionList) {
            if (!transaction.getSourceReference().isBlank()
                    && transaction.getSourceReference().equalsIgnoreCase(sourceReference.trim())) {
                return true;
            }
        }
        return false;
    }

    private void appendPointPaymentStatus(StringBuilder promotion, Member member) {
        int pendingPoints = getPendingPointsForMember(member.getMemberId());
        int redeemablePoints = Math.max(member.getPoint() - pendingPoints, 0);
        promotion.append("Point-payment redemption: ").append(redeemablePoints)
                .append(" points currently available");
        if (pendingPoints > 0) {
            promotion.append("; ").append(pendingPoints)
                    .append(" points reserved by pending request(s)");
        }
        promotion.append(".\n");
    }

    private void appendExpiringPointReminder(StringBuilder promotion, String memberId,
            int withinDays) {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(withinDays);
        LocalDate earliestExpiryDate = null;
        int expiringPoints = 0;

        for (PointTransaction transaction : transactionList) {
            boolean belongsToMember = transaction.getMemberId().equalsIgnoreCase(memberId);
            boolean expiresWithinPeriod = !transaction.getExpiryDate().isBefore(today)
                    && !transaction.getExpiryDate().isAfter(cutoff);
            if (belongsToMember && transaction.getPointsRemaining() > 0
                    && expiresWithinPeriod) {
                expiringPoints += transaction.getPointsRemaining();
                if (earliestExpiryDate == null
                        || transaction.getExpiryDate().isBefore(earliestExpiryDate)) {
                    earliestExpiryDate = transaction.getExpiryDate();
                }
            }
        }

        if (expiringPoints > 0) {
            promotion.append("Expiry reminder: ").append(expiringPoints)
                    .append(" points will expire within ").append(withinDays)
                    .append(" days; earliest expiry is ").append(earliestExpiryDate)
                    .append(".\n");
        } else {
            promotion.append("Expiry reminder: No points expire within ")
                    .append(withinDays).append(" days.\n");
        }
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

    private void rebuildPendingRequestQueue() {
        for (RedemptionRequest request : requestHistory) {
            updateNextRequestNumber(request.getRequestId());
            if ("Pending".equalsIgnoreCase(request.getStatus())) {
                requestQueue.enqueue(request);
            }
        }
    }

    private boolean createPendingRequest(String memberId, String confirmationNumber,
            int pointsRequested) {
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
                requestId, memberId, confirmationNumber,
                pointsRequested, LocalDate.now(), "Pending");
        requestQueue.enqueue(request);
        requestHistory.add(request);
        return true;
    }

    private boolean hasActiveRequestForReservation(String confirmationNumber) {
        for (RedemptionRequest request : requestHistory) {
            boolean sameReservation = request.getConfirmationNumber()
                    .equalsIgnoreCase(confirmationNumber.trim());
            boolean active = "Pending".equalsIgnoreCase(request.getStatus())
                    || "Approved".equalsIgnoreCase(request.getStatus());
            if (sameReservation && active) {
                return true;
            }
        }
        return false;
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

    // ==================== Report Helpers ====================

    public ArrayList<PointTransaction> generateTransactionReport(
            LocalDate startDate, LocalDate endDate) {
        SortedArrayList<PointTransaction> sortedResult = new SortedArrayList<>(
                (left, right) -> left.getEarnedDate().compareTo(right.getEarnedDate()));
        Iterator<PointTransaction> iterator = transactionList.iterator();
        while (iterator.hasNext()) {
            PointTransaction current = iterator.next();
            if (!current.getEarnedDate().isBefore(startDate)
                    && !current.getEarnedDate().isAfter(endDate)) {
                sortedResult.add(current);
            }
        }
        return copyToArrayList(sortedResult);
    }

    public ArrayList<RedemptionRequest> generateRequestReport(
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

    public int calculateTotalPointsEarned(ArrayList<PointTransaction> transactions) {
        int totalPointsEarned = 0;
        Iterator<PointTransaction> iterator = transactions.iterator();
        while (iterator.hasNext()) {
            totalPointsEarned += iterator.next().getPointsEarned();
        }
        return totalPointsEarned;
    }

    public int countRequestsByStatus(ArrayList<RedemptionRequest> requests, String status) {
        int count = 0;
        Iterator<RedemptionRequest> iterator = requests.iterator();
        while (iterator.hasNext()) {
            String currentStatus = iterator.next().getStatus();
            boolean matchesRejectedGroup = "Rejected".equalsIgnoreCase(status)
                    && !"Pending".equalsIgnoreCase(currentStatus)
                    && !"Approved".equalsIgnoreCase(currentStatus);
            if (currentStatus.equalsIgnoreCase(status) || matchesRejectedGroup) {
                count++;
            }
        }
        return count;
    }

    private <T extends Comparable<T>> ArrayList<T> copyToArrayList(SortedArrayList<T> sortedResult) {
        ArrayList<T> result = new ArrayList<>();
        for (T entry : sortedResult) {
            result.add(entry);
        }
        return result;
    }
}
