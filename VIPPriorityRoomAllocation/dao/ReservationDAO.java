package VIPPriorityRoomAllocation.dao;

import VIPPriorityRoomAllocation.entity.Guest;
import VIPPriorityRoomAllocation.entity.LoyaltyTier;
import VIPPriorityRoomAllocation.entity.Reservation;
import VIPPriorityRoomAllocation.entity.ReservationStatus;
import VIPPriorityRoomAllocation.entity.Room;
import VIPPriorityRoomAllocation.entity.Room.RoomStatus;
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

// Loads and saves priority reservation records using CSV files.
/**
 * @author Wan Yin
 */
public class ReservationDAO {

    private static final int INITIAL_CAPACITY = 100;
    private static final String HEADER = "ConfirmationNumber,GuestId,GuestName,PhoneNumber,LoyaltyTier,"
            + "AssignedRoomNumber,AssignedRoomPrice,AssignedRoomStatus,CheckInDate,CheckOutDate,"
            + "BookingDateTime,PaymentMethod,PaymentStatus,Status";
    private final String fileName;

    public ReservationDAO() {
        this("VIPPriorityRoomAllocation/src/reservations.csv");
    }

    public ReservationDAO(String fileName) {
        this.fileName = fileName;
    }

    public ListInterface<Reservation> retrieveFromFile() {
        ListInterface<Reservation> reservations = new ArrayList<>(INITIAL_CAPACITY); // stores loaded CSV records

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            reader.readLine(); // skip CSV header
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] fields = line.split(",", -1); // keep empty room/payment columns
                if (fields.length < 14) {
                    continue; // skip invalid rows
                }

                Guest guest = new Guest(
                        fields[1],
                        fields[2],
                        fields[3],
                        parseLoyaltyTier(fields[4]));
                Room assignedRoom = parseAssignedRoom(fields); // null if room has not been allocated

                reservations.add(new Reservation(
                        fields[0],
                        guest,
                        assignedRoom,
                        LocalDate.parse(fields[8]),
                        LocalDate.parse(fields[9]),
                        LocalDateTime.parse(fields[10]),
                        fields[11],
                        fields[12],
                        ReservationStatus.valueOf(fields[13])));
            }
        } catch (FileNotFoundException ex) {
            createReservationCSVFile(); // create new CSV when running module for the first time
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("Unable to load reservations.", ex);
        }

        return reservations;
    }

    public void saveToFile(ListInterface<Reservation> reservations) {
        createParentDirectory(); // make sure src folder exists before writing

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println(HEADER); // always rewrite CSV with the correct column order

            for (int i = 1; i <= reservations.getNumberOfEntries(); i++) {
                writer.println(toCsvLine(reservations.getEntry(i))); // one reservation becomes one CSV row
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to save reservations.", ex);
        }
    }

    private Room parseAssignedRoom(String[] fields) {
        if (fields[5].isEmpty()) {
            return null; // pending or rejected reservation has no room assigned
        }

        double price = fields[6].isEmpty() ? 0.00 : Double.parseDouble(fields[6]);
        RoomStatus roomStatus = fields[7].isEmpty()
                ? RoomStatus.RESERVED // default when old row has room number but no status
                : RoomStatus.valueOf(fields[7]);
        return new Room(fields[5], price, roomStatus);
    }

    private LoyaltyTier parseLoyaltyTier(String value) {
        return LoyaltyTier.fromTierName(value); // convert CSV text into enum
    }

    private String toCsvLine(Reservation reservation) {
        Guest guest = reservation.getGuest();
        Room assignedRoom = reservation.getAssignedRoom();

        String assignedRoomNumber = ""; // empty values are saved when no room is allocated yet
        String assignedRoomPrice = "";
        String assignedRoomStatus = "";

        if (assignedRoom != null) {
            assignedRoomNumber = assignedRoom.getRoomNumber();
            assignedRoomPrice = Double.toString(assignedRoom.getPricePerNight());
            assignedRoomStatus = assignedRoom.getStatus().toString();
        }

        return reservation.getConfirmationNumber() + ","
                + guest.getGuestId() + ","
                + guest.getFullName() + ","
                + guest.getPhoneNumber() + ","
                + guest.getLoyaltyTier() + ","
                + assignedRoomNumber + ","
                + assignedRoomPrice + ","
                + assignedRoomStatus + ","
                + reservation.getCheckInDate() + ","
                + reservation.getCheckOutDate() + ","
                + reservation.getBookingDateTime() + ","
                + reservation.getPaymentMethod() + ","
                + reservation.getPaymentStatus() + ","
                + reservation.getStatus();
    }

    private void createReservationCSVFile() {
        createParentDirectory(); // create folder first if missing

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
