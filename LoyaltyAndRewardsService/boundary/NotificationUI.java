package LoyaltyAndRewardsService.boundary;

import java.util.Iterator;

import LoyaltyAndRewardsService.control.MemberControl;
import LoyaltyAndRewardsService.control.RequestControl;
import LoyaltyAndRewardsService.control.TransactionControl;
import LoyaltyAndRewardsService.entity.Member;
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
            RequestControl requestControl, MemberControl memberControl) {
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

        int unreadUpgradeCount = memberControl.getUnreadTierUpgradeCount();
        if (unreadUpgradeCount > 0) {
            Iterator<Member> iterator = memberControl.getUnreadTierUpgradeIterator();
            while (iterator.hasNext()) {
                Member member = iterator.next();
                MessageUI.displayTierUpgradeAlert(
                        member.getMemberId(),
                        memberControl.getTierName(member.getLastNotifiedTierId()),
                        memberControl.getTierName(member.getTierId()));
            }
            memberControl.markTierUpgradesAsRead();
        } else {
            MessageUI.displayInfo("No unread tier-upgrade notifications.");
        }
        System.out.println();
    }
}
