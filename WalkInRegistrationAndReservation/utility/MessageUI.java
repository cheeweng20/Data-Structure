package WalkInRegistrationAndReservation.utility;

// Displays messages shared by the reservation boundary classes.
/**
 * @author Wan Yin
 */
public final class MessageUI {

    private MessageUI() {
    }

    public static void displaySuccess(String message) {
        System.out.println("[SUCCESS] " + message);
    }

    public static void displayError(String message) {
        System.out.println("[ERROR] " + message);
    }

    public static void displayInfo(String message) {
        System.out.println("[INFO] " + message);
    }
}
