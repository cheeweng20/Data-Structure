package VIPPriorityRoomAllocation.entity;

import java.io.Serializable;
import java.util.Objects;

// Stores the guest profile used for priority room allocation.
/**
 * @author Wan Yin
 */
public class Guest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String guestId;
    private String fullName;
    private String phoneNumber;
    private LoyaltyTier loyaltyTier;

    public Guest() {
        loyaltyTier = LoyaltyTier.CLASSIC;
    }

    public Guest(String guestId, String fullName, String phoneNumber, LoyaltyTier loyaltyTier) {
        this.guestId = guestId;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.loyaltyTier = loyaltyTier == null ? LoyaltyTier.CLASSIC : loyaltyTier;
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

    public LoyaltyTier getLoyaltyTier() {
        return loyaltyTier == null ? LoyaltyTier.CLASSIC : loyaltyTier;
    }

    public void setLoyaltyTier(LoyaltyTier loyaltyTier) {
        this.loyaltyTier = loyaltyTier == null ? LoyaltyTier.CLASSIC : loyaltyTier;
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
    public int hashCode() {
        return Objects.hash(guestId);
    }

    @Override
    public String toString() {
        return String.format("Guest ID: %s | Name: %s | Phone: %s | Tier: %s",
                guestId, fullName, phoneNumber, getLoyaltyTier());
    }
}
