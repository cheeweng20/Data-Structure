package WalkInRegistrationAndReservation.boundary;

import WalkInRegistrationAndReservation.control.ReservationManager;
import WalkInRegistrationAndReservation.entity.BookingType;
import WalkInRegistrationAndReservation.entity.Guest;
import WalkInRegistrationAndReservation.entity.Reservation;
import WalkInRegistrationAndReservation.entity.ReservationStatus;
import WalkInRegistrationAndReservation.entity.Room;
import WalkInRegistrationAndReservation.utility.InputValidator;
import adt.ListInterface;
import adt.ArrayList;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;
import java.util.Iterator;

/**
 * @author Wan Yin
 */
public class ReservationUI {

    private static final int MAX_GUESTS_PER_ROOM = 6;

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
                    checkInStandardReservation();
                    break;
                case "2":
                    createWalkInRegistration();
                    break;
                case "3":
                    searchReservation();
                    break;
                case "4":
                    displayCancellationMenu();
                    break;
                case "5":
                    displayReservations(reservationManager.getReservations());
                    break;
                case "6":
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
        System.out.println("\n--- Walk-In Registration & Standard Reservation Check-In ---");
        System.out.println("1. Check-In Standard Reservation");
        System.out.println("2. Walk-In Registration");
        System.out.println("3. Search Reservation");
        System.out.println("4. Manage Reservation Cancellation");
        System.out.println("5. View All Reservations");
        System.out.println("6. View Report");
        System.out.println("0. Back");
        System.out.print("Select an option: ");
    }

    // check in (user booking before)
    private void checkInStandardReservation() {
        System.out.println("\n--- Check-In Standard Reservation ---");
        String searchValue = promptRequiredText("Enter reservation ID / IC / Passport / Guest Name: ");
        Reservation reservation = reservationManager.findReservation(searchValue);

        if (reservation == null) {
            System.out.println("Reservation not found.");
            return;
        }

        if (reservation.getBookingType() != BookingType.STANDARD) { // check the book type
            System.out.println("This is not a standard reservation.");
            return;
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) { // check status
            System.out.println("Only confirmed reservations can be checked in.");
            return;
        }

        if (reservation.getCheckInDate().isAfter(LocalDate.now())) { // check the booking date n checkin date
            System.out.println("Check-in date is " + reservation.getCheckInDate()
                    + ". Guest cannot check in yet.");
            return;
        }

        displayReservationDetails(reservation); // make confirmation

        if (!confirmYes("Confirm guest check-in? (Y/N): ")) {
            System.out.println("Check-in cancelled.");
            return;
        }

        if (reservationManager.checkInStandardReservation(searchValue)) {
            System.out.println("Check-in successful.");
            displayReservationDetails(reservationManager.findReservation(searchValue));
        } else {
            System.out.println("Check-in failed."); // not found
        }
    }

    // register for walk-in
    private void createWalkInRegistration() {
        System.out.println("\n--- Walk-In Registration ---");
        Guest guest = inputGuest();
        int numberOfGuests = promptNumberOfGuests();
        LocalDate checkInDate = LocalDate.now();
        System.out.println("Check-in date: " + checkInDate + " (today)");
        LocalDate checkOutDate = promptCheckOutDate(checkInDate);
        Room assignedRoom = reservationManager.findAvailableRoomForGuests(numberOfGuests);

        if (assignedRoom == null) {
            System.out.println("No suitable room is currently available.");
            return;
        }

        System.out.println("\nSuitable room automatically assigned:");
        displayRoomDetails(assignedRoom);

        String paymentMethod = promptPaymentMethod();
        System.out.println("Selected payment method: " + paymentMethod);

        if (!confirmYes("Confirm payment? (Y/N): ")) {
            System.out.println("Walk-in registration cancelled.");
            return;
        }

        System.out.println("Payment successful!");
        Reservation reservation = reservationManager.createWalkInRegistration(
                guest, checkOutDate, numberOfGuests, paymentMethod);

        if (reservation == null) {
            System.out.println("No suitable room is currently available.");
            return;
        }

        System.out.println("Walk-in registration successful.");
        displayReservationDetails(reservation);
    }

    private Guest inputGuest() {
        String guestId = promptIcOrPassport();
        String fullName = promptFullName();
        String phoneNumber = promptPhoneNumber();
        String email = promptEmail();
        return new Guest(guestId, fullName, phoneNumber, email);
    }

    private String promptIcOrPassport() {
        while (true) {
            System.out.print("IC / Passport: ");
            String value = scanner.nextLine().trim();

            if (InputValidator.isValidIcOrPassport(value)) {
                return value;
            }

            System.out.println("Invalid IC / Passport.");
            System.out.println("IC format: 12 digits or xxxxxx-xx-xxxx.");
            System.out.println("Passport format: 5-20 letters/numbers.");
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

    private String promptEmail() {
        while (true) {
            System.out.print("Email: ");
            String email = scanner.nextLine().trim();

            if (InputValidator.isValidEmail(email)) {
                return email;
            }

            System.out.println("Invalid email address. Example: guest@email.com");
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

    private LocalDate promptCheckOutDate(LocalDate checkInDate) {
        while (true) {
            LocalDate checkOutDate = promptDate("Check-out date (yyyy-MM-dd): ");

            if (InputValidator.isValidStay(checkInDate, checkOutDate)) {
                return checkOutDate;
            }

            System.out.println("Check-out date must be after check-in date.");
        }
    }

    private int promptPositiveInteger(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                int value = Integer.parseInt(input);
                if (InputValidator.isPositive(value)) {
                    return value;
                }
                System.out.println("Number must be greater than zero.");
            } catch (NumberFormatException ex) {
                System.out.println("Please enter a whole number.");
            }
        }
    }

    private int promptNumberOfGuests() {
        while (true) {
            int numberOfGuests = promptPositiveInteger("Number of guests: ");

            if (numberOfGuests <= MAX_GUESTS_PER_ROOM) {
                return numberOfGuests;
            }

            System.out.println("No single room can accommodate " + numberOfGuests + " guests.");
            System.out.println("Please enter between 1 and " + MAX_GUESTS_PER_ROOM + " guests.");
        }
    }

    private String promptPaymentMethod() {
        while (true) {
            System.out.println("\n--- Payment Method ---");
            System.out.println("1. Touch n Go");
            System.out.println("2. Credit / Debit Card");
            System.out.println("3. Cash");
            System.out.println("4. Online Banking");
            System.out.print("Enter number to choose: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    return "Touch n Go";
                case "2":
                    return "Credit / Debit Card";
                case "3":
                    return "Cash";
                case "4":
                    return "Online Banking";
                default:
                    System.out.println("Invalid payment method. Please try again.");
            }
        }
    }

    private boolean confirmYes(String prompt) {
        System.out.print(prompt);
        String confirmation = scanner.nextLine().trim();
        return confirmation.equalsIgnoreCase("Y");
    }

    // search reserve
    private void searchReservation() {
        System.out.print("Enter reservation ID / IC / Passport / Guest Name: ");
        String searchValue = scanner.nextLine().trim();
        Reservation reservation = reservationManager.findReservation(searchValue);

        if (reservation == null) {
            System.out.println("Reservation not found.");
        } else {
            displayReservationDetails(reservation);
        }
    }

    // cancel reserve
    private void displayCancellationMenu() {
        boolean back = false;

        while (!back) {
            System.out.println("\n--- Reservation Cancellation ---");
            System.out.println("1. Cancel Reservation");
            System.out.println("2. Undo Last Cancellation");
            System.out.println("0. Back");
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
        String searchValue = promptRequiredText(
                "Enter reservation ID / IC / Passport / Guest Name: ");
        ListInterface<Reservation> matches = reservationManager.findMatchingReservations(searchValue);

        if (matches.isEmpty()) {
            System.out.println("Reservation not found.");
            return;
        }

        Reservation reservation = selectReservationForCancellation(matches);
        if (reservation == null) {
            return;
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            System.out.println("Only confirmed reservations can be cancelled.");
            System.out.println("Current status: " + reservation.getStatus());
            return;
        }

        if (reservation.getCheckInDate() == null
                || reservation.getCheckInDate().isBefore(LocalDate.now())) {
            System.out.println("Reservations with a past check-in date cannot be cancelled.");
            return;
        }

        Room assignedRoom = reservation.getAssignedRoom();
        Room savedRoom = assignedRoom == null
                ? null
                : reservationManager.findRoomByNumber(assignedRoom.getRoomNumber());

        if (savedRoom == null || savedRoom.getStatus() != Room.RoomStatus.RESERVED) {
            System.out.println("Reservation does not have a valid reserved room and cannot be cancelled safely.");
            return;
        }

        displayReservationDetails(reservation);

        if (!confirmYes("Confirm cancellation? (Y/N): ")) {
            System.out.println("Cancellation cancelled.");
            return;
        }

        if (reservationManager.cancelReservation(reservation.getConfirmationNumber())) {
            System.out.println("Reservation cancelled successfully.");
        } else {
            System.out.println("Reservation cancellation failed.");
        }
    }

    private Reservation selectReservationForCancellation(ListInterface<Reservation> matches) {
        if (matches.getNumberOfEntries() == 1) {
            return matches.getEntry(1);
        }

        System.out.println("\n--- Matching Reservations ---");
        System.out.printf("%-5s %-14s %-22s %-14s %-12s%n",
                "No.", "Reservation", "Guest", "Check-In", "Status");

        for (int i = 1; i <= matches.getNumberOfEntries(); i++) {
            Reservation reservation = matches.getEntry(i);
            System.out.printf("%-5d %-14s %-22s %-14s %-12s%n",
                    i,
                    reservation.getConfirmationNumber(),
                    reservation.getGuest().getFullName(),
                    reservation.getCheckInDate(),
                    reservation.getStatus());
        }

        while (true) {
            int selection = promptPositiveInteger("Select reservation number: ");
            if (selection <= matches.getNumberOfEntries()) {
                return matches.getEntry(selection);
            }
            System.out.println("Please select a number from 1 to " + matches.getNumberOfEntries() + ".");
        }
    }

    private void undoLastCancellation() {
        Reservation reservation = reservationManager.getLastCancelledReservation();

        if (reservation == null) {
            System.out.println("No cancellation is available to undo in this session.");
            return;
        }

        System.out.println("\n--- Last Cancelled Reservation ---");
        displayReservationDetails(reservation);

        if (!confirmYes("Undo this cancellation? (Y/N): ")) {
            System.out.println("Undo cancelled. Reservation remains CANCELLED.");
            return;
        }

        if (reservationManager.undoLastCancellation()) {
            System.out.println("Cancellation undone successfully.");
            displayReservationDetails(reservation);
        } else {
            System.out.println("Unable to undo cancellation because the original room is no longer available.");
        }
    }

    // daily report print
    private void displayDailyCheckInReport() {

        LocalDate reportDate = promptDate("Enter report date (yyyy-mm-dd): ");
        System.out.println("\n-- Daily Check In report:" + reportDate);

        ListInterface<Reservation> reservations = reservationManager.getReservations(); // get all reservation records
        ListInterface<Reservation> reportReservations = new ArrayList<>(); // store matched report records only
        Iterator<Reservation> iterator = reservations.iterator();

        while (iterator.hasNext()) {
            Reservation reservation = iterator.next(); // get one reservation from the list

            if (reservation.getStatus() == ReservationStatus.CHECKED_IN
                    && reservation.getCheckInDate().equals(reportDate)) {
                reportReservations.add(reservation);
            }
        }

        sortReservationsByBookingDateTime(reportReservations); // sort report by booking date and time
        printReportHeader();

        for (int i = 1; i <= reportReservations.getNumberOfEntries(); i++) {
            printReportLine(reportReservations.getEntry(i)); // display each sorted reservation
        }
        printReportBorder(); // print bottom table line

        System.out.println("Total checked-in reservations for " + reportDate + ": "
                + reportReservations.getNumberOfEntries());
    }

    private void displayRoomOccupancyReport() {
        System.out.println("\n--- Room Occupancy Report ---");

        ListInterface<Reservation> reservations = reservationManager.getReservations(); // get all reservation records
        ListInterface<Reservation> reportReservations = new ArrayList<>(); // store assigned room records only
        Iterator<Reservation> iterator = reservations.iterator();

        while (iterator.hasNext()) {
            Reservation reservation = iterator.next(); // get one reservation from the list

            if (reservation.getAssignedRoom() != null && reservation.getStatus() != ReservationStatus.CANCELLED) {
                reportReservations.add(reservation);
            }
        }

        sortReservationsByRoomNumber(reportReservations); // sort report by room number
        printReportHeader();

        for (int i = 1; i <= reportReservations.getNumberOfEntries(); i++) {
            printReportLine(reportReservations.getEntry(i));
        }
        printReportBorder(); // print bottom table line

        System.out.println("\n Total occupied/assigned reservation:" + reportReservations.getNumberOfEntries()); // print
        // total
        // assigned
        // records

    }

    // for opt 1 (checkin report)
    private void sortReservationsByBookingDateTime(ListInterface<Reservation> reservations) {
        for (int i = 1; i < reservations.getNumberOfEntries(); i++) {
            for (int j = 1; j <= reservations.getNumberOfEntries() - i; j++) {
                Reservation currentReservation = reservations.getEntry(j); // get current reservation
                Reservation nextReservation = reservations.getEntry(j + 1); // get next reservation

                if (currentReservation.compareTo(nextReservation) > 0) { // if the current late that next, then swap
                                                                         // them
                    reservations.replace(j, nextReservation); // move earlier booking to front
                    reservations.replace(j + 1, currentReservation);
                }
            }
        }
    }

    // for opt2 (assignedroom)
    private void sortReservationsByRoomNumber(ListInterface<Reservation> reservations) {
        for (int i = 1; i < reservations.getNumberOfEntries(); i++) {
            for (int j = 1; j <= reservations.getNumberOfEntries() - i; j++) {
                Reservation currentReservation = reservations.getEntry(j); // get current reservation
                Reservation nextReservation = reservations.getEntry(j + 1); // get next reservation

                String currentRoomNumber = currentReservation.getAssignedRoom().getRoomNumber(); // get current room
                                                                                                 // number
                String nextRoomNumber = nextReservation.getAssignedRoom().getRoomNumber(); // get next room number

                if (currentRoomNumber.compareToIgnoreCase(nextRoomNumber) > 0) {
                    reservations.replace(j, nextReservation); // move smaller room number to front
                    reservations.replace(j + 1, currentReservation);
                }
            }
        }
    }

    private void printReportHeader() {
        printReportBorder(); // print top table line
        System.out.printf("| %-12s | %-18s | %-12s | %-15s | %-8s | %-12s | %-12s | %-12s |%n",
                "Res ID", "Guest Name", "Room", "Room Type", "Guests",
                "Check-In", "Check-Out", "Status");
        printReportBorder();

    }

    private void printReportLine(Reservation reservation) {
        Room room = reservation.getAssignedRoom(); // get assigned room
        String roomNumber = room == null ? "-" : room.getRoomNumber(); // get room number
        String roomType = room == null ? "-" : room.getRoomType(); // get room type

        System.out.printf("| %-12s | %-18s | %-12s | %-15s | %-8d | %-12s | %-12s | %-12s |%n",
                reservation.getConfirmationNumber(),
                reservation.getGuest().getFullName(),
                roomNumber,
                roomType,
                reservation.getNumberOfGuests(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getStatus()); // print one report record

    }

    private void printReportBorder() {
        System.out.println(
                "+--------------+--------------------+--------------+-----------------+----------+--------------+--------------+--------------+");
    }

    // ------------------------------------------------------------------------------------
    // Display reservation report
    private void displayReportMenu() {
        boolean back = false;

        while (!back) {
            System.out.println("\n--- Reservation Reports ---");
            System.out.println("1. Daily Check-In Report");
            System.out.println("2. Room Occupancy Report");
            System.out.println("0. Back");
            System.out.print("Select an option: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {

                case "1":
                    displayDailyCheckInReport();
                    break;
                case "2":
                    displayRoomOccupancyReport();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid option. Please try again");
            }
        }
    }

    public void displayReservations(ListInterface<Reservation> reservations) {
        if (reservations.isEmpty()) {
            System.out.println("No reservation record found.");
            return;
        }

        Iterator<Reservation> iterator = reservations.iterator();

        while (iterator.hasNext()) {
            displayReservationDetails(iterator.next());
        }
    }

    private void displayReservationDetails(Reservation reservation) {
        Guest guest = reservation.getGuest();
        Room room = reservation.getAssignedRoom();
        long numberOfNights = ChronoUnit.DAYS.between(
                reservation.getCheckInDate(), reservation.getCheckOutDate());
        double totalPrice = room == null ? 0.00 : numberOfNights * room.getPricePerNight();

        System.out.println("\n--- Reservation Details ---");
        System.out.println("Reservation ID   : " + reservation.getConfirmationNumber());
        System.out.println("Guest Name       : " + guest.getFullName());
        System.out.println("IC / Passport    : " + guest.getGuestId());
        System.out.println("Phone Number     : " + guest.getPhoneNumber());
        System.out.println("Email            : " + guest.getEmail());
        System.out.println("Booking Type     : " + reservation.getBookingType());
        System.out.println("Number of Guests : " + reservation.getNumberOfGuests());
        System.out.println("Check-in Date    : " + reservation.getCheckInDate());
        System.out.println("Check-out Date   : " + reservation.getCheckOutDate());
        System.out.println("Number of Nights : " + numberOfNights);

        if (room == null) {
            System.out.println("Room / Unit No.  : Not assigned");
            System.out.println("Room Type        : " + reservation.getRequestedRoomType());
        } else {
            System.out.println("Room / Unit No.  : " + room.getRoomNumber());
            System.out.println("Room Type        : " + room.getRoomType());
            System.out.println("Room Capacity    : " + room.getCapacity());
            System.out.printf("Price per Night  : RM%.2f%n", room.getPricePerNight());
            System.out.printf("Estimated Price  : RM%.2f%n", totalPrice);
        }

        System.out.println("Payment Method   : " + reservation.getPaymentMethod());
        System.out.println("Payment Status   : " + reservation.getPaymentStatus());
        System.out.println("Status           : " + reservation.getStatus());
    }

    private void displayRoomDetails(Room room) {
        System.out.println("Room / Unit No.  : " + room.getRoomNumber());
        System.out.println("Room Type        : " + room.getRoomType());
        System.out.println("Room Capacity    : " + room.getCapacity());
        System.out.printf("Price per Night  : RM%.2f%n", room.getPricePerNight());
    }

    public ReservationManager getReservationManager() {
        return reservationManager;
    }

    public static void main(String[] args) {
        new ReservationUI().start();
    }
}
