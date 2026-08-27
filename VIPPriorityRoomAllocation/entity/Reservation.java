package VIPPriorityRoomAllocation.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

// Stores a VIP priority reservation and its automatic room assignment.
/**
 * @author Wan Yin
 */
public class Reservation implements Comparable<Reservation> {

    private String confirmationNumber;
    private Guest guest;
    private Room assignedRoom; 
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private LocalDateTime temporaryCheckOutAt;
    private LocalDateTime bookingDateTime;
    private String paymentMethod;
    private String paymentStatus;
    private ReservationStatus status;

    public Reservation() {
        bookingDateTime = LocalDateTime.now(); // used to break ties when tier is same
        paymentMethod = "";
        paymentStatus = "UNPAID";
        status = ReservationStatus.PENDING; // new request waits in the priority heap first
    }

    public Reservation(String confirmationNumber, Guest guest,
            LocalDate checkInDate, LocalDate checkOutDate) {
        this(confirmationNumber, guest, null, checkInDate, checkOutDate, // null means no room assigned yet
                LocalDateTime.now(), "", "UNPAID", ReservationStatus.PENDING);
    }

    public Reservation(String confirmationNumber, Guest guest, Room assignedRoom,
            LocalDate checkInDate, LocalDate checkOutDate,
            LocalDateTime bookingDateTime, String paymentMethod,
            String paymentStatus, ReservationStatus status) {
        this.confirmationNumber = confirmationNumber;
        this.guest = guest;
        this.assignedRoom = assignedRoom;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.bookingDateTime = bookingDateTime;
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

    /**
     * Gets the temporary late check-out time, if Front Desk has recorded one.
     * The scheduled check-out date remains unchanged so billing is unaffected.
     */
    public LocalDateTime getTemporaryCheckOutAt() {
        return temporaryCheckOutAt;
    }

    public void setTemporaryCheckOutAt(LocalDateTime temporaryCheckOutAt) {
        this.temporaryCheckOutAt = temporaryCheckOutAt;
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
        return Objects.equals(confirmationNumber, other.confirmationNumber); //same reservation num=same reservation
    }

    @Override
    public int hashCode() {
        return Objects.hash(confirmationNumber);
    }

    @Override
    public int compareTo(Reservation other) {
        // compareTo is the priority formula used by MaxHeapPriorityQueue
        int tierCompare = Integer.compare(
                getGuest().getLoyaltyTier().getPriorityScore(), // higher tier score has higher heap priority
                other.getGuest().getLoyaltyTier().getPriorityScore());

        if (tierCompare != 0) {
            return tierCompare; // different tier: decide by loyalty tier first
        }

        int bookingDateTimeCompare = other.bookingDateTime.compareTo(bookingDateTime); // same tier: earlier request first
        if (bookingDateTimeCompare != 0) {
            return bookingDateTimeCompare;
        }

        return other.confirmationNumber.compareToIgnoreCase(confirmationNumber); // final backup if tier and time are same
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
