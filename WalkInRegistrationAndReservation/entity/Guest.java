package WalkInRegistrationAndReservation.entity;

import java.io.Serializable;
import java.util.Objects;

// store personal data of the guest who made the reservation
/**
 * @author Wan Yin
 */

public class Guest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String guestId;
    private String fullName;
    private String phoneNumber;
    private String email;

    public Guest() {
    }

    public Guest(String guestId, String fullName, String phoneNumber, String email) {
        this.guestId = guestId;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.email = email;
    }

    public String getGuestId() {
        return guestId;
    }

    public void setGuestId(String guestId) {
        this.guestId = guestId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Guest)) {
            return false;
        }
        Guest other = (Guest) object;
        return Objects.equals(guestId, other.guestId);
    }

    @Override
    public int hashCode() { // hash: number use to compare the object
        return Objects.hash(guestId);
    }

    @Override
    public String toString() {
        return String.format("Guest ID: %s | Name: %s | Phone: %s | Email: %s",
                guestId, fullName, phoneNumber, email);
    }
}
