package LoyaltyAndRewardsService.boundary;

import LoyaltyAndRewardsService.control.RequestControl;
import LoyaltyAndRewardsService.control.TransactionControl;
import LoyaltyAndRewardsService.utility.MessageUI;

/**
 * Displays automatic loyalty notifications when the module starts.
 *
 * @author Chee Weng
 */
public final class NotificationUI {
    private static final int DEFAULT_EXPIRY_ALERT_DAYS = 30;

    private NotificationUI() {
    }

    public static void displayStartupNotifications(TransactionControl transactionControl,
            RequestControl requestControl) {
        int expiringTransactionCount =
                transactionControl.getExpiringTransactionCount(DEFAULT_EXPIRY_ALERT_DAYS);
        int expiringPointTotal =
                transactionControl.getExpiringPointTotal(DEFAULT_EXPIRY_ALERT_DAYS);
        int pendingRequestCount = requestControl.getPendingRequestCount();

        System.out.println();
        System.out.println("=== Loyalty Notifications ===");
        if (expiringTransactionCount > 0) {
            MessageUI.displayInfo(expiringPointTotal + " unredeemed point(s) from "
                    + expiringTransactionCount + " transaction(s) will expire within "
                    + DEFAULT_EXPIRY_ALERT_DAYS + " days.");
        } else {
            MessageUI.displayInfo("No unredeemed points expire within "
                    + DEFAULT_EXPIRY_ALERT_DAYS + " days.");
        }

        if (pendingRequestCount > 0) {
            MessageUI.displayInfo(pendingRequestCount
                    + " redemption request(s) are waiting for processing.");
        } else {
            MessageUI.displayInfo("No redemption requests are waiting for processing.");
        }
        System.out.println();
    }
}
