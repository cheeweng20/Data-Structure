package VIPPriorityRoomAllocation.boundary;

import VIPPriorityRoomAllocation.entity.Guest;
import VIPPriorityRoomAllocation.entity.Reservation;
import VIPPriorityRoomAllocation.entity.Room;
import adt.ListInterface;
import adt.SortedListInterface;
import common.ui.ConsoleAnimation;
import common.ui.MessageUI;
import java.util.Iterator;

/** Prints reservation tables and detail views without changing reservation data. */
final class ReservationTablePrinter {

    private ReservationTablePrinter() {
    }

    static void printAll(ListInterface<Reservation> reservations) {
        if (reservations.isEmpty()) {
            MessageUI.displayInfo("No reservation record found.");
            return;
        }

        String border = "+-----+------------+--------------------+-----------+----------+------------+----------------+----------------+";
        System.out.println("\n--- All Reservations ---");
        System.out.println(border);
        System.out.printf("| %-3s | %-10s | %-18s | %-9s | %-8s | %-10s | %-14s | %-14s |%n",
                "No.", "Res ID", "Guest Name", "Tier", "Room", "Check-In",
                "Payment Status", "Status");
        System.out.println(border);

        int number = 1;
        for (Reservation reservation : reservations) {
            Room room = reservation.getAssignedRoom();
            String roomNumber = room == null ? "-" : room.getRoomNumber();
            System.out.printf("| %-3d | %-10s | %-18.18s | %-9s | %-8s | %-10s | %-14s | %-14s |%n",
                    number++,
                    reservation.getConfirmationNumber(),
                    reservation.getGuest().getFullName(),
                    reservation.getGuest().getLoyaltyTier(),
                    roomNumber,
                    reservation.getCheckInDate(),
                    reservation.getPaymentStatus(),
                    reservation.getStatus());
        }

        System.out.println(border);
        System.out.println("Total reservations: " + reservations.getNumberOfEntries());
    }

    static void printPending(Iterator<Reservation> iterator, int pendingCount) {
        if (!iterator.hasNext()) {
            MessageUI.displayInfo("No pending priority reservation requests.");
            return;
        }

        String border = "+----------+--------------+----------------------+-----------+------------+------------+";
        System.out.println("\n--- Pending Priority Reservations ---");
        System.out.println(border);
        System.out.printf("| %-8s | %-12s | %-20s | %-9s | %-10s | %-10s |%n",
                "Priority", "Request No.", "Guest Name", "Tier", "Check-In", "Status");
        System.out.println(border);

        int position = 1;
        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            System.out.printf("| %-8d | %-12s | %-20.20s | %-9s | %-10s | %-10s |%n",
                    position++,
                    reservation.getConfirmationNumber(),
                    reservation.getGuest().getFullName(),
                    reservation.getGuest().getLoyaltyTier(),
                    reservation.getCheckInDate(),
                    reservation.getStatus());
        }

        System.out.println(border);
        System.out.println("Total pending requests: " + pendingCount);
    }

    static void printSuccessfulAllocations(
            SortedListInterface<Reservation> reservations) {
        if (reservations.isEmpty()) {
            MessageUI.displayInfo("No successful room allocation found.");
            return;
        }

        for (int i = 1; i <= reservations.getNumberOfEntries(); i++) {
            Reservation reservation = reservations.getEntry(i);
            ConsoleAnimation.statusSequence("Assigning room "
                    + reservation.getAssignedRoom().getRoomNumber() + " to "
                    + reservation.getGuest().getFullName());
        }

        String border = "+-----+------------+--------------------+-----------+----------+------------+-------------+";
        System.out.println("\n--- Successful Room Allocations ---");
        System.out.println(border);
        System.out.printf("| %-3s | %-10s | %-18s | %-9s | %-8s | %-10s | %-11s |%n",
                "No.", "Res ID", "Guest Name", "Tier", "Room", "Check-In", "Status");
        System.out.println(border);

        for (int i = 1; i <= reservations.getNumberOfEntries(); i++) {
            Reservation reservation = reservations.getEntry(i);
            System.out.printf("| %-3d | %-10s | %-18.18s | %-9s | %-8s | %-10s | %-11s |%n",
                    i,
                    reservation.getConfirmationNumber(),
                    reservation.getGuest().getFullName(),
                    reservation.getGuest().getLoyaltyTier(),
                    reservation.getAssignedRoom().getRoomNumber(),
                    reservation.getCheckInDate(),
                    reservation.getStatus());
        }

        System.out.println(border);
        System.out.println("Total successful allocations: "
                + reservations.getNumberOfEntries());
    }

    static void printDetails(Reservation reservation) {
        Guest guest = reservation.getGuest();
        Room room = reservation.getAssignedRoom();
        long numberOfNights = reservation.getCheckOutDate().toEpochDay()
                - reservation.getCheckInDate().toEpochDay();
        double totalPrice = room == null ? 0.00
                : numberOfNights * room.getPricePerNight();

        System.out.println("\n--- Reservation Details ---");
        System.out.println("Reservation ID   : " + reservation.getConfirmationNumber());
        System.out.println("Guest ID         : " + guest.getGuestId());
        System.out.println("Guest Name       : " + guest.getFullName());
        System.out.println("Phone Number     : " + guest.getPhoneNumber());
        System.out.println("Loyalty Tier     : " + guest.getLoyaltyTier());
        System.out.println("Room Type        : " + Room.ROOM_TYPE);
        System.out.println("Room / Unit No.  : "
                + (room == null ? "Not assigned" : room.getRoomNumber()));
        System.out.println("Check-in Date    : " + reservation.getCheckInDate());
        System.out.println("Check-out Date   : " + reservation.getCheckOutDate());
        System.out.println("Number of Nights : " + numberOfNights);

        if (room != null) {
            System.out.printf("Price per Night  : RM%.2f%n", room.getPricePerNight());
            System.out.printf("Total Price      : RM%.2f%n", totalPrice);
        }

        System.out.println("Payment Method   : " + reservation.getPaymentMethod());
        System.out.println("Payment Status   : " + reservation.getPaymentStatus());
        System.out.println("Status           : " + reservation.getStatus());
    }
}
