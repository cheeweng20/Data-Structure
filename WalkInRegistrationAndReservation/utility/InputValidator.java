package WalkInRegistrationAndReservation.utility;

import java.time.LocalDate;

/**
 * @author Wan Yin
 */

public final class InputValidator {

    private InputValidator() {
    }

    public static boolean isNonBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return isNonBlank(email)
                && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }

    public static boolean isValidPhoneNumber(String phoneNumber) {
        return isNonBlank(phoneNumber)
                && phoneNumber.matches("^[0-9+ -]{7,20}$");
    }

    public static boolean isValidIcOrPassport(String value) {
        return isNonBlank(value)
                && (value.matches("^\\d{12}$")
                        || value.matches("^\\d{6}-\\d{2}-\\d{4}$")
                        || value.matches("^[A-Za-z0-9]{5,20}$"));
    }

    public static boolean isValidName(String name) {
        return isNonBlank(name)
                && name.matches("^(?=.*[A-Za-z])[A-Za-z .'-]{2,50}$");
    }

    public static boolean isValidStay(LocalDate checkIn, LocalDate checkOut) {
        return checkIn != null && checkOut != null && checkOut.isAfter(checkIn);
    }

    public static boolean isPositive(int value) {
        return value > 0;
    }

    public static boolean isEightDigitConfirmationNumber(String number) {
        return number != null && number.matches("\\d{8}");
    }
}
