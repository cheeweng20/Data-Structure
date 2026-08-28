package FrontDeskService.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author Yi Ren
 *
 * <p>This operational record is intentionally separate from a reservation:
 * it does not alter the booked check-out date or the reservation's billing
 * information.</p>
 */
public class LateCheckoutExtension {

    private final String confirmationNumber;
    private final LocalDateTime extendedCheckOutAt;
    private final String reason;

    public LateCheckoutExtension(String confirmationNumber,
            LocalDateTime extendedCheckOutAt, String reason) {
        this.confirmationNumber = requireText(confirmationNumber, "confirmation number");
        this.extendedCheckOutAt = Objects.requireNonNull(extendedCheckOutAt,
                "extended check-out time cannot be null");
        this.reason = requireText(reason, "reason");
    }

    public String getConfirmationNumber() {
        return confirmationNumber;
    }

    public LocalDateTime getExtendedCheckOutAt() {
        return extendedCheckOutAt;
    }

    public String getReason() {
        return reason;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof LateCheckoutExtension)) {
            return false;
        }
        LateCheckoutExtension other = (LateCheckoutExtension) object;
        return confirmationNumber.equalsIgnoreCase(other.confirmationNumber);
    }

    @Override
    public int hashCode() {
        return confirmationNumber.toLowerCase().hashCode();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        if (value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(fieldName + " cannot contain a line break");
        }
        return value.trim();
    }
}
