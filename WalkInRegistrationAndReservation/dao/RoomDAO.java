package WalkInRegistrationAndReservation.dao;

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

// Loads and saves room records using CSV files.
/**
 * @author Wan Yin
 */
public class RoomDAO {

    private static final int INITIAL_CAPACITY = 100;
    private static final String HEADER = "RoomNumber,RoomType,Capacity,PricePerNight,Status";
    private final String fileName;

    public RoomDAO() {
        this("WalkInRegistrationAndReservation/src/rooms.csv");
    }

    public RoomDAO(String fileName) {
        this.fileName = fileName;
    }

    public ListInterface<Room> retrieveFromFile() {
        ListInterface<Room> rooms = new ArrayList<>(INITIAL_CAPACITY);

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            reader.readLine();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] fields = line.split(",", -1);
                if (fields.length < 4) {
                    continue;
                }

                String roomNumber = fields[0];
                String roomType = fields[1];
                int capacity;
                double pricePerNight;
                RoomStatus status;

                if (fields.length >= 5) {
                    capacity = Integer.parseInt(fields[2]);
                    pricePerNight = Double.parseDouble(fields[3]);
                    status = RoomStatus.valueOf(fields[4]);
                } else {
                    capacity = 0;
                    pricePerNight = Double.parseDouble(fields[2]);
                    status = RoomStatus.valueOf(fields[3]);
                }

                rooms.add(new Room(roomNumber, roomType, capacity, pricePerNight, status));
            }
        } catch (FileNotFoundException ex) {
            createRoomCSVFile();
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("Unable to load rooms.", ex);
        }

        return rooms;
    }

    public void saveToFile(ListInterface<Room> rooms) {
        createParentDirectory();

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println(HEADER);

            for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
                Room room = rooms.getEntry(i);
                writer.println(toCsvLine(room));
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to save rooms.", ex);
        }
    }

    private String toCsvLine(Room room) {
        return room.getRoomNumber() + ","
                + room.getRoomType() + ","
                + room.getCapacity() + ","
                + room.getPricePerNight() + ","
                + room.getStatus();
    }

    private void createRoomCSVFile() {
        createParentDirectory();

        try (PrintWriter writer = new PrintWriter(fileName)) {
            writer.println(HEADER);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to create room CSV file.", ex);
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
