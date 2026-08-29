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
import VIPPriorityRoomAllocation.reporting.ReportPdfExporter;
import VIPPriorityRoomAllocation.reporting.ReportPdfExporter.ChartType;
import VIPPriorityRoomAllocation.reporting.ReservationReportFormatter;
import VIPPriorityRoomAllocation.utility.ConfirmationNumberGenerator;
import adt.ArrayList;
import adt.ListInterface;
import adt.MaxHeapPriorityQueue;
import adt.PriorityQueueInterface;
import java.time.LocalDate;
import java.time.YearMonth;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import common.utility.Validation;

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
        reservations = reservationDAO.retrieveFromFile(); // load persisted reservations
        rooms = roomDAO.retrieveFromFile(); // load current room availability
        pendingPriorityReservations = new MaxHeapPriorityQueue<>(); // main non-linear ADT
        rebuildPendingPriorityQueue(); // rebuild heap from all PENDING CSV records
    }

    // loyalty checking by member ID or phone number
    public LoyaltyProfile findLoyaltyProfile(String guestId) {
        return loyaltyLookupDAO.findProfile(guestId);
    }

    // Creates a new reservation and inserts it into the heap for later allocation.
    public Reservation submitPriorityReservationRequest(Guest guest,
            LocalDate checkInDate, LocalDate checkOutDate) {
        Reservation reservation = new Reservation(generateUniqueConfirmationNumber(), guest,
                checkInDate, checkOutDate);

        reservations.add(reservation);
        pendingPriorityReservations.enqueue(reservation); // heap reorders by Reservation.compareTo()
        saveData();
        return reservation;
    }

    // Returns the current pending heap order for the waiting queue display.
    public Iterator<Reservation> getPendingPriorityReservationIterator() {
        return pendingPriorityReservations.getIterator();
    }

    public int getPendingPriorityReservationCount() {
        return pendingPriorityReservations.getNumberOfEntries();
    }

    // Allocates rooms by repeatedly removing the highest-priority reservation from the heap.
    public AllocationResult allocateAvailableRooms() {
        int confirmedCount = 0;
        int rejectedCount = 0;
        ListInterface<Reservation> confirmedReservations = new ArrayList<>();

        while (!pendingPriorityReservations.isEmpty()) {
            Reservation reservation = pendingPriorityReservations.dequeue(); // highest tier comes out first
            Room room = findAvailableRoom(); // sequentially finds first AVAILABLE room

            if (room == null) {
                // No available room left: the remaining lower-priority request is rejected.
                reservation.setStatus(ReservationStatus.REJECTED);
                rejectedCount++;
            } else {
                // Available room found: assign room and remember it for this run's output table.
                reservation.setAssignedRoom(room);
                reservation.setStatus(ReservationStatus.CONFIRMED);
                room.setStatus(RoomStatus.RESERVED);
                confirmedReservations.add(reservation);
                confirmedCount++;
            }
        }

        saveData();
        return new AllocationResult(confirmedCount, rejectedCount, confirmedReservations);
    }

    // Sequential search checks each reservation one by one for ID, member ID, or guest name.
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

    /** Updates and persists the payment state for one reservation. */
    public boolean updatePayment(String confirmationNumber, String paymentMethod,
            String paymentStatus) {
        Reservation reservation = findByConfirmationNumber(confirmationNumber);
        if (reservation == null) {
            return false;
        }

        reservation.setPaymentMethod(paymentMethod == null ? "" : paymentMethod.trim());
        reservation.setPaymentStatus(paymentStatus == null ? "UNPAID" : paymentStatus.trim());
        saveData();
        return true;
    }

    public ListInterface<Reservation> findMatchingReservations(String searchValue) {
        ListInterface<Reservation> matches = new ArrayList<>();

        if (searchValue == null || searchValue.trim().isEmpty()) {
            return matches;
        }

        String normalizedSearchValue = searchValue.trim().toLowerCase();
        Iterator<Reservation> iterator = reservations.iterator();

        // Keep all matches so a member or guest name can show multiple reservations.
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

        // Room allocation is first-available-room after the reservation priority is decided by heap.
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

    /** Builds the current monthly reservation report for the boundary. */
    public String getMonthlyReservationSummary(YearMonth reportMonth) {
        return ReservationReportFormatter.buildMonthlyReservationSummary(
                getReservations(), reportMonth);
    }

    /** Builds the current monthly room-allocation report for the boundary. */
    public String getMonthlyRoomAllocationReport(YearMonth reportMonth) {
        return ReservationReportFormatter.buildMonthlyRoomAllocationReport(
                getReservations(), reportMonth);
    }

    public Path exportReport(String title, String report, String chartType) throws IOException {
        return ReportPdfExporter.export(title, report, ChartType.valueOf(chartType));
    }

    public boolean openReport(Path pdfPath) throws IOException {
        return ReportPdfExporter.open(pdfPath);
    }

    public boolean isValidStay(LocalDate checkInDate, LocalDate checkOutDate) {
        return Validation.isValidStay(checkInDate, checkOutDate);
    }

    public boolean isNonBlank(String value) {
        return Validation.isNonBlank(value);
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

    // Rebuilds the heap when the program starts so pending CSV records are not lost.
    private void rebuildPendingPriorityQueue() {
        Iterator<Reservation> iterator = reservations.iterator();

        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            if (reservation.getStatus() == ReservationStatus.PENDING) {
                pendingPriorityReservations.enqueue(reservation);
            }
        }
    }

    // Generates a unique confirmation number by checking existing reservations.
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

    // Carries allocation totals and the current run's confirmed records back to the UI.
    public static class AllocationResult {

        private final int confirmedCount;
        private final int rejectedCount;
        private final ListInterface<Reservation> confirmedReservations;

        public AllocationResult(int confirmedCount, int rejectedCount,
                ListInterface<Reservation> confirmedReservations) {
            this.confirmedCount = confirmedCount;
            this.rejectedCount = rejectedCount;
            this.confirmedReservations = confirmedReservations;
        }

        public int getConfirmedCount() {
            return confirmedCount;
        }

        public int getRejectedCount() {
            return rejectedCount;
        }

        public ListInterface<Reservation> getConfirmedReservations() {
            return confirmedReservations;
        }
    }
}
