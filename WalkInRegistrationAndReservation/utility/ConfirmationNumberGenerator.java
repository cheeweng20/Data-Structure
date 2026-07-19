package WalkInRegistrationAndReservation.utility;

import java.security.SecureRandom;

//Generates eight-digit reservation confirmation numbers.
/**
 * @author Wan Yin
 */

public final class ConfirmationNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private ConfirmationNumberGenerator() {
    }

    public static String generate() {
        int number = 10_000_000 + RANDOM.nextInt(90_000_000);
        return Integer.toString(number);
    }
}
