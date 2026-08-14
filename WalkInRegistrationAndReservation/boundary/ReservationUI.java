package WalkInRegistrationAndReservation.boundary;

import WalkInRegistrationAndReservation.control.ReservationManager;
import WalkInRegistrationAndReservation.control.ReservationManager.AllocationResult;
import WalkInRegistrationAndReservation.dao.LoyaltyLookupDAO.LoyaltyProfile;
import WalkInRegistrationAndReservation.entity.Guest;
import WalkInRegistrationAndReservation.entity.LoyaltyTier;
import WalkInRegistrationAndReservation.entity.Reservation;
import WalkInRegistrationAndReservation.entity.ReservationStatus;
import WalkInRegistrationAndReservation.entity.Room;
import WalkInRegistrationAndReservation.utility.InputValidator;
import adt.ArrayList;
import adt.ListInterface;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Scanner;

/**
 * @author Wan Yin
 */
public class ReservationUI {

    private final ReservationManager reservationManager;
    private final Scanner scanner;

    public ReservationUI() {
        this(new Scanner(System.in));
    }

    public ReservationUI(Scanner scanner) {
        this.scanner = scanner;
        reservationManager = new ReservationManager();
    }

    public void start() {
        boolean exit = false;

        while (!exit) {
            displayMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    submitReservationRequest();
                    break;
                case "2":
                    displayPendingPriorityReservations();
                    break;
                case "3":
                    allocateAvailableRooms();
                    break;
                case "4":
                    checkInPriorityReservation();
                    break;
                case "5":
                    checkOutReservation();
                    break;
                case "6":
                    searchReservation();
                    break;
                case "7":
                    displayCancellationMenu();
                    break;
                case "8":
                    displayReservations(reservationManager.getReservations());
                    break;
                case "9":
                    displayReportMenu();
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
        System.out.println("\n--- VIP & Loyalty Tier Priority Room Allocation ---\n"
                + ".-----.----------------------------------------.\n"
                + "| No. |                Function                |\n"
                + ":-----+----------------------------------------:\n"
                + "| 1.  | Submit Reservation Request             |\n"
                + ":-----+----------------------------------------:\n"
                + "| 2.  | View Pending Priority Reservations     |\n"
                + ":-----+----------------------------------------:\n"
                + "| 3.  | Allocate Available Rooms               |\n"
                + ":-----+----------------------------------------:\n"
                + "| 4.  | Check-In Confirmed Reservation         |\n"
                + ":-----+----------------------------------------:\n"
                + "| 5.  | Guest Check-Out                        |\n"
                + ":-----+----------------------------------------:\n"
                + "| 6.  | Search Reservation                     |\n"
                + ":-----+----------------------------------------:\n"
                + "| 7.  | Manage Reservation Cancellation        |\n"
                + ":-----+----------------------------------------:\n"
                + "| 8.  | View All Reservations                  |\n"
                + ":-----+----------------------------------------:\n"
                + "| 9.  | View Reports                           |\n"
                + ":-----+----------------------------------------:\n"
                + "| 0.  | Back                                   |\n"
                + "'-----'----------------------------------------'");
        System.out.print("Select an option: ");
    }

    private void submitReservationRequest() {
        System.out.println("\n--- Submit Reservation Request ---");
        Guest guest = inputGuest();
        LocalDate checkInDate = promptCheckInDate();
        LocalDate checkOutDate = promptCheckOutDate(checkInDate);

        System.out.println("\n--- Review Reservation Request ---");
        System.out.println("Guest ID         : " + guest.getGuestId());
        System.out.println("Guest Name       : " + guest.getFullName());
        System.out.println("Phone Number     : " + guest.getPhoneNumber());
        System.out.println("Loyalty Tier     : " + guest.getLoyaltyTier());
        System.out.println("Room Type        : " + Room.ROOM_TYPE);
        System.out.println("Check-in Date    : " + checkInDate);
        System.out.println("Check-out Date   : " + checkOutDate);
        System.out.println("Allocation       : Automatic by loyalty priority");

        if (!confirmYes("Submit this reservation request? (Y/N): ")) {
            System.out.println("Reservation request cancelled.");
            return;
        }

        Reservation reservation = reservationManager.submitPriorityReservationRequest(
                guest, checkInDate, checkOutDate);

        System.out.println("\nReservation request submitted successfully.");
        System.out.println("Request Number : " + reservation.getConfirmationNumber());
        System.out.println("Status         : " + reservation.getStatus());
        System.out.println("Pending Count  : " + reservationManager.getPendingPriorityReservationCount());
    }

    private Guest inputGuest() {
        String guestId = promptRequiredText("Member ID / IC / Passport: ");
        LoyaltyProfile profile = reservationManager.findLoyaltyProfile(guestId);
        String fullName;
        LoyaltyTier loyaltyTier;

        if (profile == null) {
            System.out.println("No loyalty member record found. Guest will be treated as CLASSIC.");
            fullName = promptFullName();
            loyaltyTier = LoyaltyTier.CLASSIC;
        } else {
            fullName = profile.getName();
            loyaltyTier = profile.getLoyaltyTier();
            System.out.println("Loyalty member found.");
            System.out.println("Member Name      : " + fullName);
            System.out.println("Loyalty Tier     : " + loyaltyTier);
        }

        String phoneNumber = promptPhoneNumber();
        return new Guest(guestId, fullName, phoneNumber, loyaltyTier);
    }

    private void displayPendingPriorityReservations() {
        Iterator<Reservation> iterator = reservationManager.getPendingPriorityReservationIterator();

        if (!iterator.hasNext()) {
            System.out.println("No pending priority reservation requests.");
            return;
        }

        System.out.println("\n--- Pending Priority Reservations ---");
        String border = "+----------+--------------+----------------------+-----------+------------+------------+";
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
        System.out.println("Total pending requests: "
                + reservationManager.getPendingPriorityReservationCount());
    }

    private void allocateAvailableRooms() {
        int pendingCount = reservationManager.getPendingPriorityReservationCount();

        if (pendingCount == 0) {
            System.out.println("No pending priority reservation requests.");
            return;
        }

        System.out.println("\n--- Allocate Available Rooms ---");
        System.out.println("Pending Requests : " + pendingCount);

        if (!confirmYes("Allocate available rooms by loyalty priority? (Y/N): ")) {
            System.out.println("Allocation cancelled.");
            return;
        }

        AllocationResult result = reservationManager.allocateAvailableRooms();
        System.out.println("\nAllocation completed.");
        System.out.println("Confirmed : " + result.getConfirmedCount());
        System.out.println("Rejected  : " + result.getRejectedCount());
    }

    private void checkInPriorityReservation() {
        System.out.println("\n--- Check-In Confirmed Reservation ---");
        String searchValue = promptRequiredText("Enter reservation ID / Member ID / Guest Name: ");
        Reservation reservation = reservationManager.findReservation(searchValue);

        if (reservation == null) {
            System.out.println("Reservation not found.");
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
            paymentMethod = promptPaymentMethod();

            if (!confirmYes("Confirm payment? (Y/N): ")) {
                System.out.println("Payment cancelled. Check-in not completed.");
                return;
            }
        }

        if (reservationManager.checkInPriorityReservation(searchValue, paymentMethod)) {
            System.out.println("Check-in successful.");
            displayReservationDetails(reservationManager.findReservation(searchValue));
        } else {
            System.out.println("Check-in failed.");
        }
    }

    private void checkOutReservation() {
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

        if (reservationManager.checkOutReservation(reservation.getConfirmationNumber())) {
            System.out.println("Guest checked out successfully.");
            displayReservationDetails(reservation);
        } else {
            System.out.println("Check-out failed.");
        }
    }

    private void searchReservation() {
        System.out.println("\n--- Search Reservation ---");
        ListInterface<Reservation> matches = findReservationsByPrompt();

        if (matches.isEmpty()) {
            System.out.println("Reservation not found.");
            return;
        }

        for (int i = 1; i <= matches.getNumberOfEntries(); i++) {
            displayReservationDetails(matches.getEntry(i));
        }
    }

    private void displayCancellationMenu() {
        boolean back = false;

        while (!back) {
            System.out.println("\n--- Reservation Cancellation ---\n"
                    + "1. Cancel Reservation\n"
                    + "2. Undo Last Cancellation\n"
                    + "0. Back");
            System.out.print("Select an option: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    cancelReservation();
                    break;
                case "2":
                    undoLastCancellation();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void cancelReservation() {
        Reservation reservation = selectReservationBySearch();

        if (reservation == null) {
            return;
        }

        if (!reservationManager.canCancelReservation(reservation)) {
            System.out.println("Only future confirmed reservations with reserved rooms can be cancelled.");
            return;
        }

        displayReservationDetails(reservation);

        if (!confirmYes("Cancel this reservation? (Y/N): ")) {
            System.out.println("Cancellation aborted.");
            return;
        }

        if (reservationManager.cancelReservation(reservation.getConfirmationNumber())) {
            System.out.println("Reservation cancelled successfully.");
        } else {
            System.out.println("Reservation cancellation failed.");
        }
    }

    private void undoLastCancellation() {
        Reservation reservation = reservationManager.getLastCancelledReservation();

        if (reservation == null) {
            System.out.println("No cancellation is available to undo in this session.");
            return;
        }

        displayReservationDetails(reservation);

        if (!confirmYes("Undo this cancellation? (Y/N): ")) {
            System.out.println("Undo cancelled.");
            return;
        }

        if (reservationManager.undoLastCancellation()) {
            System.out.println("Cancellation undone successfully.");
        } else {
            System.out.println("Unable to undo cancellation because the original room is no longer available.");
        }
    }

    private Reservation selectReservationBySearch() {
        ListInterface<Reservation> matches = findReservationsByPrompt();

        if (matches.isEmpty()) {
            System.out.println("Reservation not found.");
            return null;
        }

        return selectReservation(matches);
    }

    private ListInterface<Reservation> findReservationsByPrompt() {
        String searchValue = promptRequiredText("Enter reservation ID / Member ID / Guest Name: ");
        return reservationManager.findMatchingReservations(searchValue);
    }

    private Reservation selectReservation(ListInterface<Reservation> matches) {
        if (matches.getNumberOfEntries() == 1) {
            return matches.getEntry(1);
        }

        System.out.println("\n--- Matching Reservations ---");
        String border = "+-----+--------------+----------------------+-----------+------------+";
        System.out.println(border);
        System.out.printf("| %-3s | %-12s | %-20s | %-9s | %-10s |%n",
                "No.", "Reservation", "Guest", "Tier", "Status");
        System.out.println(border);

        for (int i = 1; i <= matches.getNumberOfEntries(); i++) {
            Reservation reservation = matches.getEntry(i);
            System.out.printf("| %-3d | %-12s | %-20.20s | %-9s | %-10s |%n",
                    i,
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
            System.out.println("Please select a number from 1 to " + matches.getNumberOfEntries() + ".");
        }
    }

    public void displayReservations(ListInterface<Reservation> reservations) {
        if (reservations.isEmpty()) {
            System.out.println("No reservation record found.");
            return;
        }

        String border = "+-----+------------+--------------------+-----------+----------+------------+------------+-------------+";
        System.out.println("\n--- All Reservations ---");
        System.out.println(border);
        System.out.printf("| %-3s | %-10s | %-18s | %-9s | %-8s | %-10s | %-10s | %-11s |%n",
                "No.", "Res ID", "Guest Name", "Tier", "Room", "Check-In", "Payment", "Status");
        System.out.println(border);

        Iterator<Reservation> iterator = reservations.iterator();
        int number = 1;

        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            Room room = reservation.getAssignedRoom();
            String roomNumber = room == null ? "-" : room.getRoomNumber();

            System.out.printf("| %-3d | %-10s | %-18.18s | %-9s | %-8s | %-10s | %-10s | %-11s |%n",
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

    private void displayReservationDetails(Reservation reservation) {
        Guest guest = reservation.getGuest();
        Room room = reservation.getAssignedRoom();
        long numberOfNights = ChronoUnit.DAYS.between(
                reservation.getCheckInDate(), reservation.getCheckOutDate());
        double totalPrice = room == null ? 0.00 : numberOfNights * room.getPricePerNight();

        System.out.println("\n--- Reservation Details ---");
        System.out.println("Reservation ID   : " + reservation.getConfirmationNumber());
        System.out.println("Guest ID         : " + guest.getGuestId());
        System.out.println("Guest Name       : " + guest.getFullName());
        System.out.println("Phone Number     : " + guest.getPhoneNumber());
        System.out.println("Loyalty Tier     : " + guest.getLoyaltyTier());
        System.out.println("Room Type        : " + Room.ROOM_TYPE);
        System.out.println("Room / Unit No.  : " + (room == null ? "Not assigned" : room.getRoomNumber()));
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

    private void displayReportMenu() {
        boolean back = false;

        while (!back) {
            System.out.println("\n--- Reservation Reports ---\n"
                    + "1. Monthly Reservation Summary\n"
                    + "2. Monthly Room Allocation Report\n"
                    + "0. Back");
            System.out.print("Select an option: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    displayMonthlyReservationSummary();
                    break;
                case "2":
                    displayMonthlyRoomAllocationReport();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void displayMonthlyReservationSummary() {
        YearMonth reportMonth = promptReportMonth();
        ListInterface<Reservation> reportReservations = new ArrayList<>();
        Iterator<Reservation> iterator = reservationManager.getReservations().iterator();
        int confirmedCount = 0;
        int rejectedCount = 0;
        double estimatedRevenue = 0.00;

        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            if (!YearMonth.from(reservation.getCheckInDate()).equals(reportMonth)) {
                continue;
            }

            reportReservations.add(reservation);
            if (reservation.getStatus() == ReservationStatus.CONFIRMED
                    || reservation.getStatus() == ReservationStatus.CHECKED_IN) {
                confirmedCount++;
                estimatedRevenue += calculateReservationAmount(reservation);
            } else if (reservation.getStatus() == ReservationStatus.REJECTED) {
                rejectedCount++;
            }
        }

        sortReservationsByBookingDateTime(reportReservations);
        System.out.println("\n--- Monthly Reservation Summary: " + reportMonth + " ---");
        System.out.println("Total Reservations : " + reportReservations.getNumberOfEntries());
        System.out.println("Confirmed/Active   : " + confirmedCount);
        System.out.println("Rejected           : " + rejectedCount);
        System.out.printf("Estimated Revenue  : RM%.2f%n", estimatedRevenue);

        printMonthlyReportHeader();
        for (int i = 1; i <= reportReservations.getNumberOfEntries(); i++) {
            printMonthlyReportLine(reportReservations.getEntry(i));
        }
        printMonthlyReportBorder();
    }

    private void displayMonthlyRoomAllocationReport() {
        YearMonth reportMonth = promptReportMonth();
        ListInterface<Reservation> reportReservations = new ArrayList<>();
        Iterator<Reservation> iterator = reservationManager.getReservations().iterator();

        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();

            if (YearMonth.from(reservation.getCheckInDate()).equals(reportMonth)
                    && reservation.getAssignedRoom() != null
                    && reservation.getStatus() != ReservationStatus.REJECTED
                    && reservation.getStatus() != ReservationStatus.CANCELLED) {
                reportReservations.add(reservation);
            }
        }

        sortReservationsByRoomNumber(reportReservations);
        System.out.println("\n--- Monthly Room Allocation Report: " + reportMonth + " ---");
        printAllocationReportHeader();

        for (int i = 1; i <= reportReservations.getNumberOfEntries(); i++) {
            printAllocationReportLine(reportReservations.getEntry(i));
        }
        printAllocationReportBorder();
        System.out.println("Total Allocated Reservations : " + reportReservations.getNumberOfEntries());
        System.out.println("Room Type                    : " + Room.ROOM_TYPE);
    }

    private YearMonth promptReportMonth() {
        while (true) {
            System.out.print("Enter report month (yyyy-MM): ");
            try {
                return YearMonth.parse(scanner.nextLine().trim());
            } catch (DateTimeParseException ex) {
                System.out.println("Invalid month. Please use yyyy-MM format.");
            }
        }
    }

    private double calculateReservationAmount(Reservation reservation) {
        Room room = reservation.getAssignedRoom();
        if (room == null) {
            return 0.00;
        }

        long nights = ChronoUnit.DAYS.between(reservation.getCheckInDate(), reservation.getCheckOutDate());
        return nights * room.getPricePerNight();
    }

    private void printMonthlyReportHeader() {
        printMonthlyReportBorder();
        System.out.printf("| %-10s | %-18s | %-9s | %-16s | %-10s | %-7s | %-12s |%n",
                "Res ID", "Guest Name", "Tier", "Booking Time", "Check-In", "Room", "Amount");
        printMonthlyReportBorder();
    }

    private void printMonthlyReportLine(Reservation reservation) {
        Room room = reservation.getAssignedRoom();
        String roomNumber = room == null ? "-" : room.getRoomNumber();
        String amount = room == null ? "-" : String.format("RM%.2f", calculateReservationAmount(reservation));
        String bookingTime = reservation.getBookingDateTime()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        System.out.printf("| %-10s | %-18.18s | %-9s | %-16s | %-10s | %-7s | %-12s |%n",
                reservation.getConfirmationNumber(),
                reservation.getGuest().getFullName(),
                reservation.getGuest().getLoyaltyTier(),
                bookingTime,
                reservation.getCheckInDate(),
                roomNumber,
                amount);
    }

    private void printMonthlyReportBorder() {
        System.out.println(
                "+------------+--------------------+-----------+------------------+------------+---------+--------------+");
    }

    private void printAllocationReportHeader() {
        printAllocationReportBorder();
        System.out.printf("| %-12s | %-18s | %-9s | %-12s | %-12s | %-12s |%n",
                "Res ID", "Guest Name", "Tier", "Room No.", "Check-In", "Status");
        printAllocationReportBorder();
    }

    private void printAllocationReportLine(Reservation reservation) {
        Room room = reservation.getAssignedRoom();
        String roomNumber = room == null ? "-" : room.getRoomNumber();

        System.out.printf("| %-12s | %-18.18s | %-9s | %-12s | %-12s | %-12s |%n",
                reservation.getConfirmationNumber(),
                reservation.getGuest().getFullName(),
                reservation.getGuest().getLoyaltyTier(),
                roomNumber,
                reservation.getCheckInDate(),
                reservation.getStatus());
    }

    private void printAllocationReportBorder() {
        System.out.println("+--------------+--------------------+-----------+--------------+--------------+--------------+");
    }

    private void sortReservationsByBookingDateTime(ListInterface<Reservation> reservations) {
        for (int i = 1; i < reservations.getNumberOfEntries(); i++) {
            for (int j = 1; j <= reservations.getNumberOfEntries() - i; j++) {
                Reservation current = reservations.getEntry(j);
                Reservation next = reservations.getEntry(j + 1);

                if (current.compareTo(next) > 0) {
                    reservations.replace(j, next);
                    reservations.replace(j + 1, current);
                }
            }
        }
    }

    private void sortReservationsByRoomNumber(ListInterface<Reservation> reservations) {
        for (int i = 1; i < reservations.getNumberOfEntries(); i++) {
            for (int j = 1; j <= reservations.getNumberOfEntries() - i; j++) {
                Reservation current = reservations.getEntry(j);
                Reservation next = reservations.getEntry(j + 1);

                if (current.getAssignedRoom().getRoomNumber()
                        .compareToIgnoreCase(next.getAssignedRoom().getRoomNumber()) > 0) {
                    reservations.replace(j, next);
                    reservations.replace(j + 1, current);
                }
            }
        }
    }

    private LocalDate promptCheckInDate() {
        while (true) {
            LocalDate checkInDate = promptDate("Check-in date (yyyy-MM-dd): ");

            if (!checkInDate.isBefore(LocalDate.now())) {
                return checkInDate;
            }

            System.out.println("Check-in date cannot be before today.");
        }
    }

    private LocalDate promptCheckOutDate(LocalDate checkInDate) {
        while (true) {
            LocalDate checkOutDate = promptDate("Check-out date (yyyy-MM-dd): ");

            if (InputValidator.isValidStay(checkInDate, checkOutDate)) {
                return checkOutDate;
            }

            System.out.println("Check-out date must be after check-in date.");
        }
    }

    private LocalDate promptDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException ex) {
                System.out.println("Dates must use yyyy-MM-dd format.");
            }
        }
    }

    private String promptFullName() {
        while (true) {
            System.out.print("Full name: ");
            String fullName = scanner.nextLine().trim();

            if (InputValidator.isValidName(fullName)) {
                return fullName;
            }

            System.out.println("Invalid name. Use 2-50 letters only; spaces, apostrophe, hyphen, and dot are allowed.");
        }
    }

    private String promptPhoneNumber() {
        while (true) {
            System.out.print("Phone number: ");
            String phoneNumber = scanner.nextLine().trim();

            if (InputValidator.isValidPhoneNumber(phoneNumber)) {
                return phoneNumber;
            }

            System.out.println("Invalid phone number. Please enter 7 to 20 digits.");
        }
    }

    private String promptPaymentMethod() {
        while (true) {
            System.out.println("\n--- Payment Method ---");
            System.out.println("1. Cash");
            System.out.println("2. Credit / Debit Card");
            System.out.println("3. Touch n Go");
            System.out.println("4. Online Banking");
            System.out.print("Select payment method: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
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

    private String promptRequiredText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();

            if (InputValidator.isNonBlank(value)) {
                return value;
            }

            System.out.println("This field is required.");
        }
    }

    private int promptPositiveInteger(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                int value = Integer.parseInt(input);
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException ex) {
                // Re-prompt below.
            }

            System.out.println("Please enter a positive number.");
        }
    }

    private boolean confirmYes(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("Y")) {
                return true;
            }

            if (input.equalsIgnoreCase("N")) {
                return false;
            }

            System.out.println("Please enter Y or N.");
        }
    }

    public ReservationManager getReservationManager() {
        return reservationManager;
    }

    public static void main(String[] args) {
        new ReservationUI().start();
    }
}
