package WalkInRegistrationAndReservation.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

// Stores a VIP priority reservation and its automatic room assignment.
/**
 * @author Wan Yin
 */
public class Reservation implements Serializable, Comparable<Reservation> {

    private static final long serialVersionUID = 1L;

    private String confirmationNumber;
    private Guest guest;
    private Room assignedRoom;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private LocalDateTime bookingDateTime;
    private BookingType bookingType;
    private String paymentMethod;
    private String paymentStatus;
    private ReservationStatus status;

    public Reservation() {
        bookingDateTime = LocalDateTime.now();
        bookingType = BookingType.VIP_PRIORITY;
        paymentMethod = "";
        paymentStatus = "UNPAID";
        status = ReservationStatus.PENDING;
    }

    public Reservation(String confirmationNumber, Guest guest,
            LocalDate checkInDate, LocalDate checkOutDate) {
        this(confirmationNumber, guest, null, checkInDate, checkOutDate,
                LocalDateTime.now(), BookingType.VIP_PRIORITY, "", "UNPAID",
                ReservationStatus.PENDING);
    }

    public Reservation(String confirmationNumber, Guest guest, Room assignedRoom,
            LocalDate checkInDate, LocalDate checkOutDate,
            LocalDateTime bookingDateTime, BookingType bookingType,
            String paymentMethod, String paymentStatus, ReservationStatus status) {
        this.confirmationNumber = confirmationNumber;
        this.guest = guest;
        this.assignedRoom = assignedRoom;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.bookingDateTime = bookingDateTime;
        this.bookingType = bookingType;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
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

    public BookingType getBookingType() {
        return bookingType;
    }

    public void setBookingType(BookingType bookingType) {
        this.bookingType = bookingType;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
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
    public int compareTo(Reservation other) {
        int bookingDateTimeCompare = bookingDateTime.compareTo(other.bookingDateTime);
        if (bookingDateTimeCompare != 0) {
            return bookingDateTimeCompare;
        }
        return confirmationNumber.compareToIgnoreCase(other.confirmationNumber);
    }

    @Override
    public String toString() {
        String roomNumber = assignedRoom == null ? "Not assigned" : assignedRoom.getRoomNumber();
        String guestName = guest == null ? "Unknown" : guest.getFullName();
        String tier = guest == null ? "Unknown" : guest.getLoyaltyTier().toString();

        return String.format(
                "Confirmation: %s | Guest: %s | Tier: %s | Room: %s | Check-in: %s | Check-out: %s | Status: %s",
                confirmationNumber, guestName, tier, roomNumber,
                checkInDate, checkOutDate, status);
    }
}
