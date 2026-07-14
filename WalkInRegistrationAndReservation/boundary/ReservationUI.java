package WalkInRegistrationAndReservation.boundary;

import WalkInRegistrationAndReservation.control.ReservationManager;
import WalkInRegistrationAndReservation.entity.BookingType;
import WalkInRegistrationAndReservation.entity.Guest;
import WalkInRegistrationAndReservation.entity.Reservation;
import WalkInRegistrationAndReservation.entity.ReservationStatus;
import WalkInRegistrationAndReservation.entity.Room;
import WalkInRegistrationAndReservation.utility.InputValidator;
import adt.ListInterface;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;

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
                    cancelReservation();
                    break;
                case "5":
                    displayReservations(reservationManager.getReservations());
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
        System.out.println("4. Cancel Reservation");
        System.out.println("5. View All Reservations");
        System.out.println("0. Back");
        System.out.print("Select an option: ");
    }

    //check in (user booking before)
    private void checkInStandardReservation() {
        System.out.println("\n--- Check-In Standard Reservation ---");
        String searchValue = promptRequiredText("Enter reservation ID / IC / Passport / Guest Name: ");
        Reservation reservation = reservationManager.findReservation(searchValue);

        if (reservation == null) {
            System.out.println("Reservation not found.");
            return;
        }

        if (reservation.getBookingType() != BookingType.STANDARD) {  //check the book type 
            System.out.println("This is not a standard reservation.");
            return;
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {  //check status
            System.out.println("Only confirmed reservations can be checked in.");
            return;
        }

        if (reservation.getCheckInDate().isAfter(LocalDate.now())) {  //check the booking date n checkin date
            System.out.println("Check-in date is " + reservation.getCheckInDate()
                    + ". Guest cannot check in yet.");
            return;
        }

        displayReservationDetails(reservation);  //make confirmation

        if (!confirmYes("Confirm guest check-in? (Y/N): ")) {
            System.out.println("Check-in cancelled.");
            return;
        }

        if (reservationManager.checkInStandardReservation(searchValue)) {
            System.out.println("Check-in successful.");
            displayReservationDetails(reservationManager.findReservation(searchValue));
        } else {
            System.out.println("Check-in failed.");  //not found
        }
    }

    //register for walk-in
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
        String guestId = promptRequiredText("Guest ID / IC / Passport: ");
        String fullName = promptRequiredText("Full name: ");
        String phoneNumber = promptPhoneNumber();
        String email = promptEmail();
        return new Guest(guestId, fullName, phoneNumber, email);
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

    //search reserve
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

    //cancel reserve
    private void cancelReservation() {
        System.out.print("Enter confirmation number to cancel: ");
        String confirmationNumber = scanner.nextLine().trim();
        Reservation reservation = reservationManager.findReservation(confirmationNumber);

        if (reservation == null) {
            System.out.println("Reservation not found.");
            return;
        }

        if (reservation.getStatus() == ReservationStatus.CHECKED_IN) {
            System.out.println("Checked-in reservation cannot be cancelled here.");
            return;
        }

        displayReservationDetails(reservation);

        if (!confirmYes("Confirm cancellation? (Y/N): ")) {
            System.out.println("Cancellation cancelled.");
            return;
        }

        if (reservationManager.cancelReservation(confirmationNumber)) {
            System.out.println("Reservation cancelled successfully.");
        } else {
            System.out.println("Reservation cancellation failed.");
        }
    }

    public void displayReservations(ListInterface<Reservation> reservations) {
        if (reservations.isEmpty()) {
            System.out.println("No reservation record found.");
            return;
        }

        for (int position = 1; position <= reservations.getNumberOfEntries(); position++) {
            displayReservationDetails(reservations.getEntry(position));
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
