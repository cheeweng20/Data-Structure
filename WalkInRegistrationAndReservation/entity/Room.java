package WalkInRegistrationAndReservation.entity;

import java.io.Serializable;
import java.util.Objects;

// Stores room details used during reservation and room assignment.
/**
 * @author Wan Yin
 */
public class Room implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum RoomStatus {
        AVAILABLE,
        RESERVED,
        OCCUPIED,
        CLEANING,
        MAINTENANCE
    }

    private String roomNumber;
    private String roomType;
    private int capacity;
    private double pricePerNight;
    private RoomStatus status;

    public Room() {
        status = RoomStatus.AVAILABLE;
    }

    public Room(String roomNumber, String roomType, double pricePerNight) {
        this(roomNumber, roomType, 0, pricePerNight, RoomStatus.AVAILABLE);
    }

    public Room(String roomNumber, String roomType, double pricePerNight,
            RoomStatus status) {
        this(roomNumber, roomType, 0, pricePerNight, status);
    }

    public Room(String roomNumber, String roomType, int capacity,
            double pricePerNight, RoomStatus status) {
        this.roomNumber = roomNumber;
        this.roomType = roomType;
        this.capacity = capacity;
        this.pricePerNight = pricePerNight;
        this.status = status;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public double getPricePerNight() {
        return pricePerNight;
    }

    public void setPricePerNight(double pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Room)) {
            return false;
        }
        Room other = (Room) object;
        return Objects.equals(roomNumber, other.roomNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomNumber);
    }

    @Override
    public String toString() {
        return String.format(
                "Room: %s | Type: %s | Capacity: %d | Rate: RM %.2f | Status: %s",
                roomNumber, roomType, capacity, pricePerNight, status);
    }
}
