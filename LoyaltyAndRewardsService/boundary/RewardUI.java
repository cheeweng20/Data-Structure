package LoyaltyAndRewardsService.boundary;

import java.util.Scanner;

import LoyaltyAndRewardsService.control.RewardControl;
import LoyaltyAndRewardsService.dao.RewardDao;
import LoyaltyAndRewardsService.entity.Reward;
import LoyaltyAndRewardsService.utility.MessageUI;
import LoyaltyAndRewardsService.utility.Verification;
import common.src.InputHelper;

/**
 * Console interface for reward CRUD operations.
 *
 * @author Chee Weng
 */
public class RewardUI {
    public static void rewardOperator(Scanner scanner, RewardControl rewardList) {
        boolean exit = false;

        while (!exit) {
            System.out.println("\r\n" +
                    ".-----.----------------------.\r\n" +
                    "| No. |       Function       |\r\n" +
                    ":-----+----------------------:\r\n" +
                    "| 1.  | New Reward           |\r\n" +
                    ":-----+----------------------:\r\n" +
                    "| 2.  | Remove Reward        |\r\n" +
                    ":-----+----------------------:\r\n" +
                    "| 3.  | Update Reward Info   |\r\n" +
                    ":-----+----------------------:\r\n" +
                    "| 4.  | Reward List          |\r\n" +
                    "'-----'----------------------'\r\n");

            int userEntry = InputHelper.inputInt(scanner, "Please enter a number (0 to exit): ");
            scanner.nextLine();

            switch (userEntry) {
                case 1:
                    addReward(scanner, rewardList);
                    break;
                case 2:
                    removeReward(scanner, rewardList);
                    break;
                case 3:
                    updateReward(scanner, rewardList);
                    break;
                case 4:
                    rewardList.displayAllRewards();
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

    private static void addReward(Scanner scanner, RewardControl rewardList) {
        String rewardName = InputHelper.inputString(scanner, "Enter reward name: ");
        int pointRequired = InputHelper.inputInt(scanner, "Enter points required to redeem: ");

        if (!Verification.verifyRewardName(rewardName) || !Verification.verifyRewardPoints(pointRequired)) {
            return;
        }
        String rewardId = rewardList.generateRewardId();
        rewardList.addReward(new Reward(rewardId, rewardName, pointRequired));
        RewardDao.saveToRewardFile(rewardList);
        MessageUI.displaySuccess("Reward " + rewardId + " added successfully.");
    }

    private static void removeReward(Scanner scanner, RewardControl rewardList) {
        if (rewardList.isEmpty()) {
            MessageUI.displayInfo("No reward records found.");
            return;
        }

        rewardList.displayAllRewards();
        String rewardId = InputHelper.inputString(scanner, "Enter reward ID to remove: ");

        if (rewardList.deleteRewardById(rewardId)) {
            RewardDao.saveToRewardFile(rewardList);
            MessageUI.displaySuccess("Reward deleted successfully.");
        } else {
            MessageUI.displayError("Reward not found.");
        }
    }

    private static void updateReward(Scanner scanner, RewardControl rewardList) {
        if (rewardList.isEmpty()) {
            MessageUI.displayInfo("No reward records found.");
            return;
        }

        rewardList.displayAllRewards();
        String rewardId = InputHelper.inputString(scanner, "Enter reward ID to update: ");
        if (!rewardList.findReward(rewardId)) {
            MessageUI.displayError("Reward not found.");
            return;
        }

        String rewardName = InputHelper.inputString(scanner, "Enter new reward name: ");
        int pointRequired = InputHelper.inputInt(scanner, "Enter new points required to redeem: ");
        if (!Verification.verifyRewardName(rewardName) || !Verification.verifyRewardPoints(pointRequired)) {
            return;
        }
        rewardList.updateRewardById(rewardId, rewardName, pointRequired);
        RewardDao.saveToRewardFile(rewardList);
        MessageUI.displaySuccess("Reward updated successfully.");
    }

}
