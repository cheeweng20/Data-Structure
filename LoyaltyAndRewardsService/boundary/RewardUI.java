package LoyaltyAndRewardsService.boundary;

import java.util.Scanner;

import LoyaltyAndRewardsService.control.RewardControl;
import LoyaltyAndRewardsService.dao.RewardDao;
import LoyaltyAndRewardsService.entity.Reward;
import common.src.InputHelper;
import common.src.Logo;

/**
 * Console interface for reward CRUD operations.
 */
public class RewardUI {
    public static void rewardOperator(Scanner scanner, RewardControl rewardList) {
        boolean exit = false;

        while (!exit) {
            Logo.displayLoyaltyAndRewardsService();
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
                    System.out.println("Invalid option.");
                    break;
            }
        }
    }

    private static void addReward(Scanner scanner, RewardControl rewardList) {
        String rewardName = InputHelper.inputString(scanner, "Enter reward name: ");
        int pointRequired = inputNonNegativePoint(scanner, "Enter points required to redeem: ");

        String rewardId = rewardList.generateRewardId();
        rewardList.addReward(new Reward(rewardId, rewardName, pointRequired));
        RewardDao.saveToRewardFile(rewardList);
        System.out.println("Reward " + rewardId + " added successfully.");
    }

    private static void removeReward(Scanner scanner, RewardControl rewardList) {
        if (rewardList.isEmpty()) {
            System.out.println("No reward record found.");
            return;
        }

        rewardList.displayAllRewards();
        String rewardId = InputHelper.inputString(scanner, "Enter reward ID to remove: ");

        if (rewardList.deleteRewardById(rewardId)) {
            RewardDao.saveToRewardFile(rewardList);
            System.out.println("Reward deleted successfully.");
        } else {
            System.out.println("Reward not found.");
        }
    }

    private static void updateReward(Scanner scanner, RewardControl rewardList) {
        if (rewardList.isEmpty()) {
            System.out.println("No reward record found.");
            return;
        }

        rewardList.displayAllRewards();
        String rewardId = InputHelper.inputString(scanner, "Enter reward ID to update: ");
        if (!rewardList.findReward(rewardId)) {
            System.out.println("Reward not found.");
            return;
        }

        String rewardName = InputHelper.inputString(scanner, "Enter new reward name: ");
        int pointRequired = inputNonNegativePoint(scanner, "Enter new points required to redeem: ");
        rewardList.updateRewardById(rewardId, rewardName, pointRequired);
        RewardDao.saveToRewardFile(rewardList);
        System.out.println("Reward updated successfully.");
    }

    private static int inputNonNegativePoint(Scanner scanner, String prompt) {
        int pointRequired;
        do {
            pointRequired = InputHelper.inputInt(scanner, prompt);
            if (pointRequired < 0) {
                System.out.println("Points required cannot be negative.");
            }
        } while (pointRequired < 0);
        scanner.nextLine();
        return pointRequired;
    }
}
