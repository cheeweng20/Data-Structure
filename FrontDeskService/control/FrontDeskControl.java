package FrontDeskService.control;

import FrontDeskService.adt.ConfirmationSearchTree;
import VIPPriorityRoomAllocation.dao.ReservationDAO;
import VIPPriorityRoomAllocation.dao.RoomDAO;
import VIPPriorityRoomAllocation.entity.Reservation;
import VIPPriorityRoomAllocation.entity.Room;
import VIPPriorityRoomAllocation.entity.Room.RoomStatus;
import adt.ArrayList;
import adt.ListInterface;
import java.time.LocalDate;
import java.util.Iterator;

/** Business rules and searches for front-desk enquiries.
 * @author Front Desk Service team
 */
public class FrontDeskControl {
    private final ListInterface<Reservation> reservations;
    private final ListInterface<Room> rooms;
    private final ConfirmationSearchTree<Reservation> confirmationIndex;

    public FrontDeskControl() {
        reservations = new ReservationDAO().retrieveFromFile();
        rooms = new RoomDAO().retrieveFromFile();
        confirmationIndex = new ConfirmationSearchTree<>();
        Iterator<Reservation> iterator = reservations.iterator();
        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            confirmationIndex.add(reservation.getConfirmationNumber(), reservation);
        }
    }

    public Reservation findByConfirmationNumber(String confirmationNumber) {
        return confirmationIndex.search(confirmationNumber);
    }

    public ListInterface<Room> findAvailableRooms() {
        ListInterface<Room> result = new ArrayList<>();
        Iterator<Room> iterator = rooms.iterator();
        while (iterator.hasNext()) {
            Room room = iterator.next();
            if (room.getStatus() == RoomStatus.AVAILABLE) {
                result.add(room);
            }
        }
        sortRoomsByPrice(result);
        return result;
    }

    public double calculateBill(Reservation reservation) {
        if (reservation == null || reservation.getAssignedRoom() == null) {
            return 0.0;
        }
        long nights = reservation.getCheckOutDate().toEpochDay()
                - reservation.getCheckInDate().toEpochDay();
        return Math.max(1, nights) * reservation.getAssignedRoom().getPricePerNight();
    }

    public ListInterface<Reservation> getArrivalsReport(LocalDate date, String paymentStatus) {
        ListInterface<Reservation> result = new ArrayList<>();
        Iterator<Reservation> iterator = reservations.iterator();
        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            boolean matchesPayment = paymentStatus.equals("ALL")
                    || reservation.getPaymentStatus().equalsIgnoreCase(paymentStatus);
            if (reservation.getCheckInDate().equals(date) && matchesPayment) {
                result.add(reservation);
            }
        }
        sortReservationsByGuestName(result);
        return result;
    }

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

    private void sortRoomsByPrice(ListInterface<Room> list) {
        for (int end = list.getNumberOfEntries(); end > 1; end--) {
            for (int position = 1; position < end; position++) {
                if (list.getEntry(position).getPricePerNight() > list.getEntry(position + 1).getPricePerNight()) {
                    Room temporary = list.getEntry(position);
                    list.replace(position, list.getEntry(position + 1));
                    list.replace(position + 1, temporary);
                }
            }
        }
    }

    private void sortReservationsByGuestName(ListInterface<Reservation> list) {
        for (int end = list.getNumberOfEntries(); end > 1; end--) {
            for (int position = 1; position < end; position++) {
                if (list.getEntry(position).getGuest().getFullName().compareToIgnoreCase(
                        list.getEntry(position + 1).getGuest().getFullName()) > 0) {
                    swap(list, position, position + 1);
                }
            }
        }
    }

    private void sortReservationsByBillDescending(ListInterface<Reservation> list) {
        for (int end = list.getNumberOfEntries(); end > 1; end--) {
            for (int position = 1; position < end; position++) {
                if (calculateBill(list.getEntry(position)) < calculateBill(list.getEntry(position + 1))) {
                    swap(list, position, position + 1);
                }
            }
        }
    }

    private void swap(ListInterface<Reservation> list, int first, int second) {
        Reservation temporary = list.getEntry(first);
        list.replace(first, list.getEntry(second));
        list.replace(second, temporary);
    }
}
