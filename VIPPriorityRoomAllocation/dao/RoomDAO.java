package VIPPriorityRoomAllocation.dao;

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

// Loads and saves single-type room records using CSV files.
/**
 * @author Wan Yin
 */
public class RoomDAO {

    private static final int INITIAL_CAPACITY = 100;
    private static final String HEADER = "RoomNumber,PricePerNight,Status";
    private final String fileName;

    public RoomDAO() {
        this("VIPPriorityRoomAllocation/src/rooms.csv");
    }

    public RoomDAO(String fileName) {
        this.fileName = fileName;
    }

    public ListInterface<Room> retrieveFromFile() {
        ListInterface<Room> rooms = new ArrayList<>(INITIAL_CAPACITY); // stores loaded room records

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            reader.readLine(); // skip CSV header
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] fields = line.split(",", -1); // keep empty CSV columns
                if (fields.length < 3) {
                    continue; // skip invalid rows
                }

                rooms.add(new Room(
                        fields[0],
                        Double.parseDouble(fields[1]),
                        RoomStatus.valueOf(fields[2]))); // create Room object from CSV row
            }
        } catch (FileNotFoundException ex) {
            createRoomCSVFile(); // create new CSV when running module for the first time
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("Unable to load rooms.", ex);
        }

        return rooms;
    }

    public void saveToFile(ListInterface<Room> rooms) {
        createParentDirectory(); // make sure src folder exists before writing

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println(HEADER); // always rewrite CSV with correct column order

            for (int i = 1; i <= rooms.getNumberOfEntries(); i++) {
                writer.println(toCsvLine(rooms.getEntry(i))); // one room becomes one CSV row
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to save rooms.", ex);
        }
    }

    private String toCsvLine(Room room) {
        return room.getRoomNumber() + ","
                + room.getPricePerNight() + ","
                + room.getStatus();
    }

    private void createRoomCSVFile() {
        createParentDirectory(); // create folder first if missing

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
