package LoyaltyAndRewardsService.boundary;

import java.util.Scanner;

import LoyaltyAndRewardsService.control.TierControl;
import LoyaltyAndRewardsService.entity.Tier;
import common.src.*;

public class TierUI {
    public static void tierOperator(Scanner scanner, TierControl tierLinkedList) {
        boolean exit = false;

        while (!exit) {
            Logo.displayLoyaltyAndRewardsService();

            System.out.println("\r\n" + //
                    ".-----.------------------------.\r\n" + //
                    "| No. |        Function        |\r\n" + //
                    ":-----+------------------------:\r\n" + //
                    "|  1. | New Tier Level         |\r\n" + //
                    ":-----+------------------------:\r\n" + //
                    "|  2. | Remove Tier Level      |\r\n" + //
                    ":-----+------------------------:\r\n" + //
                    "|  3. | Update Tier Level Info |\r\n" + //
                    ":-----+------------------------:\r\n" + //
                    "|  4. | Tier List              |\r\n" + //
                    "'-----'------------------------'\r\n" + //
                    "\r\n" + //
                    "");

            int userEntry = InputHelper.inputInt(scanner, "Please Enter A number(0 to exit):");

            switch (userEntry) {
                case 1: {
                    scanner.nextLine();
                    String tierLevel = InputHelper.inputString(scanner, "Tier Level Name: ");
                    int minPoint = InputHelper.inputInt(scanner, "Min Point: ");
                    int maxPoint = InputHelper.inputInt(scanner, "Max Point(Enter 0 for highest level): ");

                    String tierId = tierLinkedList.generateTierId();
                    Tier tier = new Tier(tierId, tierLevel, minPoint, maxPoint);
                    tierLinkedList.addTierLevel(tier);
                    System.out.println("New Tier Level Added Successful");
                    break;
                }
                case 2: {
                    scanner.nextLine();
                    if (tierLinkedList.size() < 1) {
                        System.out.println("Not Tier Record");
                        break;
                    }

                    tierLinkedList.displayAllTierLevel();

                    String tierId = InputHelper.inputString(scanner, "Enter Tier ID:");

                    if (tierLinkedList.findTier(tierId)) {
                        tierLinkedList.removeTierLevel(tierId);
                        System.out.println("Delete Tier Level successfully");
                    } else {
                        System.out.println("Tier Level Not Found");
                    }
                    break;
                }
                case 3: {
                    scanner.nextLine();
                    if (tierLinkedList.size() < 1) {
                        System.out.println("Not Tier Record");
                        break;
                    }

                    tierLinkedList.displayAllTierLevel();

                    String tierId = InputHelper.inputString(scanner, "Enter Tier ID to Update:");

                    if (tierLinkedList.findTier(tierId)) {
                        String newName = InputHelper.inputString(scanner, "Enter New Tier Level Name:");
                        int minPoint = InputHelper.inputInt(scanner, "Enter New Min Point:");
                        int maxPoint = InputHelper.inputInt(scanner, "Enter New Max Point:");

                        tierLinkedList.updateTierLevelById(tierId, newName, minPoint, maxPoint);
                        System.out.println("Update Tier Level Successful");

                    } else {
                        System.out.print("Tier Level Not Found");
                    }

                    break;
                }
                case 4: {
                    tierLinkedList.displayAllTierLevel();
                    break;
                }
                case 0:
                    exit = true;
                    break;
                default:
                    break;
            }
        }
    }
}
