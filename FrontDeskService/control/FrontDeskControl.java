package FrontDeskService.control;

import FrontDeskService.dao.LateCheckoutExtensionDAO;
import FrontDeskService.entity.LateCheckoutExtension;
import FrontDeskService.reporting.ReportPdfExporter;
import FrontDeskService.reporting.ReportPdfExporter.ChartType;
import HousekeepingAndTaskLog.control.HousekeepingControl;
import LoyaltyAndRewardsService.control.LoyaltyServiceControl;
import LoyaltyAndRewardsService.entity.PromotionOffer;
import VIPPriorityRoomAllocation.dao.ReservationDAO;
import VIPPriorityRoomAllocation.dao.RoomDAO;
import VIPPriorityRoomAllocation.entity.Reservation;
import VIPPriorityRoomAllocation.entity.ReservationStatus;
import VIPPriorityRoomAllocation.entity.Room;
import VIPPriorityRoomAllocation.entity.Room.RoomStatus;
import adt.ArrayList;
import adt.BinarySearchTree;
import adt.ListInterface;
import adt.SearchTreeInterface;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import common.utility.Validation;

/** Business rules for Front Desk guest service operations.
 * @author Yi Ren
 */
public class FrontDeskControl {
    private static final String MEMBER_POINTS_PAYMENT_METHOD = "Member Points";
    private static final String REPORT_BORDER =
            "+------------+----------------------+--------------+------------------------+--------------+";

    private final ReservationDAO reservationDAO;
    private final RoomDAO roomDAO;
    private final HousekeepingControl housekeepingControl;
    private final LateCheckoutExtensionDAO lateCheckoutExtensionDAO;
    private final LoyaltyServiceControl loyaltyServiceControl;
    private final ListInterface<Reservation> reservations;
    private final ListInterface<Room> rooms;
    private final SearchTreeInterface<String, Reservation> confirmationIndex;
    private int lastAwardedPoints;
    private boolean lastLoyaltyAwardFailed;
    private PromotionOffer lastAppliedPromotionOffer;

    public FrontDeskControl() {
        this(new ReservationDAO(), new RoomDAO(), new HousekeepingControl(false),
                new LateCheckoutExtensionDAO(),
                new LoyaltyServiceControl());
    }

    public FrontDeskControl(ReservationDAO reservationDAO, RoomDAO roomDAO) {
        this(reservationDAO, roomDAO, new HousekeepingControl(false),
                new LateCheckoutExtensionDAO(), new LoyaltyServiceControl());
    }

    public FrontDeskControl(ReservationDAO reservationDAO, RoomDAO roomDAO,
            HousekeepingControl housekeepingControl) {
        this(reservationDAO, roomDAO, housekeepingControl,
                new LateCheckoutExtensionDAO(), new LoyaltyServiceControl());
    }

    /**
     * Creates the control with Front Desk-owned late check-out storage. The
     * extension is intentionally separate from the Reservation module.
     */
    public FrontDeskControl(ReservationDAO reservationDAO, RoomDAO roomDAO,
            HousekeepingControl housekeepingControl,
            LateCheckoutExtensionDAO lateCheckoutExtensionDAO) {
        this(reservationDAO, roomDAO, housekeepingControl,
                lateCheckoutExtensionDAO, new LoyaltyServiceControl());
    }

    /** Full constructor used when module gateways need to be isolated or tested. */
    public FrontDeskControl(ReservationDAO reservationDAO, RoomDAO roomDAO,
            HousekeepingControl housekeepingControl,
            LateCheckoutExtensionDAO lateCheckoutExtensionDAO,
            LoyaltyServiceControl loyaltyServiceControl) {
        this.reservationDAO = reservationDAO;
        this.roomDAO = roomDAO;
        this.housekeepingControl = housekeepingControl;
        this.lateCheckoutExtensionDAO = lateCheckoutExtensionDAO;
        this.loyaltyServiceControl = loyaltyServiceControl;
        reservations = reservationDAO.retrieveFromFile();
        rooms = roomDAO.retrieveFromFile();
        confirmationIndex = new BinarySearchTree<>();

        Iterator<Reservation> iterator = reservations.iterator();
        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            confirmationIndex.add(reservation.getConfirmationNumber(), reservation);
        }
    }

    public Reservation findByConfirmationNumber(String confirmationNumber) {
        return confirmationIndex.search(confirmationNumber);
    }

    /** Returns the active Front Desk late check-out record, if one exists. */
    public LateCheckoutExtension findLateCheckoutExtension(String confirmationNumber) {
        return lateCheckoutExtensionDAO.findByConfirmationNumber(confirmationNumber);
    }

    /**
     * Finds reservations by confirmation number, member ID, or a part of the
     * guest name.
     */
    public ListInterface<Reservation> findMatchingReservations(String searchValue) {
        ListInterface<Reservation> matches = new ArrayList<>();
        if (searchValue == null || searchValue.trim().isEmpty()) {
            return matches;
        }

        String normalizedSearchValue = searchValue.trim().toLowerCase();
        Iterator<Reservation> iterator = reservations.iterator();
        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            boolean confirmationMatches = reservation.getConfirmationNumber()
                    .equalsIgnoreCase(normalizedSearchValue);
            boolean guestIdMatches = reservation.getGuest() != null
                    && reservation.getGuest().getGuestId()
                            .equalsIgnoreCase(normalizedSearchValue);
            boolean guestNameMatches = reservation.getGuest() != null
                    && reservation.getGuest().getFullName().toLowerCase()
                            .contains(normalizedSearchValue);

            if (confirmationMatches || guestIdMatches || guestNameMatches) {
                matches.add(reservation);
            }
        }
        return matches;
    }

    public double calculateBill(Reservation reservation) {
        if (reservation == null || reservation.getAssignedRoom() == null) {
            return 0.0;
        }
        long nights = reservation.getCheckOutDate().toEpochDay()
                - reservation.getCheckInDate().toEpochDay();
        return Math.max(1, nights) * reservation.getAssignedRoom().getPricePerNight();
    }

    /** Returns unpaid reservations ordered by their bill, highest first. */
    public ListInterface<Reservation> getOutstandingBalanceReport() {
        ListInterface<Reservation> result = new ArrayList<>();
        Iterator<Reservation> iterator = reservations.iterator();
        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            if (!"PAID".equalsIgnoreCase(reservation.getPaymentStatus())) {
                result.add(reservation);
            }
        }
        sortReservationsByBillDescending(result);
        return result;
    }

    /**
     * Returns paid room reservations grouped by payment method through their
     * sort order. Unpaid and unassigned reservations are excluded.
     */
    public ListInterface<Reservation> getPaymentMethodReport() {
        ListInterface<Reservation> result = new ArrayList<>();
        Iterator<Reservation> iterator = reservations.iterator();
        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            if (reservation.getAssignedRoom() != null
                    && "PAID".equalsIgnoreCase(reservation.getPaymentStatus())) {
                result.add(reservation);
            }
        }
        sortReservationsByPaymentMethodThenRoom(result);
        return result;
    }

    public String getOutstandingBalanceReportDisplay() {
        ListInterface<Reservation> reportReservations = getOutstandingBalanceReport();
        if (reportReservations.isEmpty()) {
            return "";
        }
        return buildOutstandingBalanceReport(reportReservations);
    }

    public String getPaymentMethodReportDisplay() {
        ListInterface<Reservation> reportReservations = getPaymentMethodReport();
        if (reportReservations.isEmpty()) {
            return "";
        }
        return buildPaymentMethodReport(reportReservations);
    }

    public Path exportReport(String title, String report, String chartType) throws IOException {
        return ReportPdfExporter.export(title, report, ChartType.valueOf(chartType));
    }

    public boolean openReport(Path pdfPath) throws IOException {
        return ReportPdfExporter.open(pdfPath);
    }

    public boolean isValidConfirmationNumber(String confirmationNumber) {
        return Validation.isValidConfirmationNumber(confirmationNumber);
    }

    /**
     * Completes a confirmed reservation's check-in and persists both the room
     * and reservation state.
     */
    public CheckInResult checkInReservation(String confirmationNumber, String paymentMethod) {
        Reservation reservation = findByConfirmationNumber(confirmationNumber);
        if (reservation == null) {
            return CheckInResult.RESERVATION_NOT_FOUND;
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            return CheckInResult.NOT_CONFIRMED;
        }
        if (reservation.getCheckInDate().isAfter(LocalDate.now())) {
            return CheckInResult.CHECK_IN_DATE_NOT_REACHED;
        }
        if (reservation.getAssignedRoom() == null) {
            return CheckInResult.ROOM_NOT_RESERVED;
        }

        Room savedRoom = findRoomByNumber(reservation.getAssignedRoom().getRoomNumber());
        if (savedRoom == null || savedRoom.getStatus() != RoomStatus.RESERVED) {
            return CheckInResult.ROOM_NOT_RESERVED;
        }

        if ("POINTS_PENDING".equalsIgnoreCase(reservation.getPaymentStatus())) {
            return CheckInResult.POINTS_PAYMENT_PENDING;
        }

        boolean alreadyPaid = "PAID".equalsIgnoreCase(reservation.getPaymentStatus());
        if (!alreadyPaid && (paymentMethod == null || paymentMethod.trim().isEmpty())) {
            return CheckInResult.PAYMENT_REQUIRED;
        }

        savedRoom.setStatus(RoomStatus.OCCUPIED);
        reservation.setAssignedRoom(savedRoom);
        if (!alreadyPaid) {
            reservation.setPaymentMethod(paymentMethod);
            reservation.setPaymentStatus("PAID");
        }
        reservation.setStatus(ReservationStatus.CHECKED_IN);
        saveData();
        return CheckInResult.SUCCESS;
    }

    /** Completes a checked-in reservation's check-out. */
    public CheckOutResult checkOutReservation(String confirmationNumber) {
        lastAwardedPoints = 0;
        lastLoyaltyAwardFailed = false;
        lastAppliedPromotionOffer = null;
        Reservation reservation = findByConfirmationNumber(confirmationNumber);
        if (reservation == null) {
            return CheckOutResult.RESERVATION_NOT_FOUND;
        }
        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            return CheckOutResult.NOT_CHECKED_IN;
        }
        if (reservation.getAssignedRoom() == null) {
            return CheckOutResult.ROOM_NOT_OCCUPIED;
        }

        Room savedRoom = findRoomByNumber(reservation.getAssignedRoom().getRoomNumber());
        if (savedRoom == null || savedRoom.getStatus() != RoomStatus.OCCUPIED) {
            return CheckOutResult.ROOM_NOT_OCCUPIED;
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        savedRoom.setStatus(RoomStatus.NEEDS_CLEANING);
        reservation.setAssignedRoom(savedRoom);
        saveData();
        // The operational extension belongs to Front Desk and expires once
        // the guest has completed the real check-out.
        lateCheckoutExtensionDAO.deleteByConfirmationNumber(
                reservation.getConfirmationNumber());
        awardCompletedStayPoints(reservation);
        return CheckOutResult.SUCCESS;
    }

    /** Number of loyalty points awarded by the most recent successful check-out. */
    public int getLastAwardedPoints() {
        return lastAwardedPoints;
    }

    /** Whether check-out succeeded but its loyalty award could not be persisted. */
    public boolean didLastLoyaltyAwardFail() {
        return lastLoyaltyAwardFailed;
    }

    /** Promotion data for the most recent successful loyalty award. */
    public PromotionOffer getLastAppliedPromotionOffer() {
        return lastAppliedPromotionOffer;
    }

    /** Records a temporary late check-out and rollback any early housekeeping task. */
    public LateCheckoutResult extendCheckOut(String confirmationNumber,
            LocalDateTime extendedCheckOutAt, String reason) {
        Reservation reservation = findByConfirmationNumber(confirmationNumber);
        if (reservation == null) {
            return new LateCheckoutResult(LateCheckoutStatus.RESERVATION_NOT_FOUND, null);
        }
        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            return new LateCheckoutResult(LateCheckoutStatus.NOT_CHECKED_IN, null);
        }
        if (reservation.getAssignedRoom() == null) {
            return new LateCheckoutResult(LateCheckoutStatus.ROOM_NOT_OCCUPIED, null);
        }
        if (extendedCheckOutAt == null || !extendedCheckOutAt.isAfter(LocalDateTime.now())) {
            return new LateCheckoutResult(LateCheckoutStatus.INVALID_EXTENDED_CHECK_OUT_TIME,
                    null);
        }
        if (reason == null || reason.trim().isEmpty()) {
            return new LateCheckoutResult(LateCheckoutStatus.REASON_REQUIRED, null);
        }

        Room savedRoom = findRoomByNumber(reservation.getAssignedRoom().getRoomNumber());
        if (savedRoom == null || savedRoom.getStatus() != RoomStatus.OCCUPIED) {
            return new LateCheckoutResult(LateCheckoutStatus.ROOM_NOT_OCCUPIED, null);
        }

        housekeepingControl.removeAutoTaskForReservation(savedRoom.getRoomNumber(),
                reservation.getConfirmationNumber());
        lateCheckoutExtensionDAO.saveOrUpdate(new LateCheckoutExtension(
                reservation.getConfirmationNumber(), extendedCheckOutAt, reason.trim()));
        return new LateCheckoutResult(LateCheckoutStatus.SUCCESS, null);
    }

    private Room findRoomByNumber(String roomNumber) {
        Iterator<Room> iterator = rooms.iterator();
        while (iterator.hasNext()) {
            Room room = iterator.next();
            if (room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                return room;
            }
        }
        return null;
    }

    private void awardCompletedStayPoints(Reservation reservation) {
        if (MEMBER_POINTS_PAYMENT_METHOD.equalsIgnoreCase(reservation.getPaymentMethod())) {
            return;
        }
        if (reservation.getGuest() == null
                || loyaltyServiceControl.getMemberById(
                        reservation.getGuest().getGuestId()) == null) {
            return;
        }

        try {
            PromotionOffer offer = loyaltyServiceControl.getAppliedBookingPromotionOffer(
                    reservation.getGuest().getGuestId(), reservation.getConfirmationNumber(),
                    reservation.getCheckInDate());
            int awarded = loyaltyServiceControl.awardPointsForCompletedStay(
                    reservation.getGuest().getGuestId(),
                    reservation.getConfirmationNumber(), calculateBill(reservation),
                    reservation.getCheckInDate());
            if (awarded > 0) {
                lastAwardedPoints = awarded;
                lastAppliedPromotionOffer = offer;
            } else if (awarded < 0) {
                lastLoyaltyAwardFailed = true;
            }
        } catch (RuntimeException exception) {
            // A completed operational check-out remains valid even if a separate
            // loyalty file is temporarily unavailable. The UI reports the issue.
            lastLoyaltyAwardFailed = true;
        }
    }

    private void sortReservationsByBillDescending(ListInterface<Reservation> list) {
        for (int end = list.getNumberOfEntries(); end > 1; end--) {
            for (int position = 1; position < end; position++) {
                if (calculateBill(list.getEntry(position))
                        < calculateBill(list.getEntry(position + 1))) {
                    Reservation temporary = list.getEntry(position);
                    list.replace(position, list.getEntry(position + 1));
                    list.replace(position + 1, temporary);
                }
            }
        }
    }

    private void sortReservationsByPaymentMethodThenRoom(ListInterface<Reservation> list) {
        for (int end = list.getNumberOfEntries(); end > 1; end--) {
            for (int position = 1; position < end; position++) {
                Reservation current = list.getEntry(position);
                Reservation next = list.getEntry(position + 1);
                int methodComparison = paymentMethodLabel(current)
                        .compareToIgnoreCase(paymentMethodLabel(next));
                boolean roomComesLater = methodComparison == 0
                        && current.getAssignedRoom().getRoomNumber().compareToIgnoreCase(
                                next.getAssignedRoom().getRoomNumber()) > 0;

                if (methodComparison > 0 || roomComesLater) {
                    list.replace(position, next);
                    list.replace(position + 1, current);
                }
            }
        }
    }

    private String paymentMethodLabel(Reservation reservation) {
        String paymentMethod = reservation.getPaymentMethod();
        return paymentMethod == null || paymentMethod.trim().isEmpty()
                ? "Unspecified" : paymentMethod.trim();
    }

    private String buildOutstandingBalanceReport(ListInterface<Reservation> reportReservations) {
        StringBuilder report = createReportTable(
                "Outstanding Balance Report", "Payment Status", "Balance (RM)");
        int total = 0;
        for (Reservation reservation : reportReservations) {
            appendReportRow(report, reservation, roomLabel(reservation),
                    String.valueOf(reservation.getPaymentStatus()));
            total++;
        }
        report.append(REPORT_BORDER).append(System.lineSeparator());
        return report.append("Total outstanding balance records: ").append(total)
                .append(System.lineSeparator()).toString();
    }

    private String buildPaymentMethodReport(ListInterface<Reservation> reportReservations) {
        StringBuilder report = createReportTable(
                "Payment Method Room Report", "Payment Method", "Bill (RM)");
        int total = 0;
        for (Reservation reservation : reportReservations) {
            appendReportRow(report, reservation, roomLabel(reservation),
                    paymentMethodLabel(reservation));
            total++;
        }
        report.append(REPORT_BORDER).append(System.lineSeparator());
        return report.append("Total paid room records: ").append(total)
                .append(System.lineSeparator()).toString();
    }

    private StringBuilder createReportTable(String title, String paymentColumn,
            String amountColumn) {
        StringBuilder report = new StringBuilder("\n--- ").append(title)
                .append(" ---").append(System.lineSeparator());
        report.append(REPORT_BORDER).append(System.lineSeparator());
        report.append(String.format("| %-10s | %-20s | %-12s | %-22s | %-12s |%n",
                "Room", "Guest", "Confirm No.", paymentColumn, amountColumn));
        return report.append(REPORT_BORDER).append(System.lineSeparator());
    }

    private void appendReportRow(StringBuilder report, Reservation reservation,
            String room, String paymentDetails) {
        report.append(String.format("| %-10s | %-20.20s | %-12s | %-22.22s | %-12.2f |%n",
                room, reservation.getGuest().getFullName(),
                reservation.getConfirmationNumber(), paymentDetails, calculateBill(reservation)));
    }

    private String roomLabel(Reservation reservation) {
        return reservation.getAssignedRoom() == null
                ? "Unassigned" : reservation.getAssignedRoom().getRoomNumber();
    }

    private void saveData() {
        reservationDAO.saveToFile(reservations);
        roomDAO.saveToFile(rooms);
    }

    public enum CheckInResult {
        SUCCESS,
        RESERVATION_NOT_FOUND,
        NOT_CONFIRMED,
        CHECK_IN_DATE_NOT_REACHED,
        ROOM_NOT_RESERVED,
        PAYMENT_REQUIRED,
        POINTS_PAYMENT_PENDING
    }

    public enum CheckOutResult {
        SUCCESS,
        RESERVATION_NOT_FOUND,
        NOT_CHECKED_IN,
        ROOM_NOT_OCCUPIED
    }

    public enum LateCheckoutStatus {
        SUCCESS,
        RESERVATION_NOT_FOUND,
        NOT_CHECKED_IN,
        ROOM_NOT_OCCUPIED,
        INVALID_EXTENDED_CHECK_OUT_TIME,
        INVALID_ROOM_READY_TIME,
        REASON_REQUIRED,
        HOUSEKEEPING_NOTIFICATION_FAILED
    }

    /** Result of an extend-check-out request from the Front Desk. */
    public static class LateCheckoutResult {
        private final LateCheckoutStatus status;
        private final String housekeepingTaskId;

        public LateCheckoutResult(LateCheckoutStatus status, String housekeepingTaskId) {
            this.status = status;
            this.housekeepingTaskId = housekeepingTaskId;
        }

        public LateCheckoutStatus getStatus() {
            return status;
        }

        public String getHousekeepingTaskId() {
            return housekeepingTaskId;
        }

        public boolean isSuccessful() {
            return status == LateCheckoutStatus.SUCCESS;
        }
    }
}
