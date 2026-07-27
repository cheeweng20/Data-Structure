package LoyaltyAndRewardsService.boundary;

import java.util.Scanner;

import LoyaltyAndRewardsService.control.RewardControl;
import LoyaltyAndRewardsService.utility.MessageUI;
import LoyaltyAndRewardsService.utility.Verification;
import common.src.InputHelper;

/**
 * Handles actor interaction for reward-maintenance use cases.
 *
 * @author Chee Weng
 */
public class RewardUI {
    public static void rewardOperator(Scanner scanner, RewardControl rewardControl) {
        boolean exit = false;

        while (!exit) {
            System.out.println("\r\n"
                    + ".-----.----------------------.\r\n"
                    + "| No. |       Function       |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 1.  | New Reward           |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 2.  | Remove Reward        |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 3.  | Update Reward Info   |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 4.  | Reward List          |\r\n"
                    + "'-----'----------------------'\r\n");

            int userEntry = InputHelper.inputInt(scanner, "Please enter a number (0 to exit): ");
            scanner.nextLine();

            switch (userEntry) {
                case 1:
                    addReward(scanner, rewardControl);
                    break;
                case 2:
                    removeReward(scanner, rewardControl);
                    break;
                case 3:
                    updateReward(scanner, rewardControl);
                    break;
                case 4:
                    displayRewardTable(rewardControl);
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

    private static void addReward(Scanner scanner, RewardControl rewardControl) {
        String rewardName = InputHelper.inputString(scanner, "Enter reward name: ");
        int pointRequired = InputHelper.inputInt(scanner, "Enter points required to redeem: ");
        if (!Verification.verifyRewardName(rewardName)
                || !Verification.verifyRewardPoints(pointRequired)) {
            return;
        }

        String rewardId = rewardControl.createReward(rewardName, pointRequired);
        MessageUI.displaySuccess("Reward " + rewardId + " added successfully.");
    }

    private static void removeReward(Scanner scanner, RewardControl rewardControl) {
        if (rewardControl.isEmpty()) {
            MessageUI.displayInfo("No reward records found.");
            return;
        }

        displayRewardTable(rewardControl);
        String rewardId = InputHelper.inputString(scanner, "Enter reward ID to remove: ");
        if (rewardControl.removeReward(rewardId)) {
            MessageUI.displaySuccess("Reward deleted successfully.");
        } else {
            MessageUI.displayError("Reward not found.");
        }
    }

    private static void updateReward(Scanner scanner, RewardControl rewardControl) {
        if (rewardControl.isEmpty()) {
            MessageUI.displayInfo("No reward records found.");
            return;
        }

        displayRewardTable(rewardControl);
        String rewardId = InputHelper.inputString(scanner, "Enter reward ID to update: ");
        if (!rewardControl.findReward(rewardId)) {
            MessageUI.displayError("Reward not found.");
            return;
        }

        String rewardName = InputHelper.inputString(scanner, "Enter new reward name: ");
        int pointRequired = InputHelper.inputInt(scanner, "Enter new points required: ");
        if (!Verification.verifyRewardName(rewardName)
                || !Verification.verifyRewardPoints(pointRequired)) {
            return;
        }

        rewardControl.updateReward(rewardId, rewardName, pointRequired);
        MessageUI.displaySuccess("Reward updated successfully.");
    }

    private static void displayRewardTable(RewardControl rewardControl) {
        String table = rewardControl.getRewardTable();
        if (table.isEmpty()) {
            MessageUI.displayInfo("No reward records found.");
        } else {
            System.out.println(table);
        }
    }
}
