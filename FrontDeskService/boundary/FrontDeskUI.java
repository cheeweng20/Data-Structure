package FrontDeskService.boundary;

import FrontDeskService.control.FrontDeskControl;
import FrontDeskService.utility.FrontDeskValidator;
import VIPPriorityRoomAllocation.entity.Reservation;
import VIPPriorityRoomAllocation.entity.Room;
import adt.ListInterface;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Scanner;
import common.src.ConsoleStyle;

/** Console boundary for front-desk enquiries and reports.
 * @author Front Desk Service team
 */
public class FrontDeskUI {
    private final Scanner scanner;
    private final FrontDeskControl control;

    public FrontDeskUI(Scanner scanner) {
        this.scanner = scanner;
        control = new FrontDeskControl();
    }

    public void start() {
        boolean exit = false;
        while (!exit) {
            System.out.println(ConsoleStyle.title("\n--- Front Desk Service ---"));
            System.out.println(ConsoleStyle.menu("1. Search Guest by Confirmation Number\n"
                    + "2. Check Room Availability\n"
                    + "3. View Billing Details\n"
                    + "4. Arrivals Report\n"
                    + "5. Outstanding Balance Report\n"
                    + "0. Back"));
            System.out.print(ConsoleStyle.prompt("Select an option: "));
            switch (scanner.nextLine().trim()) {
                case "1": searchGuest(); break;
                case "2": checkAvailability(); break;
                case "3": viewBilling(); break;
                case "4": arrivalsReport(); break;
                case "5": outstandingBalanceReport(); break;
                case "0": exit = true; break;
                default: System.out.println(ConsoleStyle.error("Invalid option. Please try again."));
            }
        }
    }

    private void searchGuest() {
        Reservation reservation = findReservation();
        if (reservation != null) {
            displayReservation(reservation);
        }
    }

    private void checkAvailability() {
        ListInterface<Room> rooms = control.findAvailableRooms();
        if (rooms.isEmpty()) {
            System.out.println(ConsoleStyle.info("No available standard room was found."));
            return;
        }
        System.out.println(ConsoleStyle.title("\nAvailable rooms (sorted by nightly rate)"));
        System.out.print(ConsoleStyle.tableHeader(
                String.format("%-8s %-14s %-12s%n", "Room", "Type", "Rate (RM)")));
        Iterator<Room> iterator = rooms.iterator();
        while (iterator.hasNext()) {
            Room room = iterator.next();
            System.out.printf("%-8s %-14s %-12.2f%n", room.getRoomNumber(),
                    Room.ROOM_TYPE, room.getPricePerNight());
        }
    }

    private void viewBilling() {
        Reservation reservation = findReservation();
        if (reservation == null) return;
        displayReservation(reservation);
        if (reservation.getAssignedRoom() == null) {
            System.out.println(ConsoleStyle.info("Billing is unavailable until a room has been assigned."));
            return;
        }
        System.out.printf("Total stay charge: RM %.2f%n", control.calculateBill(reservation));
        System.out.println("Payment method   : " + reservation.getPaymentMethod());
        System.out.println("Payment status   : " + reservation.getPaymentStatus());
    }

    private void arrivalsReport() {
        LocalDate date = promptDate("Arrival date (yyyy-MM-dd): ");
        System.out.print(ConsoleStyle.prompt("Payment status (PAID, UNPAID, or ALL): "));
        String paymentStatus = scanner.nextLine().trim().toUpperCase();
        if (!paymentStatus.equals("PAID") && !paymentStatus.equals("UNPAID") && !paymentStatus.equals("ALL")) {
            System.out.println(ConsoleStyle.error("Use PAID, UNPAID, or ALL."));
            return;
        }
        System.out.println(ConsoleStyle.title("\nArrivals report for " + date + " (sorted by guest name)"));
        displayReservationList(control.getArrivalsReport(date, paymentStatus));
    }

    private void outstandingBalanceReport() {
        System.out.println(ConsoleStyle.title("\nOutstanding balance report (sorted by amount, highest first)"));
        displayReservationList(control.getOutstandingBalanceReport());
    }

    private Reservation findReservation() {
        System.out.print(ConsoleStyle.prompt("8-digit confirmation number: "));
        String confirmationNumber = scanner.nextLine().trim();
        if (!FrontDeskValidator.isConfirmationNumber(confirmationNumber)) {
            System.out.println(ConsoleStyle.error("Confirmation number must contain exactly 8 digits."));
            return null;
        }
        Reservation reservation = control.findByConfirmationNumber(confirmationNumber);
        if (reservation == null) System.out.println(ConsoleStyle.error("Guest record not found."));
        return reservation;
    }

    private void displayReservationList(ListInterface<Reservation> reservations) {
        if (reservations.isEmpty()) {
            System.out.println(ConsoleStyle.info("No matching reservation found."));
            return;
        }
        System.out.print(ConsoleStyle.tableHeader(String.format(
                "%-10s %-20s %-10s %-12s %-12s%n",
                "Confirm No.", "Guest", "Room", "Payment", "Bill (RM)")));
        Iterator<Reservation> iterator = reservations.iterator();
        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            String room = reservation.getAssignedRoom() == null ? "Unassigned"
                    : reservation.getAssignedRoom().getRoomNumber();
            System.out.printf("%-10s %-20s %-10s %-12s %-12.2f%n", reservation.getConfirmationNumber(),
                    reservation.getGuest().getFullName(), room, reservation.getPaymentStatus(),
                    control.calculateBill(reservation));
        }
    }

    private void displayReservation(Reservation reservation) {
        System.out.println(ConsoleStyle.title("\n--- Guest Information ---"));
        System.out.println("Confirmation no. : " + reservation.getConfirmationNumber());
        System.out.println("Guest name       : " + reservation.getGuest().getFullName());
        System.out.println("Guest ID         : " + reservation.getGuest().getGuestId());
        System.out.println("Phone number     : " + reservation.getGuest().getPhoneNumber());
        System.out.println("Stay             : " + reservation.getCheckInDate() + " to " + reservation.getCheckOutDate());
        System.out.println("Assigned room    : " + (reservation.getAssignedRoom() == null ? "Unassigned" : reservation.getAssignedRoom().getRoomNumber()));
        System.out.println("Reservation      : " + reservation.getStatus());
    }

    private int promptPositiveInteger(String prompt) {
        while (true) {
            try {
                System.out.print(ConsoleStyle.prompt(prompt));
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value > 0) return value;
            } catch (NumberFormatException ex) { }
            System.out.println(ConsoleStyle.error("Enter a positive whole number."));
        }
    }

    private LocalDate promptDate(String prompt) {
        while (true) {
            try {
                System.out.print(ConsoleStyle.prompt(prompt));
                return LocalDate.parse(scanner.nextLine().trim());
            } catch (DateTimeParseException ex) {
                System.out.println(ConsoleStyle.error("Use yyyy-MM-dd format."));
            }
        }
    }
}
