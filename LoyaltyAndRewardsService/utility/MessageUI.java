package LoyaltyAndRewardsService.utility;

/**
 * Displays consistent user-facing messages for the Loyalty and Rewards UI.
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
