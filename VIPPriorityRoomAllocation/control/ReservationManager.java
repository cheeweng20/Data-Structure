package VIPPriorityRoomAllocation.control;

import VIPPriorityRoomAllocation.dao.LoyaltyLookupDAO;
import VIPPriorityRoomAllocation.dao.LoyaltyLookupDAO.LoyaltyProfile;
import VIPPriorityRoomAllocation.dao.ReservationDAO;
import VIPPriorityRoomAllocation.dao.RoomDAO;
import VIPPriorityRoomAllocation.entity.Guest;
import VIPPriorityRoomAllocation.entity.Reservation;
import VIPPriorityRoomAllocation.entity.ReservationStatus;
import VIPPriorityRoomAllocation.entity.Room;
import VIPPriorityRoomAllocation.entity.Room.RoomStatus;
import VIPPriorityRoomAllocation.utility.ConfirmationNumberGenerator;
import adt.ArrayList;
import adt.ListInterface;
import adt.MaxHeapPriorityQueue;
import adt.PriorityQueueInterface;
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

    public ReservationManager() {
        this(new ReservationDAO(), new RoomDAO(), new LoyaltyLookupDAO());
    }

    public ReservationManager(ReservationDAO reservationDAO, RoomDAO roomDAO,
            LoyaltyLookupDAO loyaltyLookupDAO) {
        this.reservationDAO = reservationDAO;
        this.roomDAO = roomDAO;
        this.loyaltyLookupDAO = loyaltyLookupDAO;
        reservations = reservationDAO.retrieveFromFile();    // load reservations from CSV file
        rooms = roomDAO.retrieveFromFile();                     // load rooms from CSV file
        pendingPriorityReservations = new MaxHeapPriorityQueue<>();   // main non-linear ADT
        rebuildPendingPriorityQueue(); // put all PENDING reservations back into the heap
    }

    // loyalty checking by member ID or phone number
    public LoyaltyProfile findLoyaltyProfile(String guestId) {
        return loyaltyLookupDAO.findProfile(guestId);
    }

    //create a new reservation and add it to the pending priority queue
    public Reservation submitPriorityReservationRequest(Guest guest,
            LocalDate checkInDate, LocalDate checkOutDate) {
        Reservation reservation = new Reservation(generateUniqueConfirmationNumber(), guest,
                checkInDate, checkOutDate);

        reservations.add(reservation);
        pendingPriorityReservations.enqueue(reservation); // heap will reorder based on tier priority
        saveData();
        return reservation;
    }

    //view all pending priority reservations
    public Iterator<Reservation> getPendingPriorityReservationIterator() {
        return pendingPriorityReservations.getIterator();
    }

    public int getPendingPriorityReservationCount() {
        return pendingPriorityReservations.getNumberOfEntries();
    }

    // allocate available rooms by taking highest priority guest from heap first
    public AllocationResult allocateAvailableRooms() {
        int confirmedCount = 0;
        int rejectedCount = 0;

        while (!pendingPriorityReservations.isEmpty()) {
            Reservation reservation = pendingPriorityReservations.dequeue(); // highest tier comes out first
            Room room = findAvailableRoom(); // sequentially find first AVAILABLE room

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

    // sequential search, check each reservation one by one
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
                matches.add(reservation); // keep all matched records
            }
        }

        return matches;
    }

    /** Returns only reservations owned by the exact member/guest ID. */
    public ListInterface<Reservation> findReservationsByGuestId(String guestId) {
        ListInterface<Reservation> matches = new ArrayList<>();
        if (guestId == null || guestId.trim().isEmpty()) {
            return matches;
        }

        for (Reservation reservation : reservations) {
            Guest guest = reservation.getGuest();
            if (guest != null && guest.getGuestId().equalsIgnoreCase(guestId.trim())) {
                matches.add(reservation);
            }
        }
        return matches;
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

    public String generateGuestId() {
        int highestNumber = 0;
        Iterator<Reservation> iterator = reservations.iterator();

        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            Guest guest = reservation.getGuest();
            if (guest != null) {
                highestNumber = Math.max(highestNumber, parseNumericId(guest.getGuestId(), "G"));
            }
        }

        return String.format("G%03d", highestNumber + 1); // non-member guest ID format
    }

    //Rebuild the pending priority queue from the reservations list
    private void rebuildPendingPriorityQueue() {
        Iterator<Reservation> iterator = reservations.iterator();

        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            if (reservation.getStatus() == ReservationStatus.PENDING) {
                pendingPriorityReservations.enqueue(reservation);
            }
        }
    }

    //create confirmation num, if duplicate-->generate a new one
    private String generateUniqueConfirmationNumber() {
        String confirmationNumber = ConfirmationNumberGenerator.generate();

        while (findReservation(confirmationNumber) != null) {
            confirmationNumber = ConfirmationNumberGenerator.generate();
        }

        return confirmationNumber;
    }

    private int parseNumericId(String value, String prefix) {
        if (value == null || !value.startsWith(prefix) || value.length() <= prefix.length()) {
            return 0;
        }

        try {
            return Integer.parseInt(value.substring(prefix.length()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    //RESUT     
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
