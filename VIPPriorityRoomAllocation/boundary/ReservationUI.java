package VIPPriorityRoomAllocation.boundary;

import VIPPriorityRoomAllocation.control.ReservationManager;
import VIPPriorityRoomAllocation.control.ReservationManager.AllocationResult;
import VIPPriorityRoomAllocation.dao.LoyaltyLookupDAO.LoyaltyProfile;
import VIPPriorityRoomAllocation.entity.Guest;
import VIPPriorityRoomAllocation.entity.LoyaltyTier;
import VIPPriorityRoomAllocation.entity.Reservation;
import VIPPriorityRoomAllocation.entity.ReservationStatus;
import VIPPriorityRoomAllocation.entity.Room;
import VIPPriorityRoomAllocation.utility.InputValidator;
import VIPPriorityRoomAllocation.utility.MessageUI;
import adt.ArrayList;
import adt.ListInterface;
import common.src.InputHelper;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
            String choice = InputHelper.inputString(scanner, "Select an option: ").trim();

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
                    displayReservations(reservationManager.getReservations());
                    break;
                case "8":
                    displayReportMenu();
                    break;
                case "0":
                    exit = true;
                    break;
                default:
                    MessageUI.displayError("Invalid option. Please try again.");
            }
        }
    }

    private void displayMenu() {
        System.out.println("\n--- VIP & Loyalty Tier Priority Room Allocation ---\n"
                + ".-----.----------------------------------------.\n"
                + "| No. |                Function                |\n"
                + ":-----+----------------------------------------:\n"
                + "| 1.  | Add New Reservation Request            |\n"
                + ":-----+----------------------------------------:\n"
                + "| 2.  | View Priority Waiting Queue            |\n"
                + ":-----+----------------------------------------:\n"
                + "| 3.  | Allocate Rooms by Priority             |\n"
                + ":-----+----------------------------------------:\n"
                + "| 4.  | Check-In Guest                         |\n"
                + ":-----+----------------------------------------:\n"
                + "| 5.  | Guest Check-Out                        |\n"
                + ":-----+----------------------------------------:\n"
                + "| 6.  | Search Reservation                     |\n"
                + ":-----+----------------------------------------:\n"
                + "| 7.  | View All Reservations                  |\n"
                + ":-----+----------------------------------------:\n"
                + "| 8.  | View Reports                           |\n"
                + ":-----+----------------------------------------:\n"
                + "| 0.  | Back                                   |\n"
                + "'-----'----------------------------------------'");
    }


    // add new reservation request
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
            MessageUI.displayInfo("Reservation request cancelled.");
            return;
        }

        Reservation reservation = reservationManager.submitPriorityReservationRequest(
                guest, checkInDate, checkOutDate);

        MessageUI.displaySuccess("Reservation request submitted successfully.");
        System.out.println("Request Number : " + reservation.getConfirmationNumber());
        System.out.println("Status         : " + reservation.getStatus());
        System.out.println("Pending Count  : " + reservationManager.getPendingPriorityReservationCount());
    }

    private Guest inputGuest() {
        String guestId = promptRequiredText("Member ID : ");
        LoyaltyProfile profile = reservationManager.findLoyaltyProfile(guestId);  // use the guestID
        String fullName;
        LoyaltyTier loyaltyTier;

        if (profile == null) {
            MessageUI.displayInfo("No loyalty member record found. Guest will be treated as CLASSIC.");
            fullName = promptFullName();
            loyaltyTier = LoyaltyTier.CLASSIC;
        } else {
            fullName = profile.getName();
            loyaltyTier = profile.getLoyaltyTier();
            MessageUI.displaySuccess("Loyalty member found.");
            System.out.println("Member Name      : " + fullName);
            System.out.println("Loyalty Tier     : " + loyaltyTier);
        }

        String phoneNumber = promptPhoneNumber();
        return new Guest(guestId, fullName, phoneNumber, loyaltyTier);
    }

    //put the reservation inside the  waiting queue
    private void displayPendingPriorityReservations() {
        Iterator<Reservation> iterator = reservationManager.getPendingPriorityReservationIterator();

        if (!iterator.hasNext()) {
            MessageUI.displayInfo("No pending priority reservation requests.");
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

    //allocate room to guest based on priority level
    private void allocateAvailableRooms() {
        int pendingCount = reservationManager.getPendingPriorityReservationCount(); //show how many prequest pending

        if (pendingCount == 0) {
            MessageUI.displayInfo("No pending priority reservation requests.");
            return;
        }

        System.out.println("\n--- Allocate Available Rooms ---");
        System.out.println("Pending Requests : " + pendingCount);

        if (!confirmYes("Allocate available rooms by loyalty priority? (Y/N): ")) {
            MessageUI.displayInfo("Allocation cancelled.");
            return;
        }

        AllocationResult result = reservationManager.allocateAvailableRooms();
        MessageUI.displaySuccess("Allocation completed.");
        System.out.println("Confirmed : " + result.getConfirmedCount());
        System.out.println("Rejected  : " + result.getRejectedCount());
    }


    //guest checkin 
    private void checkInPriorityReservation() {
        System.out.println("\n--- Check-In Confirmed Reservation ---");
        String searchValue = promptRequiredText("Enter reservation ID / Member ID / Guest Name: ");
        Reservation reservation = reservationManager.findReservation(searchValue);

        if (reservation == null) {
            MessageUI.displayError("Reservation not found.");
            return;
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            MessageUI.displayError("Only confirmed reservations can be checked in.");
            MessageUI.displayInfo("Current status: " + reservation.getStatus());
            return;
        }

        if (reservation.getCheckInDate().isAfter(LocalDate.now())) {
            MessageUI.displayError("Check-in date is " + reservation.getCheckInDate()
                    + ". Guest cannot check in yet.");
            return;
        }

        displayReservationDetails(reservation);

        if (!confirmYes("Confirm guest check-in? (Y/N): ")) {
            MessageUI.displayInfo("Check-in cancelled.");
            return;
        }

        String paymentMethod = reservation.getPaymentMethod();
        if (!"PAID".equalsIgnoreCase(reservation.getPaymentStatus())) {
            paymentMethod = promptPaymentMethod();

            if (!confirmYes("Confirm payment? (Y/N): ")) {
                MessageUI.displayInfo("Payment cancelled. Check-in not completed.");
                return;
            }
        }

        if (reservationManager.checkInPriorityReservation(searchValue, paymentMethod)) {
            MessageUI.displaySuccess("Check-in successful.");
            displayReservationDetails(reservationManager.findReservation(searchValue));
        } else {
            MessageUI.displayError("Check-in failed.");
        }
    }


    //guest checkout
    private void checkOutReservation() {
        System.out.println("\n--- Guest Check-Out ---");
        Reservation reservation = selectReservationBySearch();

        if (reservation == null) {
            return;
        }

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            MessageUI.displayError("Only checked-in reservations can be checked out.");
            MessageUI.displayInfo("Current status: " + reservation.getStatus());
            return;
        }

        displayReservationDetails(reservation);

        if (!confirmYes("Confirm guest check-out? (Y/N): ")) {
            MessageUI.displayInfo("Check-out cancelled.");
            return;
        }

        if (reservationManager.checkOutReservation(reservation.getConfirmationNumber())) {
            MessageUI.displaySuccess("Guest checked out successfully.");
            displayReservationDetails(reservation);
        } else {
            MessageUI.displayError("Check-out failed.");
        }
    }

    //search reservation 
    private void searchReservation() {
        System.out.println("\n--- Search Reservation ---");
        ListInterface<Reservation> matches = findReservationsByPrompt();

        if (matches.isEmpty()) {
            MessageUI.displayError("Reservation not found.");
            return;
        }

        for (int i = 1; i <= matches.getNumberOfEntries(); i++) {
            displayReservationDetails(matches.getEntry(i));
        }
    }

    private Reservation selectReservationBySearch() {
        ListInterface<Reservation> matches = findReservationsByPrompt();

        if (matches.isEmpty()) {
            MessageUI.displayError("Reservation not found.");
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
            MessageUI.displayError("Please select a number from 1 to "
                    + matches.getNumberOfEntries() + ".");
        }
    }

    public void displayReservations(ListInterface<Reservation> reservations) {
        if (reservations.isEmpty()) {
            MessageUI.displayInfo("No reservation record found.");
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

    
    // show all reservations 
    private void displayReservationDetails(Reservation reservation) {
        Guest guest = reservation.getGuest();
        Room room = reservation.getAssignedRoom();
        long numberOfNights = reservation.getCheckOutDate().toEpochDay()
                - reservation.getCheckInDate().toEpochDay();
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

    //MENU NEED TO MAKE THE CHANGES 

    private void displayReportMenu() {
        boolean back = false;

        while (!back) {
            System.out.println("\n--- Reservation Reports ---\n"
                    + "1. Monthly Reservation Summary\n"
                    + "2. Monthly Room Allocation Report\n"
                    + "0. Back");
            String choice = InputHelper.inputString(scanner, "Select an option: ").trim();

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
                    MessageUI.displayError("Invalid option. Please try again.");
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
                    && reservation.getStatus() != ReservationStatus.REJECTED) {
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
            String input = InputHelper.inputString(scanner, "Enter report month (yyyy-MM): ").trim();
            try {
                return YearMonth.parse(input);
            } catch (DateTimeParseException ex) {
                MessageUI.displayError("Invalid month. Please use yyyy-MM format.");
            }
        }
    }

    private double calculateReservationAmount(Reservation reservation) {
        Room room = reservation.getAssignedRoom();
        if (room == null) {
            return 0.00;
        }

        long nights = reservation.getCheckOutDate().toEpochDay()
                - reservation.getCheckInDate().toEpochDay();
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

            MessageUI.displayError("Check-in date cannot be before today.");
        }
    }

    private LocalDate promptCheckOutDate(LocalDate checkInDate) {
        while (true) {
            LocalDate checkOutDate = promptDate("Check-out date (yyyy-MM-dd): ");

            if (InputValidator.isValidStay(checkInDate, checkOutDate)) {
                return checkOutDate;
            }

            MessageUI.displayError("Check-out date must be after check-in date.");
        }
    }

    private LocalDate promptDate(String prompt) {
        while (true) {
            String input = InputHelper.inputString(scanner, prompt).trim();

            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException ex) {
                MessageUI.displayError("Dates must use yyyy-MM-dd format.");
            }
        }
    }

    private String promptFullName() {
        while (true) {
            String fullName = InputHelper.inputString(scanner, "Full name: ").trim();

            if (InputValidator.isValidName(fullName)) {
                return fullName;
            }

            MessageUI.displayError("Invalid name. Use 2-50 letters only; spaces, apostrophe, hyphen, and dot are allowed.");
        }
    }

    private String promptPhoneNumber() {
        while (true) {
            String phoneNumber = InputHelper.inputString(scanner, "Phone number: ").trim();

            if (InputValidator.isValidPhoneNumber(phoneNumber)) {
                return phoneNumber;
            }

            MessageUI.displayError("Invalid phone number. Please enter 7 to 20 digits.");
        }
    }

    private String promptPaymentMethod() {
        while (true) {
            System.out.println("\n--- Payment Method ---");
            System.out.println("1. Cash");
            System.out.println("2. Credit / Debit Card");
            System.out.println("3. Touch n Go");
            System.out.println("4. Online Banking");
            String choice = InputHelper.inputString(scanner, "Select payment method: ").trim();

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
                    MessageUI.displayError("Invalid payment method. Please select 1 to 4.");
            }
        }
    }

    private String promptRequiredText(String prompt) {
        while (true) {
            String value = InputHelper.inputString(scanner, prompt).trim();

            if (InputValidator.isNonBlank(value)) {
                return value;
            }

            MessageUI.displayError("This field is required.");
        }
    }

    private int promptPositiveInteger(String prompt) {
        while (true) {
            String input = InputHelper.inputString(scanner, prompt).trim();

            try {
                int value = Integer.parseInt(input);
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException ex) {
                // Re-prompt below.
            }

            MessageUI.displayError("Please enter a positive number.");
        }
    }

    private boolean confirmYes(String prompt) {
        while (true) {
            String input = InputHelper.inputString(scanner, prompt).trim();

            if (input.equalsIgnoreCase("Y")) {
                return true;
            }

            if (input.equalsIgnoreCase("N")) {
                return false;
            }

            MessageUI.displayError("Please enter Y or N.");
        }
    }

    public ReservationManager getReservationManager() {
        return reservationManager;
    }

    public static void main(String[] args) {
        new ReservationUI().start();
    }
}
