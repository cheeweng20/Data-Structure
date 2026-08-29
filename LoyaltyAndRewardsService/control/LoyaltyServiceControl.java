package LoyaltyAndRewardsService.control;

import LoyaltyAndRewardsService.dao.MemberDao;
import LoyaltyAndRewardsService.dao.PointTransactionDao;
import LoyaltyAndRewardsService.dao.RequestDao;
import LoyaltyAndRewardsService.entity.Member;
import LoyaltyAndRewardsService.entity.PointTransaction;
import LoyaltyAndRewardsService.entity.PromotionOffer;
import LoyaltyAndRewardsService.entity.RedemptionRequest;
import LoyaltyAndRewardsService.entity.Tier;
import LoyaltyAndRewardsService.reporting.LoyaltyReportFormatter;
import LoyaltyAndRewardsService.reporting.ReportPdfExporter;
import LoyaltyAndRewardsService.reporting.ReportPdfExporter.ChartType;
import VIPPriorityRoomAllocation.dao.ReservationDAO;
import VIPPriorityRoomAllocation.entity.Reservation;
import VIPPriorityRoomAllocation.entity.ReservationStatus;
import common.utility.Validation;
import adt.LinkedQueue;
import adt.ListInterface;
import adt.QueueInterface;
import adt.SortedArrayList;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

/**
 * Initializes and coordinates the Loyalty and Rewards subsystem.
 *
 * @author Chee Weng
 */
public class LoyaltyServiceControl {
    private static final int POINTS_PER_RINGGIT = 1;
    private static final int MINIMUM_HISTORY_SIZE = 2;
    private static final double HISTORY_POINT_MULTIPLIER = 1.5;
    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_APPROVED = "Approved";
    private static final String STATUS_REJECTED = "Rejected";
    private static final String STATUS_REJECTED_INSUFFICIENT_POINTS = "Rejected - insufficient points";

    private final MemberDao memberDao;
    private final PointTransactionDao pointTransactionDao;
    private final RequestDao requestDao;
    private final ReservationDAO reservationDao;
    private ListInterface<Member> memberList;
    private ListInterface<PointTransaction> transactionList;
    private QueueInterface<RedemptionRequest> requestQueue;
    private ListInterface<RedemptionRequest> requestHistory;
    private int nextRequestNumber;

    public LoyaltyServiceControl() {
        this(new MemberDao(), new PointTransactionDao(), new RequestDao(),
                new ReservationDAO());
    }

    public LoyaltyServiceControl(MemberDao memberDao,
            PointTransactionDao pointTransactionDao, RequestDao requestDao) {
        this(memberDao, pointTransactionDao, requestDao, new ReservationDAO());
    }

    public LoyaltyServiceControl(MemberDao memberDao,
            PointTransactionDao pointTransactionDao, RequestDao requestDao,
            ReservationDAO reservationDao) {
        this.memberDao = memberDao;
        this.pointTransactionDao = pointTransactionDao;
        this.requestDao = requestDao;
        this.reservationDao = reservationDao;

        memberList = memberDao.retrieveFromFile(); // load members from CSV file
        transactionList = pointTransactionDao.retrieveFromFile(); // load point transactions from CSV file
        requestQueue = new LinkedQueue<>();
        requestHistory = requestDao.retrieveFromFile(); // load redemption requests from CSV file
        nextRequestNumber = 1;
        rebuildPendingRequestQueue(); // add pending requests back into the queue

        expirePointsAndSave();
    }

    private void saveMembersAndTransactions() {
        saveMembers();
        saveTransactions();
    }

    // return membership tier based on total points
    public String getTierName(Member member) {
        return member == null ? "Unknown"
                : Tier.fromPoints(member.getTotalExpenses()).getTierLevel();
    }

    public int getMemberCount() {
        return memberList.getNumberOfEntries();
    }

    public boolean isMemberEmpty() {
        return memberList.isEmpty();
    }

    // create a new loyalty member
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
        return awardPointsForCompletedStay(memberId, reservationId, bookingAmount, null);
    }

    // award base points or promotion points after a completed stay
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

        PromotionOffer offer = analyzePromotion(memberId, sourceId);
        double multiplier = isPromotionApplicable(offer, checkInDate)
                ? offer.getPointMultiplier() : 1.0;
        double calculatedPoints = Math.floor(
                bookingAmount * POINTS_PER_RINGGIT * multiplier);
        if (calculatedPoints <= 0 || calculatedPoints > Integer.MAX_VALUE) {
            return -1;
        }

        int points = (int) calculatedPoints;
        int expenseAmount = (int) Math.floor(bookingAmount);
        if (member.getPoint() > Integer.MAX_VALUE - points
                || member.getTotalExpenses() > Integer.MAX_VALUE - expenseAmount) {
            return -1;
        }

        member.setPoint(member.getPoint() + points);
        member.addTotalExpenses(expenseAmount);
        addTransaction(memberId, points, sourceId);
        saveMembersAndTransactions();
        return points;
    }

    public Member getMemberById(String memberId) {
        return findMemberById(memberId);
    }

    private Member findMemberById(String memberId) {
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

    public PromotionOffer getBookingPromotionOffer(String memberId) {
        return analyzePromotion(memberId, null);
    }

    public PromotionOffer getAppliedBookingPromotionOffer(String memberId,
            String reservationId, LocalDate checkInDate) {
        PromotionOffer offer = analyzePromotion(memberId, reservationId);
        return isPromotionApplicable(offer, checkInDate) ? offer : null;
    }

    private PromotionOffer analyzePromotion(String memberId,
            String excludedConfirmationNumber) {
        int historySize = 0;
        int weekendStays = 0;
        for (Reservation reservation : reservationDao.retrieveFromFile()) {
            if (!belongsToMember(reservation, memberId)
                    || isExcluded(reservation, excludedConfirmationNumber)
                    || !isHistoricalStay(reservation)) {
                continue;
            }
            historySize++;
            if (isWeekendStay(reservation)) {
                weekendStays++;
            }
        }

        if (historySize < MINIMUM_HISTORY_SIZE) {
            return new PromotionOffer(historySize, weekendStays, 1.0);
        }

        if (weekendStays * 2 == historySize) {
            return new PromotionOffer(historySize, weekendStays, 1.0);
        }

        return new PromotionOffer(historySize, weekendStays, HISTORY_POINT_MULTIPLIER);
    }

    private boolean isPromotionApplicable(PromotionOffer offer, LocalDate checkInDate) {
        if (checkInDate == null || offer.getPointMultiplier() <= 1.0) {
            return false;
        }
        boolean weekend = isWeekend(checkInDate);
        boolean weekendPreferred = offer.getWeekendStayCount() * 2
                > offer.getCompletedStayCount();
        return weekendPreferred ? weekend : !weekend;
    }

    public int getTierUpgradePointsRemaining(String memberId) {
        Member member = getMemberById(memberId);
        if (member == null) {
            return -1;
        }
        int totalExpenses = member.getTotalExpenses();
        Tier nextTier = Tier.fromPoints(totalExpenses).getNextTier();
        if (nextTier == null) {
            return -1;
        }
        return nextTier.getMinPoint() - totalExpenses;
    }

    private void saveMembers() {
        memberDao.saveToFile(memberList);
    }

    // add a point transaction after points are awarded
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

    public String getExpiringPointsReport(int withinDays) {
        return LoyaltyReportFormatter.buildExpiringPointsReport(generateExpiringReport(withinDays));
    }

    public String getPointsTransactionReport(LocalDate startDate, LocalDate endDate) {
        return LoyaltyReportFormatter.buildPointsTransactionReport(startDate, endDate,
                generateTransactionReport(startDate, endDate));
    }

    public boolean isValidMemberName(String memberName) {
        return Validation.isValidMemberName(memberName);
    }

    public boolean isValidPassport(String passport) {
        return Validation.isValidPassport(passport);
    }

    public boolean isValidPhoneNumber(String phoneNumber) {
        return Validation.isValidPhoneNumber(phoneNumber);
    }

    public Path exportReport(String title, String report, String chartType) throws IOException {
        return ReportPdfExporter.export(title, report, ChartType.valueOf(chartType));
    }

    public boolean openReport(Path pdfPath) throws IOException {
        return ReportPdfExporter.open(pdfPath);
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

    // remove points that have passed their expiry date
    /**
     * Expires unused transaction points after their expiry date and deducts the
     * available amount from the member's spendable balance without changing the
     * member's achieved tier.
     *
     * @return the total number of unused transaction points that expired
     */
    private int expirePointsAndSave() {
        int expiredPoints = expirePoints(LocalDate.now());
        if (expiredPoints > 0) {
            saveMembersAndTransactions();
        }
        return expiredPoints;
    }

    public SortedArrayList<PointTransaction> generateMemberExpiringReport(
            String memberId, int withinDays) {
        SortedArrayList<PointTransaction> sortedResult = new SortedArrayList<>();
        if (memberId == null || memberId.isBlank() || withinDays < 0) {
            return sortedResult;
        }

        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(withinDays);
        for (PointTransaction transaction : transactionList) {
            if (belongsToMember(transaction, memberId.trim())
                    && expiresWithin(transaction, today, cutoff)) {
                sortedResult.add(transaction);
            }
        }
        return sortedResult;
    }

    private void saveTransactions() {
        pointTransactionDao.saveToFile(transactionList);
    }

    private String generateTransactionId() {
        int highestNumber = 0;
        for (PointTransaction transaction : transactionList) {
            highestNumber = Math.max(highestNumber,
                    parseNumericId(transaction.getTransactionId(), "TS"));
        }
        return String.format("TS%03d", highestNumber + 1);
    }

    // submit and process member point-payment requests
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

        if (!isValidPointPaymentReservation(memberId, confirmationNumber,
                pointsRequested, "UNPAID")) {
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
        boolean reservationIsPending = isValidPointPaymentReservation(
                processed.getMemberId(), processed.getConfirmationNumber(),
                processed.getPointsRequested(), "POINTS_PENDING");

        if (approve) {
            Member member = getMemberById(processed.getMemberId());
            if (!reservationIsPending) {
                processed.setStatus(STATUS_REJECTED);
            } else if (member == null || member.getPoint() < processed.getPointsRequested()) {
                processed.setStatus(STATUS_REJECTED_INSUFFICIENT_POINTS);
                resetReservationPayment(processed);
            } else if (updateReservationPayment(processed, "Member Points", "PAID")
                    && applyApprovedRedemption(
                            processed.getMemberId(), processed.getPointsRequested())) {
                processed.setStatus(STATUS_APPROVED);
            } else {
                processed.setStatus(STATUS_REJECTED);
            }
        } else {
            processed.setStatus(STATUS_REJECTED);
            if (reservationIsPending) {
                resetReservationPayment(processed);
            }
        }

        saveRequests();

        if (STATUS_APPROVED.equalsIgnoreCase(processed.getStatus())) {
            saveMembersAndTransactions();
        }

        return processed;
    }

    // update reservation payment status after a redemption decision
    private boolean updateReservationPayment(RedemptionRequest request, String paymentMethod,
            String paymentStatus) {
        ListInterface<Reservation> reservations = reservationDao.retrieveFromFile();
        for (Reservation reservation : reservations) {
            if (reservation.getConfirmationNumber().equalsIgnoreCase(
                    request.getConfirmationNumber())) {
                reservation.setPaymentMethod(paymentMethod);
                reservation.setPaymentStatus(paymentStatus);
                reservationDao.saveToFile(reservations);
                return true;
            }
        }
        return false;
    }

    private void resetReservationPayment(RedemptionRequest request) {
        updateReservationPayment(request, "", "UNPAID");
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

    private void saveRequests() {
        requestDao.saveToFile(requestHistory);
    }

    // helper methods
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

    private PointTransaction findOldestAvailableTransaction(String memberId) {
        PointTransaction oldest = null;
        LocalDate today = LocalDate.now();

        for (PointTransaction current : transactionList) {
            boolean belongsToMember = belongsToMember(current, memberId);
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

    private static boolean belongsToMember(Reservation reservation, String memberId) {
        return reservation.getGuest() != null && memberId != null
                && reservation.getGuest().getGuestId().equalsIgnoreCase(memberId.trim());
    }

    private static boolean belongsToMember(PointTransaction transaction, String memberId) {
        return transaction.getMemberId().equalsIgnoreCase(memberId);
    }

    private static boolean isExcluded(Reservation reservation, String confirmationNumber) {
        return confirmationNumber != null
                && reservation.getConfirmationNumber()
                        .equalsIgnoreCase(confirmationNumber.trim());
    }

    private static boolean isHistoricalStay(Reservation reservation) {
        return reservation.getStatus() == ReservationStatus.CHECKED_OUT;
    }

    private static boolean isWeekendStay(Reservation reservation) {
        return isWeekend(reservation.getCheckInDate());
    }

    private static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;
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

            Member member = findMemberById(transaction.getMemberId());
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

    /** Confirms that a points request still belongs to a payable reservation. */
    private boolean isValidPointPaymentReservation(String memberId, String confirmationNumber,
            int pointsRequested, String paymentStatus) {
        if (memberId == null || confirmationNumber == null) {
            return false;
        }

        for (Reservation reservation : reservationDao.retrieveFromFile()) {
            if (!reservation.getConfirmationNumber().equalsIgnoreCase(confirmationNumber.trim())) {
                continue;
            }

            if (reservation.getGuest() == null || reservation.getAssignedRoom() == null
                    || !reservation.getGuest().getGuestId().equalsIgnoreCase(memberId.trim())
                    || reservation.getStatus() != ReservationStatus.CONFIRMED
                    || !paymentStatus.equalsIgnoreCase(reservation.getPaymentStatus())) {
                return false;
            }

            long nights = Math.max(1, reservation.getCheckOutDate().toEpochDay()
                    - reservation.getCheckInDate().toEpochDay());
            double amount = nights * reservation.getAssignedRoom().getPricePerNight();
            return calculatePointsForPaymentAmount(amount) == pointsRequested;
        }

        return false;
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

    public int getPendingPointsForMember(String memberId) {
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

    // generate point transaction report within the selected date range
    public SortedArrayList<PointTransaction> generateTransactionReport(
            LocalDate startDate, LocalDate endDate) {
        SortedArrayList<PointTransaction> sortedResult = new SortedArrayList<>(
                new TransactionDateComparator());
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
