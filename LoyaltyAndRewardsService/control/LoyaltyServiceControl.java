package LoyaltyAndRewardsService.control;

import java.time.LocalDate;
import java.util.Iterator;

import adt.LinkedQueue;
import adt.ListInterface;
import adt.QueueInterface;
import adt.SortedArrayList;

import LoyaltyAndRewardsService.dao.MemberDao;
import LoyaltyAndRewardsService.dao.PointTransactionDao;
import LoyaltyAndRewardsService.dao.RequestDao;
import LoyaltyAndRewardsService.entity.Member;
import LoyaltyAndRewardsService.entity.PointTransaction;
import LoyaltyAndRewardsService.entity.RedemptionRequest;
import LoyaltyAndRewardsService.entity.Tier;
import common.utility.Validation;

/**
 * Initializes and coordinates the Loyalty and Rewards subsystem.
 *
 * @author Chee Weng
 */
public class LoyaltyServiceControl {
    private static final int POINTS_PER_RINGGIT = 1;
    private static final int TIER_UPGRADE_ALERT_POINTS = 50;
    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_APPROVED = "Approved";
    private static final String STATUS_REJECTED = "Rejected";
    private static final String STATUS_REJECTED_INSUFFICIENT_POINTS = "Rejected - insufficient points";

    public record ExpiringPointSummary(int transactionCount, int pointTotal) {
    }

    private final MemberDao memberDao;
    private final PointTransactionDao pointTransactionDao;
    private final RequestDao requestDao;
    private final MemberPromotionAnalyzer promotionAnalyzer;
    private ListInterface<Member> memberList;
    private ListInterface<PointTransaction> transactionList;
    private QueueInterface<RedemptionRequest> requestQueue;
    private ListInterface<RedemptionRequest> requestHistory;
    private int nextRequestNumber;
    private int recentlyExpiredPointTotal;

    // Initialization and persistence

    public LoyaltyServiceControl() {
        this(new MemberDao(), new PointTransactionDao(), new RequestDao(),
                new MemberPromotionAnalyzer());
    }

    public LoyaltyServiceControl(MemberDao memberDao,
            PointTransactionDao pointTransactionDao, RequestDao requestDao) {
        this(memberDao, pointTransactionDao, requestDao, new MemberPromotionAnalyzer());
    }

    public LoyaltyServiceControl(MemberDao memberDao,
            PointTransactionDao pointTransactionDao, RequestDao requestDao,
            MemberPromotionAnalyzer promotionAnalyzer) {
        this.memberDao = memberDao;
        this.pointTransactionDao = pointTransactionDao;
        this.requestDao = requestDao;
        this.promotionAnalyzer = promotionAnalyzer;

        memberList = memberDao.retrieveFromFile();
        transactionList = pointTransactionDao.retrieveFromFile();
        requestQueue = new LinkedQueue<>();
        requestHistory = requestDao.retrieveFromFile();
        nextRequestNumber = 1;
        rebuildPendingRequestQueue();

        recentlyExpiredPointTotal = expirePointsAndSave();
    }

    public void saveAll() {
        saveMembersAndTransactions();
        saveRequests();
    }

    private void saveMembersAndTransactions() {
        saveMembers();
        saveTransactions();
    }

    // Tier operations

    public String getTierName(Member member) {
        return member == null ? "Unknown"
                : Tier.fromPoints(member.getTotalExpenses()).getTierLevel();
    }

    // Member operations

    public int getMemberCount() {
        return memberList.getNumberOfEntries();
    }

    public boolean isMemberEmpty() {
        return memberList.isEmpty();
    }

    public String createMember(String name, String passport, String phoneNumber) {
        String memberId = generateMemberId();
        memberList.add(new Member(memberId, name, passport, phoneNumber, 0, 0));
        saveMembers();
        return memberId;
    }

    public boolean isPassportAvailable(String passport) {
        if (Validation.isBlank(passport)) {
            return false;
        }
        for (Member member : memberList) {
            if (member.getPassport().equalsIgnoreCase(passport.trim())) {
                return false;
            }
        }
        return true;
    }

    public boolean isPhoneNumberAvailable(String phoneNumber) {
        if (Validation.isBlank(phoneNumber)) {
            return false;
        }
        String normalizedPhone = Validation.normalizePhoneNumber(phoneNumber);
        for (Member member : memberList) {
            if (Validation.normalizePhoneNumber(member.getPhoneNumber())
                    .equals(normalizedPhone)) {
                return false;
            }
        }
        return true;
    }

    public Member getMemberEntry(int position) {
        return memberList.getEntry(position);
    }

    private boolean applyApprovedRedemption(String memberId, int pointsToDeduct) {
        Member member = getMemberById(memberId);
        if (member == null || pointsToDeduct <= 0 || member.getPoint() < pointsToDeduct) {
            return false;
        }

        member.setPoint(member.getPoint() - pointsToDeduct);
        deductPointsFromOldestTransactions(memberId, pointsToDeduct);
        return true;
    }

    public int awardPointsForCompletedStay(String memberId, String reservationId,
            double bookingAmount) {
        return awardPointsForCompletedStay(
                memberId, reservationId, bookingAmount, null);
    }

    /** Awards base or history-promotion points for one completed stay. */
    public int awardPointsForCompletedStay(String memberId, String reservationId,
            double bookingAmount, LocalDate checkInDate) {
        Member member = getMemberById(memberId);
        if (member == null || reservationId == null || reservationId.isBlank()
                || !Double.isFinite(bookingAmount) || bookingAmount <= 0) {
            return -1;
        }

        String sourceId = reservationId.trim();
        if (hasTransactionSource(sourceId)) {
            return 0;
        }

        MemberPromotionAnalyzer.PromotionOffer offer =
                promotionAnalyzer.analyze(memberId, sourceId);
        double multiplier = offer.appliesTo(checkInDate)
                ? offer.pointMultiplier() : 1.0;
        double calculatedPoints = Math.floor(
                bookingAmount * POINTS_PER_RINGGIT * multiplier);
        if (calculatedPoints <= 0 || calculatedPoints > Integer.MAX_VALUE) {
            return -1;
        }

        int points = (int) calculatedPoints;
        if (member.getPoint() > Integer.MAX_VALUE - points
                || member.getTotalExpenses() > Integer.MAX_VALUE - points) {
            return -1;
        }

        member.setPoint(member.getPoint() + points);
        member.addTotalExpenses(points);
        addTransaction(memberId, points, sourceId);
        saveMembersAndTransactions();
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

    private String generateMemberId() {
        int highestNumber = 0;
        for (Member member : memberList) {
            highestNumber = Math.max(highestNumber, parseNumericId(member.getMemberId(), "M"));
        }
        return String.format("M%03d", highestNumber + 1);
    }

    public String generatePersonalizedPromotion(String memberId) {
        refreshRequestState();
        Member member = getMemberById(memberId);
        if (member == null) {
            return "Member Not Found";
        }

        StringBuilder promotion = new StringBuilder();
        int totalExpenses = member.getTotalExpenses();
        Tier nextTier = Tier.fromPoints(totalExpenses).getNextTier();

        if (nextTier == null) {
            promotion.append("Tier progress: You have reached the highest membership tier.\n");
        } else {
            promotion.append("Tier progress: Spend RM")
                    .append(nextTier.getMinPoint() - totalExpenses)
                    .append(" more to reach ")
                    .append(nextTier.getTierLevel())
                    .append(".\n");
        }

        appendPointPaymentStatus(promotion, member);
        appendExpiringPointReminder(promotion, member.getMemberId(), 30);
        appendHistoryBasedPromotion(promotion, member.getMemberId());

        return promotion.toString();
    }

    /** Shows a tier alert only when the member is close to the next threshold. */
    public String generateTierUpgradeNotification(String memberId) {
        Member member = getMemberById(memberId);
        if (member == null) {
            return "";
        }
        int totalExpenses = member.getTotalExpenses();
        Tier nextTier = Tier.fromPoints(totalExpenses).getNextTier();
        if (nextTier == null) {
            return "";
        }
        int remainingPoints = nextTier.getMinPoint() - totalExpenses;
        return remainingPoints > 0 && remainingPoints <= TIER_UPGRADE_ALERT_POINTS
                ? "Tier upgrade alert: Spend RM" + remainingPoints
                        + " more to reach "
                        + nextTier.getTierLevel() + "."
                : "";
    }

    public void saveMembers() {
        memberDao.saveToFile(memberList);
    }

    // Point transaction operations

    private void addTransaction(String memberId, int points, String sourceReference) {
        LocalDate earnedDate = LocalDate.now();
        LocalDate expiryDate = earnedDate.plusYears(1);
        String transactionId = generateTransactionId();
        transactionList.add(new PointTransaction(transactionId, memberId, points, points,
                earnedDate, expiryDate, sourceReference));
    }

    public SortedArrayList<PointTransaction> generateExpiringReport(int withinDays) {
        SortedArrayList<PointTransaction> sortedResult = new SortedArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(withinDays);

        for (PointTransaction current : transactionList) {
            if (expiresWithin(current, today, cutoff)) {
                sortedResult.add(current);
            }
        }

        return sortedResult;
    }

    private void deductPointsFromOldestTransactions(String memberId, int pointsToDeduct) {
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
            saveMembersAndTransactions();
        }
        return expiredPoints;
    }

    public int getRecentlyExpiredPointTotal() {
        return recentlyExpiredPointTotal;
    }

    public ExpiringPointSummary getExpiringPointSummary(int withinDays) {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(withinDays);
        int transactionCount = 0;
        int pointTotal = 0;

        for (PointTransaction transaction : transactionList) {
            if (expiresWithin(transaction, today, cutoff)) {
                transactionCount++;
                pointTotal += transaction.getPointsRemaining();
            }
        }

        return new ExpiringPointSummary(transactionCount, pointTotal);
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

    // Redemption request operations

    public int calculatePointsForPaymentAmount(double paymentAmount) {
        double points = Math.ceil(paymentAmount * POINTS_PER_RINGGIT);
        return points > 0 && points <= Integer.MAX_VALUE ? (int) points : -1;
    }

    public int getAvailablePointsForPayment(String memberId) {
        return getRedeemablePoints(memberId);
    }

    public boolean submitPointPaymentRequest(String memberId, String confirmationNumber,
            double paymentAmount) {
        int pointsRequested = calculatePointsForPaymentAmount(paymentAmount);
        if (!Validation.isValidConfirmationNumber(confirmationNumber)
                || pointsRequested <= 0) {
            return false;
        }

        expirePointsAndSave();

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
            if (applyApprovedRedemption(
                    processed.getMemberId(), processed.getPointsRequested())) {
                processed.setStatus(STATUS_APPROVED);
            } else {
                processed.setStatus(STATUS_REJECTED_INSUFFICIENT_POINTS);
            }
        } else {
            processed.setStatus(STATUS_REJECTED);
        }

        saveRequests();

        if (STATUS_APPROVED.equalsIgnoreCase(processed.getStatus())) {
            saveMembersAndTransactions();
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

    // Shared ID helpers

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

    // Point transaction helpers

    private PointTransaction findOldestAvailableTransaction(String memberId) {
        PointTransaction oldest = null;
        LocalDate today = LocalDate.now();

        for (PointTransaction current : transactionList) {
            boolean belongsToMember = current.getMemberId().equalsIgnoreCase(memberId);
            if (belongsToMember && hasUnexpiredPoints(current, today)
                    && (oldest == null || current.compareTo(oldest) < 0)) {
                oldest = current;
            }
        }

        return oldest;
    }

    private boolean hasTransactionSource(String sourceReference) {
        String normalizedSource = sourceReference.trim();
        for (PointTransaction transaction : transactionList) {
            if (!transaction.getSourceReference().isBlank()
                    && transaction.getSourceReference().equalsIgnoreCase(normalizedSource)) {
                return true;
            }
        }
        return false;
    }

    private void appendPointPaymentStatus(StringBuilder promotion, Member member) {
        int pendingPoints = getPendingPointsForMember(member.getMemberId());
        if (pendingPoints <= 0) {
            return;
        }

        int redeemablePoints = getRedeemablePoints(member.getMemberId());
        promotion.append("Points on hold: ").append(pendingPoints)
                .append(" points reserved by pending request(s); ")
                .append(redeemablePoints).append(" points remain available.\n");
    }

    private void appendExpiringPointReminder(StringBuilder promotion, String memberId,
            int withinDays) {
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(withinDays);
        LocalDate earliestExpiryDate = null;
        int expiringPoints = 0;

        for (PointTransaction transaction : transactionList) {
            boolean belongsToMember = transaction.getMemberId().equalsIgnoreCase(memberId);
            if (belongsToMember && expiresWithin(transaction, today, cutoff)) {
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
        }
    }

    private void appendHistoryBasedPromotion(StringBuilder promotion, String memberId) {
        MemberPromotionAnalyzer.PromotionOffer offer = promotionAnalyzer.analyze(memberId);
        promotion.append("History-based promotion: ")
                .append(offer.message()).append("\n");
    }

    private int expirePoints(LocalDate today) {
        int totalExpired = 0;

        for (PointTransaction transaction : transactionList) {
            if (!hasExpiredPoints(transaction, today)) {
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

    private static boolean hasUnexpiredPoints(
            PointTransaction transaction, LocalDate today) {
        return transaction.getPointsRemaining() > 0
                && !transaction.getExpiryDate().isBefore(today);
    }

    private static boolean hasExpiredPoints(
            PointTransaction transaction, LocalDate today) {
        return transaction.getPointsRemaining() > 0
                && transaction.getExpiryDate().isBefore(today);
    }

    private static boolean expiresWithin(
            PointTransaction transaction, LocalDate today, LocalDate cutoff) {
        return hasUnexpiredPoints(transaction, today)
                && !transaction.getExpiryDate().isAfter(cutoff);
    }

    // Redemption request helpers

    private int getRedeemablePoints(String memberId) {
        Member member = getMemberById(memberId);
        return member == null ? -1
                : Math.max(member.getPoint() - getPendingPointsForMember(memberId), 0);
    }

    private void rebuildPendingRequestQueue() {
        for (RedemptionRequest request : requestHistory) {
            updateNextRequestNumber(request.getRequestId());
            if (STATUS_PENDING.equalsIgnoreCase(request.getStatus())) {
                requestQueue.enqueue(request);
            }
        }
    }

    private void refreshRequestState() {
        requestHistory = requestDao.retrieveFromFile();
        requestQueue = new LinkedQueue<>();
        nextRequestNumber = 1;
        rebuildPendingRequestQueue();
    }

    private boolean createPendingRequest(String memberId, String confirmationNumber,
            int pointsRequested) {
        Member currentMember = getMemberById(memberId);
        if (currentMember == null || hasActiveRequestForReservation(confirmationNumber)) {
            return false;
        }

        int availablePoints = currentMember.getPoint() - getPendingPointsForMember(memberId);
        if (availablePoints < pointsRequested) {
            return false;
        }

        String requestId = generateRequestId();
        RedemptionRequest request = new RedemptionRequest(
                requestId, memberId, confirmationNumber,
                pointsRequested, LocalDate.now(), STATUS_PENDING);
        requestQueue.enqueue(request);
        requestHistory.add(request);
        return true;
    }

    private boolean hasActiveRequestForReservation(String confirmationNumber) {
        String normalizedNumber = confirmationNumber.trim();
        for (RedemptionRequest request : requestHistory) {
            boolean sameReservation = request.getConfirmationNumber()
                    .equalsIgnoreCase(normalizedNumber);
            boolean active = STATUS_PENDING.equalsIgnoreCase(request.getStatus())
                    || STATUS_APPROVED.equalsIgnoreCase(request.getStatus());
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
            if (request.getMemberId().equalsIgnoreCase(memberId)) {
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

    // Reporting

    public SortedArrayList<PointTransaction> generateTransactionReport(
            LocalDate startDate, LocalDate endDate) {
        SortedArrayList<PointTransaction> sortedResult = new SortedArrayList<>(
                (left, right) -> left.getEarnedDate().compareTo(right.getEarnedDate()));
        for (PointTransaction current : transactionList) {
            if (isWithinRange(current.getEarnedDate(), startDate, endDate)) {
                sortedResult.add(current);
            }
        }
        return sortedResult;
    }

    private boolean isWithinRange(LocalDate date, LocalDate start, LocalDate end) {
        return !date.isBefore(start) && !date.isAfter(end);
    }

}
