package WalkInRegistrationAndReservation.boundary;

import WalkInRegistrationAndReservation.control.ReservationManager;
import WalkInRegistrationAndReservation.entity.BookingType;
import WalkInRegistrationAndReservation.entity.Guest;
import WalkInRegistrationAndReservation.entity.Reservation;
import WalkInRegistrationAndReservation.utility.InputValidator;
import adt.ListInterface;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * Provides the console interface for standard and walk-in reservations.
 */
public class ReservationUI {

    private final ReservationManager reservationManager;
    private final Scanner scanner;

    public ReservationUI() {
        scanner = new Scanner(System.in);
        reservationManager = new ReservationManager();
    }

    public void start() {
        // TODO: Build the menu loop for standard booking, walk-in registration,
        // search, update, cancellation and queue display.
        throw new UnsupportedOperationException("Student implementation required.");
    }

    public Reservation inputReservation(BookingType bookingType) {
        System.out.print("Guest ID: ");
        String guestId = scanner.nextLine().trim();
        System.out.print("Full name: ");
        String fullName = scanner.nextLine().trim();
        System.out.print("Phone number: ");
        String phoneNumber = scanner.nextLine().trim();
        System.out.print("Email: ");
        String email = scanner.nextLine().trim();
        System.out.print("Requested room type: ");
        String requestedRoomType = scanner.nextLine().trim();
        System.out.print("Check-in date (yyyy-MM-dd): ");
        String checkInInput = scanner.nextLine().trim();
        System.out.print("Check-out date (yyyy-MM-dd): ");
        String checkOutInput = scanner.nextLine().trim();
        System.out.print("Number of guests: ");
        String guestsInput = scanner.nextLine().trim();

        if (!InputValidator.isNonBlank(guestId)
                || !InputValidator.isNonBlank(fullName)
                || !InputValidator.isNonBlank(requestedRoomType)) {
            System.out.println("Guest ID, full name and room type are required.");
            return null;
        }
        if (!InputValidator.isValidPhoneNumber(phoneNumber)) {
            System.out.println("Invalid phone number.");
            return null;
        }
        if (!InputValidator.isValidEmail(email)) {
            System.out.println("Invalid email address.");
            return null;
        }

        LocalDate checkInDate;
        LocalDate checkOutDate;
        int numberOfGuests;
        try {
            checkInDate = LocalDate.parse(checkInInput);
            checkOutDate = LocalDate.parse(checkOutInput);
            numberOfGuests = Integer.parseInt(guestsInput);
        } catch (DateTimeParseException ex) {
            System.out.println("Dates must use yyyy-MM-dd format.");
            return null;
        } catch (NumberFormatException ex) {
            System.out.println("Number of guests must be a whole number.");
            return null;
        }

        if (!InputValidator.isValidStay(checkInDate, checkOutDate)) {
            System.out.println("Check-out date must be after check-in date.");
            return null;
        }
        if (!InputValidator.isPositive(numberOfGuests)) {
            System.out.println("Number of guests must be greater than zero.");
            return null;
        }

        Guest guest = new Guest(guestId, fullName, phoneNumber, email);
        return reservationManager.createReservation(guest, requestedRoomType,
                checkInDate, checkOutDate, numberOfGuests, bookingType);
    }

    public void displayReservations(ListInterface<Reservation> reservations) {
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
