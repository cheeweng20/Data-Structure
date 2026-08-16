package VIPPriorityRoomAllocation.utility;

import java.security.SecureRandom;

//Generates eight-digit reservation confirmation numbers.
/**
 * @author Wan Yin
 */

public final class ConfirmationNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom(); // safer random number generator

    private ConfirmationNumberGenerator() {
    }

    public static String generate() {
        int number = 10_000_000 + RANDOM.nextInt(90_000_000); // keeps number within 8 digits
        return Integer.toString(number); // convert number to reservation ID text
    }
}
