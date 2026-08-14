package WalkInRegistrationAndReservation.control;

import WalkInRegistrationAndReservation.dao.LoyaltyLookupDAO;
import WalkInRegistrationAndReservation.dao.LoyaltyLookupDAO.LoyaltyProfile;
import WalkInRegistrationAndReservation.dao.ReservationDAO;
import WalkInRegistrationAndReservation.dao.RoomDAO;
import WalkInRegistrationAndReservation.entity.BookingType;
import WalkInRegistrationAndReservation.entity.Guest;
import WalkInRegistrationAndReservation.entity.LoyaltyTier;
import WalkInRegistrationAndReservation.entity.Reservation;
import WalkInRegistrationAndReservation.entity.ReservationStatus;
import WalkInRegistrationAndReservation.entity.Room;
import WalkInRegistrationAndReservation.entity.Room.RoomStatus;
import WalkInRegistrationAndReservation.utility.ConfirmationNumberGenerator;
import adt.ArrayList;
import adt.ArrayStack;
import adt.ListInterface;
import adt.MaxHeapPriorityQueue;
import adt.PriorityQueueInterface;
import adt.StackInterface;
import java.time.LocalDate;
import java.util.Iterator;

/**
 * Handles VIP priority reservations and automatic room allocation.
 *
 * @author Wan Yin
 */
public class ReservationManager {

    private final ReservationDAO reservationDAO;
    private final RoomDAO roomDAO;
    private final LoyaltyLookupDAO loyaltyLookupDAO;
    private final ListInterface<Reservation> reservations;
    private final ListInterface<Room> rooms;
    private final PriorityQueueInterface<Reservation> pendingPriorityReservations;
    private final StackInterface<String> cancellationHistory;

    public ReservationManager() {
        this(new ReservationDAO(), new RoomDAO(), new LoyaltyLookupDAO());
    }

    public ReservationManager(ReservationDAO reservationDAO, RoomDAO roomDAO,
            LoyaltyLookupDAO loyaltyLookupDAO) {
        this.reservationDAO = reservationDAO;
        this.roomDAO = roomDAO;
        this.loyaltyLookupDAO = loyaltyLookupDAO;
        reservations = reservationDAO.retrieveFromFile();
        rooms = roomDAO.retrieveFromFile();
        pendingPriorityReservations = new MaxHeapPriorityQueue<>(this::compareReservationPriority);
        cancellationHistory = new ArrayStack<>();
        rebuildPendingPriorityQueue();
    }

    public LoyaltyProfile findLoyaltyProfile(String guestId) {
        return loyaltyLookupDAO.findProfile(guestId);
    }

    public Reservation submitPriorityReservationRequest(Guest guest,
            LocalDate checkInDate, LocalDate checkOutDate) {
        Reservation reservation = new Reservation(generateUniqueConfirmationNumber(), guest,
                checkInDate, checkOutDate);

        reservations.add(reservation);
        pendingPriorityReservations.enqueue(reservation);
        saveData();
        return reservation;
    }

    public Iterator<Reservation> getPendingPriorityReservationIterator() {
        return pendingPriorityReservations.getIterator();
    }

    public int getPendingPriorityReservationCount() {
        return pendingPriorityReservations.getNumberOfEntries();
    }

    public AllocationResult allocateAvailableRooms() {
        int confirmedCount = 0;
        int rejectedCount = 0;

        while (!pendingPriorityReservations.isEmpty()) {
            Reservation reservation = pendingPriorityReservations.dequeue();
            Room room = findAvailableRoom();

            if (room == null) {
                reservation.setStatus(ReservationStatus.REJECTED);
                rejectedCount++;
            } else {
                reservation.setAssignedRoom(room);
                reservation.setStatus(ReservationStatus.CONFIRMED);
                room.setStatus(RoomStatus.RESERVED);
                confirmedCount++;
            }
        }

        saveData();
        return new AllocationResult(confirmedCount, rejectedCount);
    }

    public boolean checkInPriorityReservation(String searchValue, String paymentMethod) {
        Reservation reservation = findReservation(searchValue);

        if (reservation == null
                || reservation.getStatus() != ReservationStatus.CONFIRMED
                || reservation.getCheckInDate().isAfter(LocalDate.now())
                || reservation.getAssignedRoom() == null) {
            return false;
        }

        boolean alreadyPaid = "PAID".equalsIgnoreCase(reservation.getPaymentStatus());
        if (!alreadyPaid && (paymentMethod == null || paymentMethod.trim().isEmpty())) {
            return false;
        }

        Room savedRoom = findRoomByNumber(reservation.getAssignedRoom().getRoomNumber());
        if (savedRoom == null || savedRoom.getStatus() != RoomStatus.RESERVED) {
            return false;
        }

        savedRoom.setStatus(RoomStatus.OCCUPIED);
        reservation.setAssignedRoom(savedRoom);

        if (!alreadyPaid) {
            reservation.setPaymentMethod(paymentMethod);
            reservation.setPaymentStatus("PAID");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        saveData();
        return true;
    }

    public boolean checkOutReservation(String confirmationNumber) {
        Reservation reservation = findByConfirmationNumber(confirmationNumber);

        if (reservation == null
                || reservation.getStatus() != ReservationStatus.CHECKED_IN
                || reservation.getAssignedRoom() == null) {
            return false;
        }

        Room savedRoom = findRoomByNumber(reservation.getAssignedRoom().getRoomNumber());
        if (savedRoom == null || savedRoom.getStatus() != RoomStatus.OCCUPIED) {
            return false;
        }

        reservation.setStatus(ReservationStatus.CHECKED_OUT);
        savedRoom.setStatus(RoomStatus.NEEDS_CLEANING);
        reservation.setAssignedRoom(savedRoom);
        saveData();
        return true;
    }

    public Reservation findReservation(String searchValue) {
        if (searchValue == null) {
            return null;
        }

        Iterator<Reservation> iterator = reservations.iterator();
        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            Guest guest = reservation.getGuest();

            if (reservation.getConfirmationNumber().equalsIgnoreCase(searchValue)
                    || (guest != null && guest.getGuestId().equalsIgnoreCase(searchValue))
                    || (guest != null && guest.getFullName().toLowerCase()
                            .contains(searchValue.toLowerCase()))) {
                return reservation;
            }
        }
        return null;
    }

    public Reservation findByConfirmationNumber(String confirmationNumber) {
        if (confirmationNumber == null) {
            return null;
        }

        Iterator<Reservation> iterator = reservations.iterator();
        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            if (reservation.getConfirmationNumber().equalsIgnoreCase(confirmationNumber)) {
                return reservation;
            }
        }

        return null;
    }

    public ListInterface<Reservation> findMatchingReservations(String searchValue) {
        ListInterface<Reservation> matches = new ArrayList<>();

        if (searchValue == null || searchValue.trim().isEmpty()) {
            return matches;
        }

        String normalizedSearchValue = searchValue.trim().toLowerCase();
        Iterator<Reservation> iterator = reservations.iterator();

        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            Guest guest = reservation.getGuest();
            boolean confirmationMatches = reservation.getConfirmationNumber()
                    .equalsIgnoreCase(normalizedSearchValue);
            boolean guestIdMatches = guest != null
                    && guest.getGuestId().equalsIgnoreCase(normalizedSearchValue);
            boolean guestNameMatches = guest != null
                    && guest.getFullName().toLowerCase().contains(normalizedSearchValue);

            if (confirmationMatches || guestIdMatches || guestNameMatches) {
                matches.add(reservation);
            }
        }

        return matches;
    }

    public boolean cancelReservation(String confirmationNumber) {
        Reservation reservation = findByConfirmationNumber(confirmationNumber);

        if (!canCancelReservation(reservation)) {
            return false;
        }

        reservation.setStatus(ReservationStatus.CANCELLED);

        Room assignedRoom = reservation.getAssignedRoom();
        if (assignedRoom != null) {
            Room savedRoom = findRoomByNumber(assignedRoom.getRoomNumber());
            if (savedRoom != null) {
                savedRoom.setStatus(RoomStatus.AVAILABLE);
                reservation.setAssignedRoom(savedRoom);
            }
        }

        cancellationHistory.push(reservation.getConfirmationNumber());
        saveData();
        return true;
    }

    public boolean canCancelReservation(Reservation reservation) {
        if (reservation == null
                || reservation.getStatus() != ReservationStatus.CONFIRMED
                || reservation.getCheckInDate() == null
                || reservation.getCheckInDate().isBefore(LocalDate.now())
                || reservation.getAssignedRoom() == null) {
            return false;
        }

        Room savedRoom = findRoomByNumber(reservation.getAssignedRoom().getRoomNumber());
        return savedRoom != null && savedRoom.getStatus() == RoomStatus.RESERVED;
    }

    public Reservation getLastCancelledReservation() {
        while (!cancellationHistory.isEmpty()) {
            Reservation reservation = findByConfirmationNumber(cancellationHistory.peek());

            if (reservation != null && reservation.getStatus() == ReservationStatus.CANCELLED) {
                return reservation;
            }

            cancellationHistory.pop();
        }

        return null;
    }

    public boolean undoLastCancellation() {
        Reservation reservation = getLastCancelledReservation();

        if (reservation == null || reservation.getAssignedRoom() == null) {
            return false;
        }

        Room savedRoom = findRoomByNumber(reservation.getAssignedRoom().getRoomNumber());
        if (savedRoom == null || savedRoom.getStatus() != RoomStatus.AVAILABLE) {
            return false;
        }

        savedRoom.setStatus(RoomStatus.RESERVED);
        reservation.setAssignedRoom(savedRoom);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        cancellationHistory.pop();
        saveData();
        return true;
    }

    public Room findAvailableRoom() {
        Iterator<Room> iterator = rooms.iterator();

        while (iterator.hasNext()) {
            Room room = iterator.next();

            if (room.getStatus() == RoomStatus.AVAILABLE) {
                return room;
            }
        }

        return null;
    }

    public Room findRoomByNumber(String roomNumber) {
        Iterator<Room> iterator = rooms.iterator();

        while (iterator.hasNext()) {
            Room room = iterator.next();

            if (room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                return room;
            }
        }

        return null;
    }

    public void saveData() {
        reservationDAO.saveToFile(reservations);
        roomDAO.saveToFile(rooms);
    }

    public ListInterface<Reservation> getReservations() {
        return reservations;
    }

    public ListInterface<Room> getRooms() {
        return rooms;
    }

    private void rebuildPendingPriorityQueue() {
        Iterator<Reservation> iterator = reservations.iterator();

        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            if (reservation.getStatus() == ReservationStatus.PENDING) {
                pendingPriorityReservations.enqueue(reservation);
            }
        }
    }

    private String generateUniqueConfirmationNumber() {
        String confirmationNumber = ConfirmationNumberGenerator.generate();

        while (findReservation(confirmationNumber) != null) {
            confirmationNumber = ConfirmationNumberGenerator.generate();
        }

        return confirmationNumber;
    }

    private int compareReservationPriority(Reservation first, Reservation second) {
        int tierCompare = Integer.compare(
                first.getGuest().getLoyaltyTier().getPriorityScore(),
                second.getGuest().getLoyaltyTier().getPriorityScore());

        if (tierCompare != 0) {
            return tierCompare;
        }

        int bookingTimeCompare = second.getBookingDateTime().compareTo(first.getBookingDateTime());
        if (bookingTimeCompare != 0) {
            return bookingTimeCompare;
        }

        return second.getConfirmationNumber().compareToIgnoreCase(first.getConfirmationNumber());
    }

    public static class AllocationResult {

        private final int confirmedCount;
        private final int rejectedCount;

        public AllocationResult(int confirmedCount, int rejectedCount) {
            this.confirmedCount = confirmedCount;
            this.rejectedCount = rejectedCount;
        }

        public int getConfirmedCount() {
            return confirmedCount;
        }

        public int getRejectedCount() {
            return rejectedCount;
        }
    }
}
