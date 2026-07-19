package LoyaltyAndRewardsService.boundary;

import java.util.Scanner;

import LoyaltyAndRewardsService.control.MemberControl;
import LoyaltyAndRewardsService.control.RequestControl;
import LoyaltyAndRewardsService.control.RewardControl;
import LoyaltyAndRewardsService.control.TierControl;
import LoyaltyAndRewardsService.control.TransactionControl;
import LoyaltyAndRewardsService.dao.MemberDao;
import LoyaltyAndRewardsService.dao.PointTransactionDao;
import LoyaltyAndRewardsService.entity.Member;
import LoyaltyAndRewardsService.utility.MessageUI;
import LoyaltyAndRewardsService.utility.Verification;
import common.src.InputHelper;

/**
 * @author Chee Weng
 */
public class MemberUI {
    public static void memberOperator(Scanner scanner, MemberControl memberLinkedList,
            TierControl tierLinkedList, TransactionControl transactionList, RequestControl requestControl,
            RewardControl rewardControl) {
        boolean exit = false;

        while (!exit) {

            System.out.println("\r\n" + //
                    ".-----.----------------------.\r\n" + //
                    "| No. |       Function       |\r\n" + //
                    ":-----+----------------------:\r\n" + //
                    "| 1.  | New Member           |\r\n" + //
                    ":-----+----------------------:\r\n" + //
                    "| 2.  | Remove Member        |\r\n" + //
                    ":-----+----------------------:\r\n" + //
                    "| 3.  | Update Member Info   |\r\n" + //
                    ":-----+----------------------:\r\n" + //
                    "| 4.  | Add Point for Member |\r\n" + //
                    ":-----+----------------------:\r\n" + //
                    "| 5.  | Point Redemption     |\r\n" + //
                    ":-----+----------------------:\r\n" + //
                    "| 6.  | Member List          |\r\n" + //
                    ":-----+----------------------:\r\n" + //
                    "| 7.  | Member Promotion     |\r\n" + //
                    "'-----'----------------------'\r\n" + //
                    "\r\n" + //
                    "");

            int userEntry = InputHelper.inputInt(scanner, "Please Enter A number(0 to exit): ");
            switch (userEntry) {
                case 1: {
                    scanner.nextLine();

                    String name = InputHelper.inputString(scanner, "Enter User Name: ");

                    int point = InputHelper.inputInt(scanner, "Enter Current Member Point: ");

                    if (!Verification.verifyMemberPoint(point) || !Verification.verifyMemberName(name, memberLinkedList)) {
                        break;
                    }

                    String newMemberId = memberLinkedList.generateMemberId();
                    scanner.nextLine();

                    String tierId = tierLinkedList.getTierIdByPoint(point);
                    Member member = new Member(newMemberId, name, point, tierId);
                    memberLinkedList.addMember(member);
                    MemberDao.saveToMemberFile(memberLinkedList);
                    MessageUI.displaySuccess("Member added successfully.");

                    break;
                }

                case 2: {
                    if (memberLinkedList.size() < 1) {
                        MessageUI.displayInfo("No member records found.");
                        break;
                    }

                    scanner.nextLine();
                    memberLinkedList.displayAllMember();

                    String memberId = InputHelper.inputString(scanner, "Enter Member ID:");

                    if (memberLinkedList.findMember(memberId)) {
                        memberLinkedList.deleteMemberById(memberId);
                        MemberDao.saveToMemberFile(memberLinkedList);
                        MessageUI.displaySuccess("Member deleted successfully.");
                    } else {
                        MessageUI.displayError("Member not found.");
                    }
                    break;
                }

                case 3: {
                    scanner.nextLine();
                    if (memberLinkedList.size() < 1) {
                        MessageUI.displayInfo("No member records found.");
                        break;
                    }

                    memberLinkedList.displayAllMember();

                    String memberId = InputHelper.inputString(scanner, "Enter Member ID to Update:");

                    if (memberLinkedList.findMember(memberId)) {

                        String newName = InputHelper.inputString(scanner, "Enter Member New Name:");

                        int newPoint = InputHelper.inputInt(scanner, "Enter Member New Point:");

                        if (!Verification.verifyMemberPoint(newPoint) || !Verification.verifyMemberName(newName, memberLinkedList)) {
                            break;
                        }

                        memberLinkedList.updateMemberById(memberId, newName, newPoint);
                        MemberDao.saveToMemberFile(memberLinkedList);
                        MessageUI.displaySuccess("Member updated successfully.");

                    } else {
                        MessageUI.displayError("Member not found.");
                    }

                    break;
                }

                case 4: {
                    if (memberLinkedList.size() < 1) {
                        MessageUI.displayInfo("No member records found.");
                        break;
                    }
                    scanner.nextLine();

                    String memberId = InputHelper.inputString(scanner, "Enter Member ID:");

                    if (memberLinkedList.findMember(memberId)) {
                        int addPoint = InputHelper.inputInt(scanner, "Please Enter a Added Point: ");
                        if (addPoint <= 0) {
                            MessageUI.displayError("Points to add must be greater than zero.");
                            break;
                        }

                        String previousTierId = memberLinkedList.getMemberById(memberId).getTierId();
                        int newPoint = memberLinkedList.addMemberPoint(memberId, addPoint);
                        transactionList.addTransaction(memberId, addPoint);
                        MemberDao.saveToMemberFile(memberLinkedList);
                        PointTransactionDao.saveToTransactionFile(transactionList);

                        MessageUI.displaySuccess(addPoint + " points added successfully.");
                        MessageUI.displayInfo("Current points: " + newPoint);
                        displayTierChange(memberLinkedList, tierLinkedList, memberId, previousTierId);
                    } else {
                        MessageUI.displayError("Member not found.");
                    }
                    break;
                }

                case 5: {
                    if (memberLinkedList.size() < 1) {
                        MessageUI.displayInfo("No member records found.");
                        break;
                    }
                    scanner.nextLine();

                    RequestUI.requestOperator(scanner, requestControl, memberLinkedList, rewardControl,
                            tierLinkedList);

                    break;
                }

                case 6: {
                    memberLinkedList.displayAllMember();
                    break;
                }

                case 7: {
                    scanner.nextLine();
                    String memberId = InputHelper.inputString(scanner, "Enter Member ID:");

                    if (memberLinkedList.findMember(memberId)) {
                        String promotion = memberLinkedList.generatePersonalizedPromotion(memberId);
                        MessageUI.displayInfo(promotion);
                    } else {
                        MessageUI.displayError("Member not found.");
                    }
                    break;
                }

                case 0:
                    exit = true;
                    break;
                default:
                    MessageUI.displayError("Invalid option.");
                    break;
            }
        }
    }

    private static void displayTierChange(MemberControl memberControl, TierControl tierControl, String memberId,
            String previousTierId) {
        String newTierId = memberControl.getMemberById(memberId).getTierId();
        if (previousTierId != null && !previousTierId.equalsIgnoreCase(newTierId)) {
            MessageUI.displayInfo("Tier changed: " + tierControl.getTierNameById(previousTierId)
                    + " -> " + tierControl.getTierNameById(newTierId));
        }
    }
}
