package FrontDeskService.control;

import FrontDeskService.dao.LateCheckoutExtensionDAO;
import FrontDeskService.entity.LateCheckoutExtension;
import HousekeepingAndTaskLog.control.HousekeepingControl;
import HousekeepingAndTaskLog.entity.HousekeepingTask;
import LoyaltyAndRewardsService.dao.RequestDao;
import LoyaltyAndRewardsService.control.LoyaltyServiceControl;
import LoyaltyAndRewardsService.entity.RedemptionRequest;
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
import java.util.Iterator;

/** Business rules for Front Desk guest service operations.
 * @author Yi Ren
 */
public class FrontDeskControl {
    public static final String MEMBER_POINTS_PAYMENT_METHOD = "Member Points";

    private final ReservationDAO reservationDAO;
    private final RoomDAO roomDAO;
    private final RequestDao requestDao;
    private final HousekeepingControl housekeepingControl;
    private final LateCheckoutExtensionDAO lateCheckoutExtensionDAO;
    private final LoyaltyServiceControl loyaltyServiceControl;
    private final ListInterface<Reservation> reservations;
    private final ListInterface<Room> rooms;
    private final SearchTreeInterface<String, Reservation> confirmationIndex;
    private int lastAwardedPoints;
    private boolean lastLoyaltyAwardFailed;

    public FrontDeskControl() {
        this(new ReservationDAO(), new RoomDAO(), new RequestDao(),
                new HousekeepingControl(false), new LateCheckoutExtensionDAO(),
                new LoyaltyServiceControl());
    }

    public FrontDeskControl(ReservationDAO reservationDAO, RoomDAO roomDAO) {
        this(reservationDAO, roomDAO, new RequestDao(), new HousekeepingControl(false),
                new LateCheckoutExtensionDAO(), new LoyaltyServiceControl());
    }

    public FrontDeskControl(ReservationDAO reservationDAO, RoomDAO roomDAO,
            RequestDao requestDao) {
        this(reservationDAO, roomDAO, requestDao, new HousekeepingControl(false),
                new LateCheckoutExtensionDAO(), new LoyaltyServiceControl());
    }

    /**
     * Creates the control with an injectable Housekeeping gateway. The
     * four-argument overload keeps late check-out notifications testable
     * without changing the normal Front Desk workflow.
     */
    public FrontDeskControl(ReservationDAO reservationDAO, RoomDAO roomDAO,
            RequestDao requestDao, HousekeepingControl housekeepingControl) {
        this(reservationDAO, roomDAO, requestDao, housekeepingControl,
                new LateCheckoutExtensionDAO(), new LoyaltyServiceControl());
    }

    /**
     * Creates the control with Front Desk-owned late check-out storage. The
     * extension is intentionally separate from the Reservation module.
     */
    public FrontDeskControl(ReservationDAO reservationDAO, RoomDAO roomDAO,
            RequestDao requestDao, HousekeepingControl housekeepingControl,
            LateCheckoutExtensionDAO lateCheckoutExtensionDAO) {
        this(reservationDAO, roomDAO, requestDao, housekeepingControl,
                lateCheckoutExtensionDAO, new LoyaltyServiceControl());
    }

    /** Full constructor used when module gateways need to be isolated or tested. */
    public FrontDeskControl(ReservationDAO reservationDAO, RoomDAO roomDAO,
            RequestDao requestDao, HousekeepingControl housekeepingControl,
            LateCheckoutExtensionDAO lateCheckoutExtensionDAO,
            LoyaltyServiceControl loyaltyServiceControl) {
        this.reservationDAO = reservationDAO;
        this.roomDAO = roomDAO;
        this.requestDao = requestDao;
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

    /**
     * Reads the original point-payment request data for a newly approved,
     * full-payment redemption. Reading from the DAO each time keeps Front Desk
     * current when Loyalty approves a request after this screen was opened.
     */
    public boolean hasApprovedMemberPointsPayment(Reservation reservation) {
        if (reservation == null || reservation.getGuest() == null) {
            return false;
        }

        int requiredPoints = pointsRequiredForFullPayment(reservation);
        if (requiredPoints <= 0) {
            return false;
        }

        Iterator<RedemptionRequest> iterator = requestDao.retrieveFromFile().iterator();
        while (iterator.hasNext()) {
            RedemptionRequest request = iterator.next();
            boolean sameReservation = request.getConfirmationNumber()
                    .equalsIgnoreCase(reservation.getConfirmationNumber());
            boolean sameMember = request.getMemberId()
                    .equalsIgnoreCase(reservation.getGuest().getGuestId());
            boolean approved = "Approved".equalsIgnoreCase(request.getStatus());
            boolean paysInFull = request.getPointsRequested() >= requiredPoints;

            if (sameReservation && sameMember && approved && paysInFull) {
                return true;
            }
        }
        return false;
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

        boolean alreadyPaid = "PAID".equalsIgnoreCase(reservation.getPaymentStatus());
        boolean approvedMemberPointsPayment = !alreadyPaid
                && hasApprovedMemberPointsPayment(reservation);
        if (!alreadyPaid && !approvedMemberPointsPayment
                && (paymentMethod == null || paymentMethod.trim().isEmpty())) {
            return CheckInResult.PAYMENT_REQUIRED;
        }
        if (!alreadyPaid && MEMBER_POINTS_PAYMENT_METHOD.equals(paymentMethod)
                && !approvedMemberPointsPayment) {
            return CheckInResult.MEMBER_POINTS_PAYMENT_NOT_APPROVED;
        }

        savedRoom.setStatus(RoomStatus.OCCUPIED);
        reservation.setAssignedRoom(savedRoom);
        if (!alreadyPaid) {
            reservation.setPaymentMethod(approvedMemberPointsPayment
                    ? MEMBER_POINTS_PAYMENT_METHOD : paymentMethod);
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

    /**
     * Records a temporary late check-out from the Front Desk check-out flow
     * and places a blocked notification task in Housekeeping. The temporary
     * record is owned by Front Desk; it does not change Reservation data.
     */
    public LateCheckoutResult extendCheckOut(String confirmationNumber,
            LocalDateTime extendedCheckOutAt, LocalDateTime expectedRoomReadyAt,
            String reason) {
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
        if (expectedRoomReadyAt == null || expectedRoomReadyAt.isBefore(extendedCheckOutAt)) {
            return new LateCheckoutResult(LateCheckoutStatus.INVALID_ROOM_READY_TIME, null);
        }
        if (reason == null || reason.trim().isEmpty()) {
            return new LateCheckoutResult(LateCheckoutStatus.REASON_REQUIRED, null);
        }

        Room savedRoom = findRoomByNumber(reservation.getAssignedRoom().getRoomNumber());
        if (savedRoom == null || savedRoom.getStatus() != RoomStatus.OCCUPIED) {
            return new LateCheckoutResult(LateCheckoutStatus.ROOM_NOT_OCCUPIED, null);
        }

        String guestName = reservation.getGuest() == null
                ? "Unknown guest" : reservation.getGuest().getFullName();
        HousekeepingTask task = housekeepingControl.notifyLateCheckout(
                savedRoom.getRoomNumber(), reservation.getConfirmationNumber(), guestName,
                extendedCheckOutAt, expectedRoomReadyAt, reason.trim());
        if (task == null) {
            return new LateCheckoutResult(
                    LateCheckoutStatus.HOUSEKEEPING_NOTIFICATION_FAILED, null);
        }

        lateCheckoutExtensionDAO.saveOrUpdate(new LateCheckoutExtension(
                reservation.getConfirmationNumber(), extendedCheckOutAt,
                expectedRoomReadyAt, reason.trim()));
        return new LateCheckoutResult(LateCheckoutStatus.SUCCESS, task.getTaskId());
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
        if (reservation.getGuest() == null
                || loyaltyServiceControl.getMemberById(
                        reservation.getGuest().getGuestId()) == null) {
            return;
        }

        try {
            int awarded = loyaltyServiceControl.awardPointsForCompletedStay(
                    reservation.getGuest().getGuestId(),
                    reservation.getConfirmationNumber(), calculateBill(reservation),
                    reservation.getCheckInDate());
            if (awarded > 0) {
                lastAwardedPoints = awarded;
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

    private int pointsRequiredForFullPayment(Reservation reservation) {
        double bill = calculateBill(reservation);
        if (!Double.isFinite(bill) || bill <= 0) {
            return -1;
        }
        long points = (long) Math.ceil(bill); // 1 point = RM1; 10 points = RM10.
        return points <= Integer.MAX_VALUE ? (int) points : -1;
    }

    private String paymentMethodLabel(Reservation reservation) {
        String paymentMethod = reservation.getPaymentMethod();
        return paymentMethod == null || paymentMethod.trim().isEmpty()
                ? "Unspecified" : paymentMethod.trim();
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
        MEMBER_POINTS_PAYMENT_NOT_APPROVED
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
