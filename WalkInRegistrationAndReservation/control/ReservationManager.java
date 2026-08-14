package WalkInRegistrationAndReservation.control;

import WalkInRegistrationAndReservation.dao.ReservationDAO;
import WalkInRegistrationAndReservation.dao.RoomDAO;
import WalkInRegistrationAndReservation.entity.BookingType;
import WalkInRegistrationAndReservation.entity.Guest;
import WalkInRegistrationAndReservation.entity.Reservation;
import WalkInRegistrationAndReservation.entity.ReservationStatus;
import WalkInRegistrationAndReservation.entity.Room;
import WalkInRegistrationAndReservation.entity.Room.RoomStatus;
import WalkInRegistrationAndReservation.utility.ConfirmationNumberGenerator;
import java.time.LocalDate;
import adt.ArrayList;
import adt.ArrayStack;
import adt.ListInterface;
import adt.MaxHeapPriorityQueue;
import adt.PriorityQueueInterface;
import adt.StackInterface;
import java.util.Iterator;

/**
 * reservation check-in, walk-in registration and room assignment.
 *
 * @author Wan Yin·
 */
public class ReservationManager {

    private static final String AUTO_ASSIGN_ROOM_TYPE = "AUTO ASSIGN";

    private final ReservationDAO reservationDAO;
    private final RoomDAO roomDAO;
    private final ListInterface<Reservation> reservations;
    private final ListInterface<Room> rooms;
    private final PriorityQueueInterface<Reservation> pendingPriorityReservations;
    private final StackInterface<String> cancellationHistory;

    public ReservationManager() {
        this(new ReservationDAO(), new RoomDAO());
    }

    public ReservationManager(ReservationDAO reservationDAO, RoomDAO roomDAO) {
        this.reservationDAO = reservationDAO;
        this.roomDAO = roomDAO;
        reservations = reservationDAO.retrieveFromFile();
        rooms = roomDAO.retrieveFromFile();
        pendingPriorityReservations = new MaxHeapPriorityQueue<>(this::compareReservationPriority);
        cancellationHistory = new ArrayStack<>();
        rebuildPendingPriorityQueue();
    }

    public Reservation submitStandardBookingRequest(Guest guest,
            LocalDate checkInDate, LocalDate checkOutDate, int numberOfGuests) {
        Reservation reservation = new Reservation(generateUniqueConfirmationNumber(), guest,
                AUTO_ASSIGN_ROOM_TYPE, checkInDate, checkOutDate, numberOfGuests,
                BookingType.VIP_PRIORITY);

        // Save record and add to the heap-backed priority queue.
        reservations.add(reservation);
        pendingPriorityReservations.enqueue(reservation);
        saveData();
        return reservation;
    }

    public Reservation getNextPendingStandardReservation() {
        return pendingPriorityReservations.getFront();
    }

    public Iterator<Reservation> getPendingStandardReservationIterator() {
        return pendingPriorityReservations.getIterator();
    }

    public int getPendingStandardReservationCount() {
        return pendingPriorityReservations.getNumberOfEntries();
    }

    public Reservation processNextPendingStandardReservation() {
        // Process the highest-priority request only.
        Reservation reservation = pendingPriorityReservations.getFront();

        if (reservation == null) {
            return null;
        }

        Room room = findAvailableRoomForGuests(reservation.getNumberOfGuests());
        if (room == null) {
            reservation.setStatus(ReservationStatus.REJECTED);
        } else {
            reservation.setRequestedRoomType(room.getRoomType());
            reservation.setAssignedRoom(room);
            reservation.setStatus(ReservationStatus.CONFIRMED);
            room.setStatus(RoomStatus.RESERVED);
        }

        pendingPriorityReservations.dequeue();
        saveData();
        return reservation;
    }

    // Kept for simple standard reservation creation if needed later.
    public Reservation createReservation(Guest guest, String requestedRoomType,
            LocalDate checkInDate, LocalDate checkOutDate, int numberOfGuests,
            BookingType bookingType) {
        String confirmationNumber = generateUniqueConfirmationNumber();

        Reservation reservation = new Reservation(confirmationNumber, guest,
                requestedRoomType, checkInDate, checkOutDate, numberOfGuests,
                bookingType);
        boolean assigned = assignAvailableRoom(reservation);
        reservation.setStatus(assigned ? ReservationStatus.CONFIRMED : ReservationStatus.PENDING);
        reservations.add(reservation);
        saveData();

        return reservation;
    }

    // create register(walk-in)
    public Reservation createWalkInRegistration(Guest guest, LocalDate checkOutDate, int numberOfGuests,
            String paymentMethod) {
        Room room = findAvailableRoomForGuests(numberOfGuests);

        if (room == null) {
            return null;
        }

        room.setStatus(RoomStatus.OCCUPIED);
        String confirmationNumber = generateUniqueConfirmationNumber();

        Reservation reservation = new Reservation(confirmationNumber, guest, room.getRoomType(), room, LocalDate.now(),
                checkOutDate, java.time.LocalDateTime.now(), numberOfGuests,
                BookingType.WALK_IN,
                paymentMethod,
                "PAID",
                ReservationStatus.CHECKED_IN);

        reservations.add(reservation);
        saveData();
        return reservation;
    }

    // help check in (client already book before)
    public boolean checkInStandardReservation(String searchValue, String paymentMethod) {
        Reservation reservation = findReservation(searchValue); // find reservation first

        if (reservation == null
                || !isPriorityReservation(reservation)
                || reservation.getStatus() != ReservationStatus.CONFIRMED
                || reservation.getCheckInDate().isAfter(LocalDate.now())) {
            return false;
        }

        Room room = reservation.getAssignedRoom();

        if (room == null) {
            return false;
        }

        boolean alreadyPaid = "PAID".equalsIgnoreCase(reservation.getPaymentStatus());
        if (!alreadyPaid && (paymentMethod == null || paymentMethod.trim().isEmpty())) {
            return false;
        }

        Room savedRoom = findRoomByNumber(room.getRoomNumber());
        if (savedRoom != null) {
            savedRoom.setStatus(RoomStatus.OCCUPIED);
            reservation.setAssignedRoom(savedRoom);
        } else {
            room.setStatus(RoomStatus.OCCUPIED);
        }

        if (!alreadyPaid) {
            reservation.setPaymentMethod(paymentMethod);
            reservation.setPaymentStatus("PAID");
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        saveData();
        return true;
    }

    // Check out
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

    // find reservation by confirmation no/guestname/guest ic
    public Reservation findReservation(String searchValue) {
        Iterator<Reservation> iterator = reservations.iterator();
        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            Guest guest = reservation.getGuest();
            if (reservation.getConfirmationNumber().equalsIgnoreCase(searchValue)) { // reserve no
                return reservation;
            } else if (guest != null && guest.getGuestId().equalsIgnoreCase(searchValue)) { // ic/passport
                return reservation;
            } else if (guest != null && guest.getFullName().toLowerCase().contains(searchValue.toLowerCase())) { // name
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

    public Reservation findByConfirmationOrGuestId(String searchValue) {
        Iterator<Reservation> iterator = reservations.iterator();
        while (iterator.hasNext()) {

            Reservation reservation = iterator.next();
            Guest guest = reservation.getGuest();

            if (reservation.getConfirmationNumber().equalsIgnoreCase(searchValue)
                    || (guest != null && guest.getGuestId().equalsIgnoreCase(searchValue))) {
                return reservation;
            }
        }
        return null;
    }

    // update reservation
    public boolean updateReservation(Reservation updatedReservation) {
        // Find the reservation with the same confirmation number.
        for (int i = 1; i <= reservations.getNumberOfEntries(); i++) {
            Reservation reservation = reservations.getEntry(i);

            if (reservation.getConfirmationNumber().equals(updatedReservation.getConfirmationNumber())) {
                // Replace old with new one
                reservations.replace(i, updatedReservation);
                saveData();
                return true;
            }
        }

        return false;
    }

    // cancel reservation
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
            } else {
                assignedRoom.setStatus(RoomStatus.AVAILABLE);
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

        if (reservation == null) {
            return false;
        }

        Room assignedRoom = reservation.getAssignedRoom();
        if (assignedRoom != null) {
            Room savedRoom = findRoomByNumber(assignedRoom.getRoomNumber());

            if (savedRoom == null || savedRoom.getStatus() != RoomStatus.AVAILABLE) {
                return false;
            }

            savedRoom.setStatus(RoomStatus.RESERVED);
            reservation.setAssignedRoom(savedRoom);
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        cancellationHistory.pop();
        saveData();
        return true;
    }

    // assign room
    public boolean assignAvailableRoom(Reservation reservation) {
        Iterator<Room> iterator = rooms.iterator();

        while (iterator.hasNext()) {
            Room room = iterator.next();

            if (room.getRoomType().equalsIgnoreCase(reservation.getRequestedRoomType())
                    && room.getStatus() == RoomStatus.AVAILABLE) {
                reservation.setAssignedRoom(room);
                reservation.setStatus(ReservationStatus.CONFIRMED);
                room.setStatus(RoomStatus.RESERVED);

                return true;
            }
        }

        reservation.setStatus(ReservationStatus.PENDING);
        return false;
    }

    // check the available room, capacity of room > num of guest
    public Room findAvailableRoomForGuests(int numberOfGuests) {
        Room bestRoom = null;
        Iterator<Room> iterator = rooms.iterator();

        while (iterator.hasNext()) {
            Room room = iterator.next();

            if (room.getStatus() == RoomStatus.AVAILABLE && room.getCapacity() >= numberOfGuests
                    && (bestRoom == null || room.getCapacity() < bestRoom.getCapacity())) {
                bestRoom = room;
            }
        }

        return bestRoom;
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

    private String generateUniqueConfirmationNumber() {
        String confirmationNumber = ConfirmationNumberGenerator.generate();

        while (findReservation(confirmationNumber) != null) {
            confirmationNumber = ConfirmationNumberGenerator.generate();
        }

        return confirmationNumber;
    }

    public boolean addRoom(Room room) {
        boolean added = rooms.add(room);
        if (added) {
            roomDAO.saveToFile(rooms);
        }
        return added;
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

    // Restore pending queue after restart.
    private void rebuildPendingPriorityQueue() {
        ListInterface<Reservation> pendingReservations = new ArrayList<>();
        Iterator<Reservation> iterator = reservations.iterator();

        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            if (isPriorityReservation(reservation)
                    && reservation.getStatus() == ReservationStatus.PENDING) {
                pendingReservations.add(reservation);
            }
        }

        for (int i = 1; i < pendingReservations.getNumberOfEntries(); i++) {
            for (int j = 1; j <= pendingReservations.getNumberOfEntries() - i; j++) {
                Reservation current = pendingReservations.getEntry(j);
                Reservation next = pendingReservations.getEntry(j + 1);

                if (current.compareTo(next) > 0) {
                    pendingReservations.replace(j, next);
                    pendingReservations.replace(j + 1, current);
                }
            }
        }

        Iterator<Reservation> pendingIterator = pendingReservations.iterator();
        while (pendingIterator.hasNext()) {
            pendingPriorityReservations.enqueue(pendingIterator.next());
        }
    }

    private boolean isPriorityReservation(Reservation reservation) {
        return reservation.getBookingType() == BookingType.VIP_PRIORITY
                || reservation.getBookingType() == BookingType.STANDARD;
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

}
