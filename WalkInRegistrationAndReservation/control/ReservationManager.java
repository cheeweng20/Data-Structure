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
import adt.LinkedQueue;
import adt.ListInterface;
import adt.QueueInterface;
import java.time.LocalDate;

/**
 * Coordinates reservation, room-assignment and waiting-queue operations.
 *
 */
public class ReservationManager {

    private final ReservationDAO reservationDAO;
    private final RoomDAO roomDAO;
    private final ListInterface<Reservation> reservations;
    private final ListInterface<Room> rooms;
    private final QueueInterface<Reservation> waitingQueue;

    public ReservationManager() {
        this(new ReservationDAO(), new RoomDAO());
    }

    public ReservationManager(ReservationDAO reservationDAO, RoomDAO roomDAO) {
        this.reservationDAO = reservationDAO;
        this.roomDAO = roomDAO;
        reservations = reservationDAO.retrieveFromFile();
        rooms = roomDAO.retrieveFromFile();
        waitingQueue = new LinkedQueue<>();
        // Put saved pending reservations back into the waiting queue.
        for (int i = 1; i <= reservations.getNumberOfEntries(); i++) {
            Reservation reservation = reservations.getEntry(i);
            if (reservation.getStatus() == ReservationStatus.PENDING) {
                waitingQueue.enqueue(reservation);
            }
        }

    }

    // new reservation
    public Reservation createReservation(Guest guest, String requestedRoomType,
            LocalDate checkInDate, LocalDate checkOutDate, int numberOfGuests,
            BookingType bookingType) {
        String confirmationNumber = ConfirmationNumberGenerator.generate(); // here use number generator

        while (findByConfirmationNumber(confirmationNumber) != null) { // check if num exits, if yes create new
            confirmationNumber = ConfirmationNumberGenerator.generate();
        }

        Reservation reservation = new Reservation(confirmationNumber, guest, requestedRoomType, checkInDate,
                checkOutDate, numberOfGuests, bookingType);
        boolean assigned = assignAvailableRoom(reservation);
        if (assigned == false) {
            waitingQueue.enqueue(reservation);
        }
        reservations.add(reservation);
        saveData(); // DAO Save reservation into the file

        return reservation;

    }

    // find reservation by confirmation number
    public Reservation findByConfirmationNumber(String confirmationNumber) {
        for (int i = 1; i <= reservations.getNumberOfEntries(); i++) {
            Reservation reservation = reservations.getEntry(i);
            if (reservation.getConfirmationNumber().equals(confirmationNumber)) {
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

        // No matching reservation found.
        return false;
    }

    // cancel reservation
    public boolean cancelReservation(String confirmationNumber) {
        // Find the reservation using the confirmation number.
        Reservation reservation = findByConfirmationNumber(confirmationNumber);

        // If no reservation is found, cancellation fails.
        if (reservation == null) {
            return false;
        } else {
            // Mark the reservation as cancelled.
            reservation.setStatus(ReservationStatus.CANCELLED);

            // If this reservation has a room, release the room.
            Room assignedRoom = reservation.getAssignedRoom();
            if (assignedRoom != null) {
                assignedRoom.setStatus(RoomStatus.AVAILABLE);
            }

            // Try to assign the released room to pending reservations.
            processWaitingQueue();

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

    // when no room is available, add to waiting queue
    public void processWaitingQueue() {
        QueueInterface<Reservation> stillWaitingQueue = new LinkedQueue<>();

        while (!waitingQueue.isEmpty()) {
            Reservation reservation = waitingQueue.dequeue();
            boolean assigned = assignAvailableRoom(reservation);

            if (assigned == false) {
                stillWaitingQueue.enqueue(reservation);
            }
        }

        while (!stillWaitingQueue.isEmpty()) {
            waitingQueue.enqueue(stillWaitingQueue.dequeue());
        }

        saveData();
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

    public QueueInterface<Reservation> getWaitingQueue() {
        return waitingQueue;
    }
}
