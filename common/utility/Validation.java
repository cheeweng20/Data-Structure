package common.utility;

import java.time.LocalDate;

/** Shared stateless validation rules used by all application modules. */
/**
 * @author Chee Weng
 */
public final class Validation {

    private Validation() {
    }

    public static boolean isNonBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isValidMemberName(String memberName) {
        return memberName != null
                && memberName.trim().matches("^(?=.*[A-Za-z])[A-Za-z .'-]{3,20}$");
    }

    public static boolean isValidPassport(String passport) {
        return passport != null && passport.trim().matches("^[A-Za-z0-9]{5,20}$");
    }

    public static boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.trim().matches("^[0-9+ -]{7,20}$");
    }

    public static String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber == null ? "" : phoneNumber.replaceAll("[^0-9]", "");
    }

    public static boolean isValidConfirmationNumber(String number) {
        return number != null && number.matches("\\d{8}");
    }

    public static boolean isValidRoomNumber(String roomNumber) {
        return roomNumber != null && roomNumber.matches("[A-Za-z0-9-]{1,10}");
    }

    public static boolean isValidStay(LocalDate checkIn, LocalDate checkOut) {
        return checkIn != null && checkOut != null && checkOut.isAfter(checkIn);
    }
}
