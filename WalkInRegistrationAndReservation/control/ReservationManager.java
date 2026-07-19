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
import adt.ListInterface;
import java.time.LocalDate;

/**
 * Coordinates reservation check-in, walk-in registration and room assignment.
 *
 * @author Wan Yin
 */
public class ReservationManager {

    private final ReservationDAO reservationDAO;
    private final RoomDAO roomDAO;
    private final ListInterface<Reservation> reservations;
    private final ListInterface<Room> rooms;

    public ReservationManager() {
        this(new ReservationDAO(), new RoomDAO());
    }

    public ReservationManager(ReservationDAO reservationDAO, RoomDAO roomDAO) {
        this.reservationDAO = reservationDAO;
        this.roomDAO = roomDAO;
        reservations = reservationDAO.retrieveFromFile();
        rooms = roomDAO.retrieveFromFile();
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
    public boolean checkInStandardReservation(String searchValue) {
        Reservation reservation = findReservation(searchValue); // find reservation first

        if (reservation == null
                || reservation.getBookingType() != BookingType.STANDARD
                || reservation.getStatus() != ReservationStatus.CONFIRMED
                || reservation.getCheckInDate().isAfter(LocalDate.now())) {
            return false;
        }

        Room room = reservation.getAssignedRoom();

        if (room == null) {
            return false;
        }

        Room savedRoom = findRoomByNumber(room.getRoomNumber());
        if (savedRoom != null) {
            savedRoom.setStatus(RoomStatus.OCCUPIED);
            reservation.setAssignedRoom(savedRoom);
        } else {
            room.setStatus(RoomStatus.OCCUPIED);
        }

        reservation.setStatus(ReservationStatus.CHECKED_IN);
        reservation.setPaymentStatus("PAID"); // duble confirm
        saveData();
        return true;
    }

    // find reservation by confirmation no/guestname/guest ic
    public Reservation findReservation(String searchValue) {
        for (int i = 1; i <= reservations.getNumberOfEntries(); i++) {
            Reservation reservation = reservations.getEntry(i);
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

    public Reservation findByConfirmationOrGuestId(String searchValue) {
        for (int i = 1; i <= reservations.getNumberOfEntries(); i++) {
            Reservation reservation = reservations.getEntry(i);
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
        Reservation reservation = findReservation(confirmationNumber);

        // If reservation not found/already check in -->fail
        if (reservation == null || reservation.getStatus() == ReservationStatus.CHECKED_IN) {
            return false;
        } else {
            reservation.setStatus(ReservationStatus.CANCELLED); // here can cancel

            Room assignedRoom = reservation.getAssignedRoom();
            if (assignedRoom != null) {
                Room savedRoom = findRoomByNumber(assignedRoom.getRoomNumber());
                if (savedRoom != null) {
                    savedRoom.setStatus(RoomStatus.AVAILABLE); // same as the upate occcupied (update the assiged and
                                                               // room list)
                    reservation.setAssignedRoom(savedRoom);
                } else {
                    assignedRoom.setStatus(RoomStatus.AVAILABLE);
                }
            }

            saveData();
            return true;
        }
    }

    // assign room
    public boolean assignAvailableRoom(Reservation reservation) {
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room room = rooms.getEntry(i);

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

        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room room = rooms.getEntry(i);

            if (room.getStatus() == RoomStatus.AVAILABLE && room.getCapacity() >= numberOfGuests
                    && (bestRoom == null || room.getCapacity() < bestRoom.getCapacity())) {
                bestRoom = room;
            }
        }

        return bestRoom;
    }

    public Room findRoomByNumber(String roomNumber) {
        for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
            Room room = rooms.getEntry(i);

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

}
