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

    public static void displayRequestSubmitted(String rewardName) {
        displaySuccess("Request for " + rewardName
                + " submitted and is waiting to be processed.");
    }

    public static void displayRequestProcessed(String status) {
        if ("Approved".equalsIgnoreCase(status)) {
            displaySuccess("Request approved.");
        } else {
            displayInfo("Request " + status + ".");
        }
    }

    public static void displayTierChange(String previousTierName, String currentTierName) {
        displayInfo("Tier changed: " + previousTierName + " -> " + currentTierName);
    }

    public static void displayPointsAdded(int addedPoints, int currentPoints) {
        displaySuccess(addedPoints + " points added successfully.");
        displayInfo("Current points: " + currentPoints);
    }

    public static void displayPointsExpired(int expiredPoints) {
        displayInfo(expiredPoints
                + " unredeemed transaction point(s) expired; member balances were updated.");
    }

    public static void displayTierAdded(String tierId) {
        displaySuccess("Tier " + tierId + " added successfully.");
    }

    public static void displayTierDeleted() {
        displaySuccess("Tier level deleted successfully.");
    }

    public static void displayTierUpdated() {
        displaySuccess("Tier level updated successfully.");
    }

    public static void displayTierRecalculation(int updatedMemberCount) {
        if (updatedMemberCount > 0) {
            displayInfo(updatedMemberCount + " member tier(s) were recalculated.");
        }
    }

    public static void displayTierUpgradeAlert(String memberId, String previousTierName,
            String currentTierName) {
        displayInfo("Member " + memberId + " upgraded from " + previousTierName
                + " to " + currentTierName + ".");
    }
}
