package LoyaltyAndRewardsService.boundary;

import java.util.Scanner;

import LoyaltyAndRewardsService.control.MemberControl;
import LoyaltyAndRewardsService.control.RequestControl;
import LoyaltyAndRewardsService.control.TierControl;
import LoyaltyAndRewardsService.control.TransactionControl;
import LoyaltyAndRewardsService.entity.Member;
import common.src.InputHelper;
import common.src.Logo;

public class MemberUI {
    public static void memberOperator(Scanner scanner, MemberControl memberLinkedList,
            TierControl tierLinkedList, TransactionControl transactionList, RequestControl requestControl) {
        boolean exit = false;

        while (!exit) {
            Logo.displayLoyaltyAndRewardsService();

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

                    String newMemberId = memberLinkedList.generateMemberId();
                    scanner.nextLine();

                    String tierId = tierLinkedList.getTierIdByPoint(point);
                    Member member = new Member(newMemberId, name, point, tierId);
                    memberLinkedList.addMember(member);
                    System.out.println("Member Added Successful");

                    break;
                }

                case 2: {
                    if (memberLinkedList.size() < 1) {
                        System.out.println("Not Member Record");
                        break;
                    }

                    scanner.nextLine();
                    memberLinkedList.displayAllMember();

                    String memberId = InputHelper.inputString(scanner, "Enter Member ID:");

                    if (memberLinkedList.findMember(memberId)) {
                        memberLinkedList.deleteMemberById(memberId);
                        System.out.println("Delete member successfully");
                    } else {
                        System.out.println("Member Not Found");
                    }
                    break;
                }

                case 3: {
                    scanner.nextLine();
                    if (memberLinkedList.size() < 1) {
                        System.out.println("Not Member Record");
                        break;
                    }

                    memberLinkedList.displayAllMember();

                    String memberId = InputHelper.inputString(scanner, "Enter Member ID to Update:");

                    if (memberLinkedList.findMember(memberId)) {

                        String newName = InputHelper.inputString(scanner, "Enter Member New Name:");

                        int newPoint = InputHelper.inputInt(scanner, "Enter Member New Point:");

                        memberLinkedList.updateMemberById(memberId, newName, newPoint);
                        System.out.println("Member Updated Successful");

                    } else {
                        System.out.print("Member Not Found");
                    }

                    break;
                }

                case 4: {
                    if (memberLinkedList.size() < 1) {
                        System.out.println("Not Member Record");
                        break;
                    }
                    scanner.nextLine();

                    String memberId = InputHelper.inputString(scanner, "Enter Member ID:");

                    if (memberLinkedList.findMember(memberId)) {
                        int addPoint = InputHelper.inputInt(scanner, "Please Enter a Added Point: ");

                        int newPoint = memberLinkedList.addMemberPoint(memberId, addPoint);
                        transactionList.addTransaction(memberId, newPoint);

                        System.out.println("New point " + Integer.toString(addPoint) + " added successful");
                        System.out.println("Current point : " + Integer.toString(newPoint));
                    }

                    System.out.println("Member Not Found");
                    break;
                }

                case 5: {
                    if (memberLinkedList.size() < 1) {
                        System.out.println("Not Member Record");
                        break;
                    }
                    scanner.nextLine();

                    RequestUI.requestOperator(scanner, requestControl, memberLinkedList);

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
                        System.out.println(promotion);
                    } else {
                        System.out.println("Member Not Found");
                    }
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
