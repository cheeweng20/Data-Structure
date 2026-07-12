package WalkInRegistrationAndReservation.dao;

import adt.ArrayList;
import adt.ListInterface;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import WalkInRegistrationAndReservation.entity.Room;

// Loads and saves room records via object serialization.

public class RoomDAO {

    private static final int INITIAL_CAPACITY = 100;
    private final String fileName;

    public RoomDAO() {
        this("data/rooms.dat");
    }

    public RoomDAO(String fileName) {
        this.fileName = fileName;
    }

    @SuppressWarnings("unchecked")
    public ListInterface<Room> retrieveFromFile() {
        File file = new File(fileName);
        if (!file.exists()) {
            return new ArrayList<>(INITIAL_CAPACITY);
        }

        try (ObjectInputStream input = new ObjectInputStream(
                new FileInputStream(file))) {
            return (ListInterface<Room>) input.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException ex) {
            throw new IllegalStateException("Unable to load rooms.", ex);
        }
    }

    public void saveToFile(ListInterface<Room> rooms) {
        File file = new File(fileName);
        createParentDirectory(file);

        try (ObjectOutputStream output = new ObjectOutputStream(
                new FileOutputStream(file))) {
            output.writeObject(rooms);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to save rooms.", ex);
        }
    }

    private void createParentDirectory(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create data directory.");
        }
    }
}
