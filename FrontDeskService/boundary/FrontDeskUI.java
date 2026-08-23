package FrontDeskService.boundary;

import FrontDeskService.control.FrontDeskControl;
import FrontDeskService.control.FrontDeskControl.CheckInResult;
import FrontDeskService.control.FrontDeskControl.CheckOutResult;
import FrontDeskService.utility.FrontDeskValidator;
import VIPPriorityRoomAllocation.entity.Reservation;
import VIPPriorityRoomAllocation.entity.ReservationStatus;
import VIPPriorityRoomAllocation.entity.Room;
import adt.ListInterface;
import common.src.ConsoleStyle;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.Scanner;

/** Console boundary for Front Desk guest services.
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
            displayMenu();
            switch (scanner.nextLine().trim()) {
                case "1":
                    checkInGuest();
                    break;
                case "2":
                    checkOutGuest();
                    break;
                case "3":
                    viewBilling();
                    break;
                case "4":
                    outstandingBalanceReport();
                    break;
                case "5":
                    paymentMethodReport();
                    break;
                case "0":
                    exit = true;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void displayMenu() {
        System.out.println(ConsoleStyle.title("\n--- Front Desk Service ---"));
        System.out.println(ConsoleStyle.menu("1. Check-In Guest\n2. Guest Check-Out\n"
                + "3. View Billing Details\n4. Outstanding Balance Report\n"
                + "5. Payment Method Report\n0. Back"));
        System.out.print(ConsoleStyle.inputPrompt("Select an option: "));
    }

    private void checkInGuest() {
        System.out.println("\n--- Check-In Confirmed Reservation ---");
        Reservation reservation = selectReservationBySearch();
        if (reservation == null) {
            return;
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            System.out.println("Only confirmed reservations can be checked in.");
            System.out.println("Current status: " + reservation.getStatus());
            return;
        }
        if (reservation.getCheckInDate().isAfter(LocalDate.now())) {
            System.out.println("Check-in date is " + reservation.getCheckInDate()
                    + ". Guest cannot check in yet.");
            return;
        }

        displayReservationDetails(reservation);
        if (!confirmYes("Confirm guest check-in? (Y/N): ")) {
            System.out.println("Check-in cancelled.");
            return;
        }

        String paymentMethod = reservation.getPaymentMethod();
        if (!"PAID".equalsIgnoreCase(reservation.getPaymentStatus())) {
            if (control.hasApprovedMemberPointsPayment(reservation)) {
                paymentMethod = FrontDeskControl.MEMBER_POINTS_PAYMENT_METHOD;
                System.out.println("Approved member-points payment found."
                        + " No additional payment is required.");
            } else {
                paymentMethod = promptPaymentMethod();
                if (!confirmYes("Confirm payment? (Y/N): ")) {
                    System.out.println("Payment cancelled. Check-in not completed.");
                    return;
                }
            }
        }

        CheckInResult result = control.checkInReservation(
                reservation.getConfirmationNumber(), paymentMethod);
        if (result == CheckInResult.SUCCESS) {
            System.out.println("Check-in successful.");
            displayReservationDetails(
                    control.findByConfirmationNumber(reservation.getConfirmationNumber()));
        } else {
            displayCheckInFailure(result);
        }
    }

    private void checkOutGuest() {
        System.out.println("\n--- Guest Check-Out ---");
        Reservation reservation = selectReservationBySearch();
        if (reservation == null) {
            return;
        }

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            System.out.println("Only checked-in reservations can be checked out.");
            System.out.println("Current status: " + reservation.getStatus());
            return;
        }

        displayReservationDetails(reservation);
        if (!confirmYes("Confirm guest check-out? (Y/N): ")) {
            System.out.println("Check-out cancelled.");
            return;
        }

        CheckOutResult result = control.checkOutReservation(
                reservation.getConfirmationNumber());
        if (result == CheckOutResult.SUCCESS) {
            System.out.println("Guest checked out successfully.");
            displayReservationDetails(reservation);
        } else {
            System.out.println("Check-out failed: " + getCheckOutFailureMessage(result));
        }
    }

    private void viewBilling() {
        Reservation reservation = findReservationByConfirmationNumber();
        if (reservation == null) {
            return;
        }

        displayReservationDetails(reservation);
        if (reservation.getAssignedRoom() == null) {
            System.out.println("Billing is unavailable until a room has been assigned.");
            return;
        }
        System.out.printf("Total stay charge: RM %.2f%n", control.calculateBill(reservation));
        System.out.println("Payment method   : " + reservation.getPaymentMethod());
        System.out.println("Payment status   : " + reservation.getPaymentStatus());
    }

    private void outstandingBalanceReport() {
        System.out.println("\nOutstanding balance report (sorted by amount, highest first)");
        displayReservationList(control.getOutstandingBalanceReport());
    }

    private void paymentMethodReport() {
        ListInterface<Reservation> reservations = control.getPaymentMethodReport();
        if (reservations.isEmpty()) {
            System.out.println("\nNo paid room records found.");
            return;
        }

        System.out.println("\n--- Payment Method Room Report ---");
        String border = "+----------+----------------------+--------------+------------------------+--------------+";
        System.out.println(border);
        System.out.printf("| %-8s | %-20s | %-12s | %-22s | %-12s |%n",
                "Room", "Guest", "Confirm No.", "Payment Method", "Bill (RM)");
        System.out.println(border);
        int total = 0;
        for (int position = 1; position <= reservations.getNumberOfEntries(); position++) {
            Reservation reservation = reservations.getEntry(position);
            System.out.printf("| %-8s | %-20.20s | %-12s | %-22.22s | %-12.2f |%n",
                    reservation.getAssignedRoom().getRoomNumber(),
                    reservation.getGuest().getFullName(),
                    reservation.getConfirmationNumber(),
                    paymentMethodLabel(reservation),
                    control.calculateBill(reservation));
            total++;
        }
        System.out.println(border);
        System.out.println("Total paid room records: " + total);
    }

    private Reservation findReservationByConfirmationNumber() {
        System.out.print("8-digit confirmation number: ");
        String confirmationNumber = scanner.nextLine().trim();
        if (!FrontDeskValidator.isConfirmationNumber(confirmationNumber)) {
            System.out.println("Confirmation number must contain exactly 8 digits.");
            return null;
        }

        Reservation reservation = control.findByConfirmationNumber(confirmationNumber);
        if (reservation == null) {
            System.out.println("Guest record not found.");
        }
        return reservation;
    }

    private Reservation selectReservationBySearch() {
        ListInterface<Reservation> matches = findReservationsByPrompt();
        if (matches.isEmpty()) {
            System.out.println("Reservation not found.");
            return null;
        }

        if (matches.getNumberOfEntries() == 1) {
            return matches.getEntry(1);
        }

        System.out.println("\n--- Matching Reservations ---");
        String border = "+-----+--------------+----------------------+-----------+------------+";
        System.out.println(border);
        System.out.printf("| %-3s | %-12s | %-20s | %-9s | %-10s |%n",
                "No.", "Reservation", "Guest", "Tier", "Status");
        System.out.println(border);
        for (int position = 1; position <= matches.getNumberOfEntries(); position++) {
            Reservation reservation = matches.getEntry(position);
            System.out.printf("| %-3d | %-12s | %-20.20s | %-9s | %-10s |%n",
                    position,
                    reservation.getConfirmationNumber(),
                    reservation.getGuest().getFullName(),
                    reservation.getGuest().getLoyaltyTier(),
                    reservation.getStatus());
        }
        System.out.println(border);

        while (true) {
            int selection = promptPositiveInteger("Select reservation number: ");
            if (selection <= matches.getNumberOfEntries()) {
                return matches.getEntry(selection);
            }
            System.out.println("Please select a number from 1 to "
                    + matches.getNumberOfEntries() + ".");
        }
    }

    private ListInterface<Reservation> findReservationsByPrompt() {
        return control.findMatchingReservations(promptRequiredText(
                "Enter reservation ID / Member ID / Guest Name: "));
    }

    private void displayReservationList(ListInterface<Reservation> reservations) {
        if (reservations.isEmpty()) {
            System.out.println("No matching reservation found.");
            return;
        }

        System.out.printf("%-10s %-20s %-10s %-12s %-12s%n",
                "Confirm No.", "Guest", "Room", "Payment", "Bill (RM)");
        Iterator<Reservation> iterator = reservations.iterator();
        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            String room = reservation.getAssignedRoom() == null ? "Unassigned"
                    : reservation.getAssignedRoom().getRoomNumber();
            System.out.printf("%-10s %-20s %-10s %-12s %-12.2f%n",
                    reservation.getConfirmationNumber(),
                    reservation.getGuest().getFullName(),
                    room,
                    reservation.getPaymentStatus(),
                    control.calculateBill(reservation));
        }
    }

    private String paymentMethodLabel(Reservation reservation) {
        String paymentMethod = reservation.getPaymentMethod();
        return paymentMethod == null || paymentMethod.trim().isEmpty()
                ? "Unspecified" : paymentMethod.trim();
    }

    private String promptPaymentMethod() {
        while (true) {
            System.out.println("\n--- Payment Method ---");
            System.out.println("1. Cash");
            System.out.println("2. Credit / Debit Card");
            System.out.println("3. Touch n Go");
            System.out.println("4. Online Banking");
            System.out.print("Select payment method: ");

            switch (scanner.nextLine().trim()) {
                case "1":
                    return "Cash";
                case "2":
                    return "Credit / Debit Card";
                case "3":
                    return "Touch n Go";
                case "4":
                    return "Online Banking";
                default:
                    System.out.println("Invalid payment method. Please select 1 to 4.");
            }
        }
    }

    private void displayReservationDetails(Reservation reservation) {
        Room room = reservation.getAssignedRoom();
        long nights = Math.max(1, reservation.getCheckOutDate().toEpochDay()
                - reservation.getCheckInDate().toEpochDay());

        System.out.println("\n--- Reservation Details ---");
        System.out.println("Reservation ID   : " + reservation.getConfirmationNumber());
        System.out.println("Guest ID         : " + reservation.getGuest().getGuestId());
        System.out.println("Guest Name       : " + reservation.getGuest().getFullName());
        System.out.println("Phone Number     : " + reservation.getGuest().getPhoneNumber());
        System.out.println("Loyalty Tier     : " + reservation.getGuest().getLoyaltyTier());
        System.out.println("Room Type        : " + Room.ROOM_TYPE);
        System.out.println("Room / Unit No.  : "
                + (room == null ? "Not assigned" : room.getRoomNumber()));
        System.out.println("Check-in Date    : " + reservation.getCheckInDate());
        System.out.println("Check-out Date   : " + reservation.getCheckOutDate());
        System.out.println("Number of Nights : " + nights);
        if (room != null) {
            System.out.printf("Price per Night  : RM%.2f%n", room.getPricePerNight());
            System.out.printf("Total Price      : RM%.2f%n", control.calculateBill(reservation));
        }
        System.out.println("Payment Method   : " + reservation.getPaymentMethod());
        System.out.println("Payment Status   : " + reservation.getPaymentStatus());
        System.out.println("Status           : " + reservation.getStatus());
    }

    private void displayCheckInFailure(CheckInResult result) {
        switch (result) {
            case PAYMENT_REQUIRED:
                System.out.println("Check-in failed: payment is required.");
                break;
            case MEMBER_POINTS_PAYMENT_NOT_APPROVED:
                System.out.println("Check-in failed: the member-points request is not approved.");
                break;
            case ROOM_NOT_RESERVED:
                System.out.println("Check-in failed: the assigned room is not reserved.");
                break;
            default:
                System.out.println("Check-in failed: " + getCheckInFailureMessage(result));
        }
    }

    private String getCheckInFailureMessage(CheckInResult result) {
        switch (result) {
            case RESERVATION_NOT_FOUND:
                return "reservation not found.";
            case NOT_CONFIRMED:
                return "only confirmed reservations can be checked in.";
            case CHECK_IN_DATE_NOT_REACHED:
                return "the check-in date has not been reached.";
            default:
                return "unable to complete check-in.";
        }
    }

    private String getCheckOutFailureMessage(CheckOutResult result) {
        switch (result) {
            case RESERVATION_NOT_FOUND:
                return "reservation not found.";
            case NOT_CHECKED_IN:
                return "only checked-in reservations can be checked out.";
            case ROOM_NOT_OCCUPIED:
                return "the assigned room is not occupied.";
            default:
                return "unable to complete check-out.";
        }
    }

    private String promptRequiredText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("This field is required.");
        }
    }

    private int promptPositiveInteger(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException exception) {
                // Re-prompt below.
            }
            System.out.println("Enter a positive whole number.");
        }
    }

    private boolean confirmYes(String prompt) {
        while (true) {
            System.out.print(prompt);
            String answer = scanner.nextLine().trim();
            if (answer.equalsIgnoreCase("Y")) {
                return true;
            }
            if (answer.equalsIgnoreCase("N")) {
                return false;
            }
            System.out.println("Please enter Y or N.");
        }
    }
}
