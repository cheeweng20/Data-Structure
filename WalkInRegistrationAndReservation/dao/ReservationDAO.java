package WalkInRegistrationAndReservation.dao;

import WalkInRegistrationAndReservation.entity.BookingType;
import WalkInRegistrationAndReservation.entity.Guest;
import WalkInRegistrationAndReservation.entity.Reservation;
import WalkInRegistrationAndReservation.entity.ReservationStatus;
import WalkInRegistrationAndReservation.entity.Room;
import WalkInRegistrationAndReservation.entity.Room.RoomStatus;
import adt.ArrayList;
import adt.ListInterface;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;

// Loads and saves reservation records using CSV files.
/**
 * @author Wan Yin
 */
public class ReservationDAO {

    private static final int INITIAL_CAPACITY = 100;
    private static final String HEADER = "ConfirmationNumber,GuestId,GuestName,PhoneNumber,Email,"
            + "RequestedRoomType,AssignedRoomNumber,AssignedRoomType,AssignedRoomCapacity,AssignedRoomPrice,"
            + "AssignedRoomStatus,CheckInDate,CheckOutDate,BookingDateTime,NumberOfGuests,BookingType,"
            + "PaymentMethod,PaymentStatus,Status";
    private final String fileName;

    public ReservationDAO() {
        this("WalkInRegistrationAndReservation/src/reservations.csv");
    }

    public ReservationDAO(String fileName) {
        this.fileName = fileName;
    }

    public ListInterface<Reservation> retrieveFromFile() {
        ListInterface<Reservation> reservations = new ArrayList<>(INITIAL_CAPACITY);

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            reader.readLine();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] fields = line.split(",", -1);
                if (fields.length < 16) {
                    continue;
                }

                Guest guest = new Guest(fields[1], fields[2], fields[3], fields[4]);
                Room assignedRoom = parseAssignedRoom(fields);
                boolean hasPaymentColumns = fields.length >= 19;
                int checkInDateIndex = hasPaymentColumns ? 11 : 10;
                int checkOutDateIndex = hasPaymentColumns ? 12 : 11;
                int bookingDateTimeIndex = hasPaymentColumns ? 13 : 12;
                int numberOfGuestsIndex = hasPaymentColumns ? 14 : 13;
                int bookingTypeIndex = hasPaymentColumns ? 15 : 14;
                int paymentMethodIndex = hasPaymentColumns ? 16 : -1;
                int paymentStatusIndex = hasPaymentColumns ? 17 : -1;
                int statusIndex = hasPaymentColumns ? 18 : 15;

                String paymentMethod = paymentMethodIndex == -1 ? "" : fields[paymentMethodIndex];
                String paymentStatus = paymentStatusIndex == -1 ? "UNPAID" : fields[paymentStatusIndex];

                Reservation reservation = new Reservation(
                        fields[0],
                        guest,
                        fields[5],
                        assignedRoom,
                        LocalDate.parse(fields[checkInDateIndex]),
                        LocalDate.parse(fields[checkOutDateIndex]),
                        LocalDateTime.parse(fields[bookingDateTimeIndex]),
                        Integer.parseInt(fields[numberOfGuestsIndex]),
                        BookingType.valueOf(fields[bookingTypeIndex]),
                        paymentMethod,
                        paymentStatus,
                        ReservationStatus.valueOf(fields[statusIndex]));

                reservations.add(reservation);
            }
        } catch (FileNotFoundException ex) {
            createReservationCSVFile();
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("Unable to load reservations.", ex);
        }

        return reservations;
    }

    public void saveToFile(ListInterface<Reservation> reservations) {
        createParentDirectory();

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println(HEADER);

            for (int i = 1; i <= reservations.getNumberOfEntries(); i++) {
                writer.println(toCsvLine(reservations.getEntry(i)));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to save reservations.", ex);
        }
    }

    private Room parseAssignedRoom(String[] fields) {
        if (fields[6].isEmpty()) {
            return null;
        }

        boolean hasCapacityColumn = fields.length >= 19;
        int capacity = hasCapacityColumn && !fields[8].isEmpty()
                ? Integer.parseInt(fields[8])
                : 0;
        int priceIndex = hasCapacityColumn ? 9 : 8;
        int statusIndex = hasCapacityColumn ? 10 : 9;
        double price = fields[priceIndex].isEmpty()
                ? 0.00
                : Double.parseDouble(fields[priceIndex]);
        RoomStatus roomStatus = fields[statusIndex].isEmpty()
                ? RoomStatus.RESERVED
                : RoomStatus.valueOf(fields[statusIndex]);

        return new Room(fields[6], fields[7], capacity, price, roomStatus);
    }

    private String toCsvLine(Reservation reservation) {
        Guest guest = reservation.getGuest();
        Room assignedRoom = reservation.getAssignedRoom();

        String assignedRoomNumber = "";
        String assignedRoomType = "";
        String assignedRoomCapacity = "";
        String assignedRoomPrice = "";
        String assignedRoomStatus = "";

        if (assignedRoom != null) {
            assignedRoomNumber = assignedRoom.getRoomNumber();
            assignedRoomType = assignedRoom.getRoomType();
            assignedRoomCapacity = Integer.toString(assignedRoom.getCapacity());
            assignedRoomPrice = Double.toString(assignedRoom.getPricePerNight());
            assignedRoomStatus = assignedRoom.getStatus().toString();
        }

        return reservation.getConfirmationNumber() + ","
                + guest.getGuestId() + ","
                + guest.getFullName() + ","
                + guest.getPhoneNumber() + ","
                + guest.getEmail() + ","
                + reservation.getRequestedRoomType() + ","
                + assignedRoomNumber + ","
                + assignedRoomType + ","
                + assignedRoomCapacity + ","
                + assignedRoomPrice + ","
                + assignedRoomStatus + ","
                + reservation.getCheckInDate() + ","
                + reservation.getCheckOutDate() + ","
                + reservation.getBookingDateTime() + ","
                + reservation.getNumberOfGuests() + ","
                + reservation.getBookingType() + ","
                + reservation.getPaymentMethod() + ","
                + reservation.getPaymentStatus() + ","
                + reservation.getStatus();
    }

    private void createReservationCSVFile() {
        createParentDirectory();

        try (PrintWriter writer = new PrintWriter(fileName)) {
            writer.println(HEADER);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create reservation CSV file.", ex);
        }
    }

    private void createParentDirectory() {
        File file = new File(fileName);
        File parent = file.getParentFile();

        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create data directory.");
        }
    }
}
