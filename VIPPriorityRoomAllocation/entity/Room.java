package VIPPriorityRoomAllocation.entity;

import java.io.Serializable;
import java.util.Objects;

// Stores a single-type hotel room used during priority allocation.
/**
 * @author Wan Yin
 */
public class Room implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final String ROOM_TYPE = "Standard Room";

    public enum RoomStatus {
        AVAILABLE,
        RESERVED,
        OCCUPIED,
        NEEDS_CLEANING
    }

    private String roomNumber;
    private double pricePerNight;
    private RoomStatus status;

    public Room() {
        status = RoomStatus.AVAILABLE;
    }

    public Room(String roomNumber, double pricePerNight, RoomStatus status) {
        this.roomNumber = roomNumber;
        this.pricePerNight = pricePerNight;
        this.status = status;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
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
        return String.format("Room: %s | Type: %s | Rate: RM %.2f | Status: %s",
                roomNumber, ROOM_TYPE, pricePerNight, status);
    }
}
