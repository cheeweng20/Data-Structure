package WalkInRegistrationAndReservation.boundary;

import WalkInRegistrationAndReservation.control.ReservationManager;
import WalkInRegistrationAndReservation.entity.BookingType;
import WalkInRegistrationAndReservation.entity.Guest;
import WalkInRegistrationAndReservation.entity.Reservation;
import WalkInRegistrationAndReservation.utility.InputValidator;
import adt.ListInterface;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;

public class ReservationUI {

    private static final int MAX_GUESTS_PER_ROOM = 5;

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
                    createStandardBooking();
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
        System.out.println("\n--- Walk-In Registration & Standard Booking ---");
        System.out.println("1. Standard Booking");
        System.out.println("2. Walk-In Registration");
        System.out.println("3. Search Reservation");
        System.out.println("4. Cancel Reservation");
        System.out.println("5. View All Reservations");
        System.out.println("0. Back");
        System.out.print("Select an option: ");
    }

    private void createStandardBooking() {
        Reservation reservation = inputReservation(BookingType.STANDARD, false);
        displayReservationResult(reservation);
    }

    private void createWalkInRegistration() {
        Reservation reservation = inputReservation(BookingType.WALK_IN, true);
        displayReservationResult(reservation);
    }

    public Reservation inputReservation(BookingType bookingType, boolean walkIn) {
        String guestId = promptRequiredText("Guest ID / IC / Passport: ");
        String fullName = promptRequiredText("Full name: ");
        String phoneNumber = promptPhoneNumber();
        String email = promptEmail();

        LocalDate checkInDate;
        if (!walkIn) {
            checkInDate = promptCheckInDate();
        } else {
            checkInDate = LocalDate.now();
            System.out.println("Check-in date: " + checkInDate + " (today)");
        }

        LocalDate checkOutDate = promptCheckOutDate(checkInDate);
        int numberOfGuests = promptNumberOfGuests();
        String requestedRoomType = promptRoomType(numberOfGuests);
        Guest guest = new Guest(guestId, fullName, phoneNumber, email);

        if (!confirmBooking(guest, checkInDate, checkOutDate, numberOfGuests,
                requestedRoomType, bookingType)) {
            System.out.println("Booking cancelled.");
            return null;
        }

        return reservationManager.createReservation(guest, requestedRoomType,
                checkInDate, checkOutDate, numberOfGuests, bookingType);
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

    private LocalDate promptCheckInDate() {
        while (true) {
            LocalDate checkInDate = promptDate("Check-in date (yyyy-MM-dd): ");

            if (!checkInDate.isBefore(LocalDate.now())) {
                return checkInDate;
            }

            System.out.println("Check-in date cannot be before today.");
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

    private String promptRoomType(int numberOfGuests) {
        System.out.println("\nSuitable room types for " + numberOfGuests + " guest(s):");
        displayRoomTypeOption("1", "Standard Room", 2, 150.00, numberOfGuests);
        displayRoomTypeOption("2", "Deluxe Room", 3, 220.00, numberOfGuests);
        displayRoomTypeOption("3", "Family Room", 5, 300.00, numberOfGuests);
        displayRoomTypeOption("4", "Suite Room", 4, 350.00, numberOfGuests);
        System.out.println("Room assignment still depends on available rooms in the system.");

        while (true) {
            System.out.print("Select room type: ");
            String choice = scanner.nextLine().trim();
            String roomType = getRoomTypeByChoice(choice);

            if (roomType == null) {
                System.out.println("Invalid room type option. Please try again.");
            } else if (getRoomTypeCapacity(roomType) < numberOfGuests) {
                System.out.println(roomType + " cannot accommodate " + numberOfGuests + " guests.");
            } else {
                return roomType;
            }
        }
    }

    private void displayRoomTypeOption(String optionNumber, String roomType,
            int capacity, double price, int numberOfGuests) {
        if (capacity >= numberOfGuests) {
            System.out.printf("%s. %s | Capacity: %d guests | Price: RM%.2f per night%n",
                    optionNumber, roomType, capacity, price);
        }
    }

    private String getRoomTypeByChoice(String choice) {
        switch (choice) {
            case "1":
                return "Standard Room";
            case "2":
                return "Deluxe Room";
            case "3":
                return "Family Room";
            case "4":
                return "Suite Room";
            default:
                return null;
        }
    }

    private int getRoomTypeCapacity(String roomType) {
        switch (roomType) {
            case "Standard Room":
                return 2;
            case "Deluxe Room":
                return 3;
            case "Family Room":
                return 5;
            case "Suite Room":
                return 4;
            default:
                return 0;
        }
    }

    private double getRoomTypePrice(String roomType) {
        switch (roomType) {
            case "Standard Room":
                return 150.00;
            case "Deluxe Room":
                return 220.00;
            case "Family Room":
                return 300.00;
            case "Suite Room":
                return 350.00;
            default:
                return 0.00;
        }
    }

    private boolean confirmBooking(Guest guest, LocalDate checkInDate,
            LocalDate checkOutDate, int numberOfGuests, String roomType,
            BookingType bookingType) {
        long numberOfNights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        double totalPrice = numberOfNights * getRoomTypePrice(roomType);

        System.out.println("\n--- Booking Summary ---");
        System.out.println("Guest Name       : " + guest.getFullName());
        System.out.println("Guest ID         : " + guest.getGuestId());
        System.out.println("Phone Number     : " + guest.getPhoneNumber());
        System.out.println("Email            : " + guest.getEmail());
        System.out.println("Booking Type     : " + bookingType);
        System.out.println("Check-in Date    : " + checkInDate);
        System.out.println("Check-out Date   : " + checkOutDate);
        System.out.println("Number of Guests : " + numberOfGuests);
        System.out.println("Room Type        : " + roomType);
        System.out.println("Number of Nights : " + numberOfNights);
        System.out.printf("Estimated Price  : RM%.2f%n", totalPrice);
        System.out.print("Confirm booking? (Y/N): ");

        String confirmation = scanner.nextLine().trim();
        return confirmation.equalsIgnoreCase("Y");
    }

    private void searchReservation() {
        System.out.print("Enter confirmation number: ");
        String confirmationNumber = scanner.nextLine().trim();
        Reservation reservation = reservationManager.findByConfirmationNumber(confirmationNumber);

        if (reservation == null) {
            System.out.println("Reservation not found.");
        } else {
            System.out.println(reservation);
        }
    }

    private void cancelReservation() {
        System.out.print("Enter confirmation number to cancel: ");
        String confirmationNumber = scanner.nextLine().trim();
        boolean cancelled = reservationManager.cancelReservation(confirmationNumber);

        if (cancelled) {
            System.out.println("Reservation cancelled successfully.");
        } else {
            System.out.println("Reservation not found.");
        }
    }

    private void displayReservationResult(Reservation reservation) {
        if (reservation == null) {
            return;
        }

        System.out.println("Reservation created successfully.");
        System.out.println(reservation);
    }

    public void displayReservations(ListInterface<Reservation> reservations) {
        if (reservations.isEmpty()) {
            System.out.println("No reservation record found.");
            return;
        }

        for (int position = 1; position <= reservations.getNumberOfEntries(); position++) {
            System.out.println(reservations.getEntry(position));
        }
    }

    public ReservationManager getReservationManager() {
        return reservationManager;
    }

    public static void main(String[] args) {
        new ReservationUI().start();
    }
}
