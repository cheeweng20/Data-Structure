package WalkInRegistrationAndReservation.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

//Stores standard or walk-in reservation and its room assignment.
public class Reservation implements Serializable {

    private static final long serialVersionUID = 1L;

    private String confirmationNumber;
    private Guest guest;
    private String requestedRoomType;
    private Room assignedRoom;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private LocalDateTime bookingDateTime;
    private int numberOfGuests;
    private BookingType bookingType;
    private ReservationStatus status;

    public Reservation() {
        bookingDateTime = LocalDateTime.now();
        status = ReservationStatus.PENDING;
    }

    public Reservation(String confirmationNumber, Guest guest,String requestedRoomType, LocalDate checkInDate, LocalDate checkOutDate, int numberOfGuests, BookingType bookingType) {
        this(confirmationNumber, guest, requestedRoomType, null, checkInDate, checkOutDate, LocalDateTime.now(), numberOfGuests, bookingType, ReservationStatus.PENDING);
    }

    public Reservation(String confirmationNumber, Guest guest, String requestedRoomType, Room assignedRoom, LocalDate checkInDate,LocalDate checkOutDate, 
        LocalDateTime bookingDateTime, int numberOfGuests, BookingType bookingType,ReservationStatus status) {
        this.confirmationNumber = confirmationNumber;
        this.guest = guest;
        this.requestedRoomType = requestedRoomType;
        this.assignedRoom = assignedRoom;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.bookingDateTime = bookingDateTime;
        this.numberOfGuests = numberOfGuests;
        this.bookingType = bookingType;
        this.status = status;
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public void setConfirmationNumber(String confirmationNumber) {
        this.confirmationNumber = confirmationNumber;
    }

    public Guest getGuest() {
        return guest;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
    }

    public String getRequestedRoomType() {
        return requestedRoomType;
    }

    public void setRequestedRoomType(String requestedRoomType) {
        this.requestedRoomType = requestedRoomType;
    }

    public Room getAssignedRoom() {
        return assignedRoom;
    }

    public void setAssignedRoom(Room assignedRoom) {
        this.assignedRoom = assignedRoom;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public LocalDateTime getBookingDateTime() {
        return bookingDateTime;
    }

    public void setBookingDateTime(LocalDateTime bookingDateTime) {
        this.bookingDateTime = bookingDateTime;
    }

    public int getNumberOfGuests() {
        return numberOfGuests;
    }

    public void setNumberOfGuests(int numberOfGuests) {
        this.numberOfGuests = numberOfGuests;
    }

    public BookingType getBookingType() {
        return bookingType;
    }

    public void setBookingType(BookingType bookingType) {
        this.bookingType = bookingType;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Reservation)) {
            return false;
        }
        Reservation other = (Reservation) object;
        return Objects.equals(confirmationNumber, other.confirmationNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(confirmationNumber);
    }

    @Override
    public String toString() {
        String roomNumber = assignedRoom == null
                ? "Not assigned" : assignedRoom.getRoomNumber();
        String guestName = guest == null ? "Unknown" : guest.getFullName();

        return String.format(
                "Confirmation: %s | Guest: %s | Room type: %s | Room: %s | "
                + "Check-in: %s | Check-out: %s | Type: %s | Status: %s",
                confirmationNumber, guestName, requestedRoomType, roomNumber,
                checkInDate, checkOutDate, bookingType, status);
    }
}
