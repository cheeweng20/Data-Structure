package LoyaltyAndRewardsService.boundary;

import java.util.Scanner;

import LoyaltyAndRewardsService.control.MemberControl;
import LoyaltyAndRewardsService.control.TierControl;
import LoyaltyAndRewardsService.utility.MessageUI;
import LoyaltyAndRewardsService.utility.Verification;
import common.src.InputHelper;

/**
 * Handles actor interaction for tier-maintenance use cases.
 *
 * @author Chee Weng
 */
public class TierUI {
    public static void tierOperator(Scanner scanner, TierControl tierControl,
            MemberControl memberControl) {
        boolean exit = false;

        while (!exit) {
            System.out.println("\r\n"
                    + ".-----.------------------------.\r\n"
                    + "| No. |        Function        |\r\n"
                    + ":-----+------------------------:\r\n"
                    + "|  1. | New Tier Level         |\r\n"
                    + ":-----+------------------------:\r\n"
                    + "|  2. | Remove Tier Level      |\r\n"
                    + ":-----+------------------------:\r\n"
                    + "|  3. | Update Tier Level Info |\r\n"
                    + ":-----+------------------------:\r\n"
                    + "|  4. | Tier List              |\r\n"
                    + "'-----'------------------------'\r\n");

            int userEntry = InputHelper.inputInt(scanner, "Please enter a number (0 to exit): ");
            switch (userEntry) {
                case 1:
                    addTier(scanner, tierControl, memberControl);
                    break;
                case 2:
                    removeTier(scanner, tierControl, memberControl);
                    break;
                case 3:
                    updateTier(scanner, tierControl, memberControl);
                    break;
                case 4:
                    displayTierTable(tierControl);
                    break;
                case 0:
                    exit = true;
                    break;
                default:
                    MessageUI.displayError("Invalid option.");
                    break;
            }
        }
    }

    private static void addTier(Scanner scanner, TierControl tierControl,
            MemberControl memberControl) {
        scanner.nextLine();
        String tierName = InputHelper.inputString(scanner, "Enter tier name: ");
        int minimumPoint = InputHelper.inputInt(scanner, "Enter minimum points: ");

        if (tierControl.isEmpty() && minimumPoint != 0) {
            MessageUI.displayError("The first tier must start at 0 points.");
            return;
        }
        if (!Verification.verifyTierName(tierName, tierControl)
                || !Verification.verifyTierPoints(minimumPoint, 0)) {
            return;
        }
        if (!tierControl.isMinimumPointAvailable(minimumPoint, null)) {
            MessageUI.displayError("Another tier already uses that minimum point.");
            return;
        }

        tierControl.createTier(tierName, minimumPoint, memberControl);
    }

    private static void removeTier(Scanner scanner, TierControl tierControl,
            MemberControl memberControl) {
        if (tierControl.isEmpty()) {
            MessageUI.displayInfo("No tier records found.");
            return;
        }

        scanner.nextLine();
        displayTierTable(tierControl);
        String tierId = InputHelper.inputString(scanner, "Enter tier ID: ");
        if (!tierControl.findTier(tierId)) {
            MessageUI.displayError("Tier level not found.");
            return;
        }
        if (tierControl.size() > 1 && tierControl.isBaseTier(tierId)) {
            MessageUI.displayError("The base tier cannot be deleted while higher tiers exist.");
            return;
        }

        tierControl.removeTier(tierId, memberControl);
    }

    private static void updateTier(Scanner scanner, TierControl tierControl,
            MemberControl memberControl) {
        if (tierControl.isEmpty()) {
            MessageUI.displayInfo("No tier records found.");
            return;
        }

        scanner.nextLine();
        displayTierTable(tierControl);
        String tierId = InputHelper.inputString(scanner, "Enter tier ID to update: ");
        if (!tierControl.findTier(tierId)) {
            MessageUI.displayError("Tier level not found.");
            return;
        }

        String newName = InputHelper.inputString(scanner, "Enter new tier name: ");
        int minimumPoint = InputHelper.inputInt(scanner, "Enter new minimum points: ");
        if (tierControl.isBaseTier(tierId) && minimumPoint != 0) {
            MessageUI.displayError("The base tier must continue to start at 0 points.");
            return;
        }
        if (!Verification.verifyTierPoints(minimumPoint, 0)
                || !Verification.verifyTierName(newName, tierId, tierControl)) {
            return;
        }
        if (!tierControl.isMinimumPointAvailable(minimumPoint, tierId)) {
            MessageUI.displayError("Another tier already uses that minimum point.");
            return;
        }

        tierControl.updateTier(tierId, newName, minimumPoint, memberControl);
    }

    private static void displayTierTable(TierControl tierControl) {
        String table = tierControl.getTierTable();
        if (table.isEmpty()) {
            MessageUI.displayInfo("No tier records found.");
        } else {
            System.out.println(table);
        }
    }

}
