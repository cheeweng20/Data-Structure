package WalkInRegistrationAndReservation.dao;

import adt.ArrayList;
import adt.ListInterface;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import WalkInRegistrationAndReservation.entity.Reservation;

// Loads and saves reservation records using object serialization.

public class ReservationDAO {

    private static final int INITIAL_CAPACITY = 100;
    private final String fileName;

    public ReservationDAO() {
        this("data/reservations.dat");
    }

    public ReservationDAO(String fileName) {
        this.fileName = fileName;
    }

    @SuppressWarnings("unchecked")
    public ListInterface<Reservation> retrieveFromFile() {
        File file = new File(fileName);
        if (!file.exists()) {
            return new ArrayList<>(INITIAL_CAPACITY);
        }

        try (ObjectInputStream input = new ObjectInputStream(
                new FileInputStream(file))) {
            return (ListInterface<Reservation>) input.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException ex) {
            throw new IllegalStateException("Unable to load reservations.", ex);
        }
    }

    public void saveToFile(ListInterface<Reservation> reservations) {
        File file = new File(fileName);
        createParentDirectory(file);

        try (ObjectOutputStream output = new ObjectOutputStream(
                new FileOutputStream(file))) {
            output.writeObject(reservations);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to save reservations.", ex);
        }
    }

    private void createParentDirectory(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create data directory.");
        }
    }
}
