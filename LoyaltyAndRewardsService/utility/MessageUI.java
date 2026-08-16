package LoyaltyAndRewardsService.utility;

/**
 * Displays consistent user-facing messages for the Loyalty and Rewards UI.
 *
 * @author Chee Weng
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

    public static void displayRequestProcessed(String status) {
        if ("Approved".equalsIgnoreCase(status)) {
            displaySuccess("Request approved.");
        } else {
            displayInfo("Request " + status + ".");
        }
    }

    public static void displayPointsExpired(int expiredPoints) {
        displayInfo(expiredPoints
                + " unredeemed transaction point(s) expired; member balances were updated.");
    }

    public static void displayTierUpgradeAlert(String memberId, String previousTierName,
            String currentTierName) {
        displayInfo("Member " + memberId + " upgraded from " + previousTierName
                + " to " + currentTierName + ".");
    }
}
