package HousekeepingAndTaskLog.utility;

import java.time.LocalDateTime;

/**
 * @author Your Name
 */
public class HousekeepingValidator {

    private HousekeepingValidator() {
    }

    public static boolean isNonBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static boolean isValidRoomNumber(String roomNumber) {
        return roomNumber != null && roomNumber.matches("[A-Za-z0-9-]{1,10}");
    }

    public static boolean isValidPriority(int priority) {
        return priority >= 1 && priority <= 5;
    }

    public static boolean isFutureOrPresent(LocalDateTime dateTime) {
        return dateTime != null && !dateTime.isBefore(LocalDateTime.now().minusMinutes(1));
    }
}
