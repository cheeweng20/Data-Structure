package LoyaltyAndRewardsService.boundary;

import java.util.Scanner;

import LoyaltyAndRewardsService.control.MemberControl;
import LoyaltyAndRewardsService.control.MemberControl.PointUpdateResult;
import LoyaltyAndRewardsService.control.RequestControl;
import LoyaltyAndRewardsService.control.RewardControl;
import LoyaltyAndRewardsService.control.TransactionControl;
import LoyaltyAndRewardsService.utility.MessageUI;
import LoyaltyAndRewardsService.utility.Verification;
import common.src.InputHelper;

/**
 * Handles actor interaction for member-related use cases.
 *
 * @author Chee Weng
 */
public class MemberUI {
    public static void memberOperator(Scanner scanner, MemberControl memberControl,
            TransactionControl transactionControl, RequestControl requestControl,
            RewardControl rewardControl) {
        boolean exit = false;

        while (!exit) {
            System.out.println("\r\n"
                    + ".-----.----------------------.\r\n"
                    + "| No. |       Function       |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 1.  | New Member           |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 2.  | Remove Member        |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 3.  | Update Member Info   |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 4.  | Add Point for Member |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 5.  | Point Redemption     |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 6.  | Member List          |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 7.  | Member Promotion     |\r\n"
                    + "'-----'----------------------'\r\n");

            int userEntry = InputHelper.inputInt(scanner, "Please enter a number (0 to exit): ");
            switch (userEntry) {
                case 1:
                    addMember(scanner, memberControl);
                    break;
                case 2:
                    removeMember(scanner, memberControl);
                    break;
                case 3:
                    updateMember(scanner, memberControl);
                    break;
                case 4:
                    addMemberPoints(scanner, memberControl, transactionControl);
                    break;
                case 5:
                    if (memberControl.isEmpty()) {
                        MessageUI.displayInfo("No member records found.");
                        break;
                    }
                    scanner.nextLine();
                    RequestUI.requestOperator(scanner, requestControl, rewardControl);
                    break;
                case 6:
                    displayMemberTable(memberControl);
                    break;
                case 7:
                    displayPromotion(scanner, memberControl);
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

    private static void addMember(Scanner scanner, MemberControl memberControl) {
        scanner.nextLine();
        String name = InputHelper.inputString(scanner, "Enter member name: ");
        int point = InputHelper.inputInt(scanner, "Enter current member points: ");

        if (!Verification.verifyMemberPoint(point)
                || !Verification.verifyMemberName(name, memberControl)) {
            return;
        }

        String memberId = memberControl.createMember(name, point);
        MessageUI.displaySuccess("Member " + memberId + " added successfully.");
    }

    private static void removeMember(Scanner scanner, MemberControl memberControl) {
        if (memberControl.isEmpty()) {
            MessageUI.displayInfo("No member records found.");
            return;
        }

        scanner.nextLine();
        displayMemberTable(memberControl);
        String memberId = InputHelper.inputString(scanner, "Enter member ID: ");
        if (memberControl.removeMember(memberId)) {
            MessageUI.displaySuccess("Member deleted successfully.");
        } else {
            MessageUI.displayError("Member not found.");
        }
    }

    private static void updateMember(Scanner scanner, MemberControl memberControl) {
        if (memberControl.isEmpty()) {
            MessageUI.displayInfo("No member records found.");
            return;
        }

        scanner.nextLine();
        displayMemberTable(memberControl);
        String memberId = InputHelper.inputString(scanner, "Enter member ID to update: ");
        if (!memberControl.findMember(memberId)) {
            MessageUI.displayError("Member not found.");
            return;
        }

        String newName = InputHelper.inputString(scanner, "Enter new member name: ");
        int newPoint = InputHelper.inputInt(scanner, "Enter new member points: ");
        if (!Verification.verifyMemberPoint(newPoint)
                || !Verification.verifyMemberName(newName, memberId, memberControl)) {
            return;
        }

        memberControl.updateMember(memberId, newName, newPoint);
        MessageUI.displaySuccess("Member updated successfully.");
    }

    private static void addMemberPoints(Scanner scanner, MemberControl memberControl,
            TransactionControl transactionControl) {
        if (memberControl.isEmpty()) {
            MessageUI.displayInfo("No member records found.");
            return;
        }

        scanner.nextLine();
        String memberId = InputHelper.inputString(scanner, "Enter member ID: ");
        int addedPoint = InputHelper.inputInt(scanner, "Enter points to add: ");
        if (addedPoint <= 0) {
            MessageUI.displayError("Points to add must be greater than zero.");
            return;
        }

        PointUpdateResult result =
                memberControl.addPoints(memberId, addedPoint, transactionControl);
        if (!result.isSuccessful()) {
            MessageUI.displayError("Member not found.");
            return;
        }

        MessageUI.displaySuccess(addedPoint + " points added successfully.");
        MessageUI.displayInfo("Current points: " + result.getCurrentPoint());
        if (result.isTierChanged()) {
            MessageUI.displayInfo(result.getTierChangeMessage());
        }
    }

    private static void displayPromotion(Scanner scanner, MemberControl memberControl) {
        scanner.nextLine();
        String memberId = InputHelper.inputString(scanner, "Enter member ID: ");
        if (!memberControl.findMember(memberId)) {
            MessageUI.displayError("Member not found.");
            return;
        }
        MessageUI.displayInfo(memberControl.generatePersonalizedPromotion(memberId));
    }

    private static void displayMemberTable(MemberControl memberControl) {
        String table = memberControl.getMemberTable();
        if (table.isEmpty()) {
            MessageUI.displayInfo("No member records found.");
        } else {
            System.out.println(table);
        }
    }
}
