package LoyaltyAndRewardsService.boundary;

import java.util.Scanner;

import LoyaltyAndRewardsService.control.MemberControl;
import LoyaltyAndRewardsService.control.TierControl;
import LoyaltyAndRewardsService.entity.Member;

public class MemberUI {
    public static void memberOperator(Scanner scanner, MemberControl memberLinkedList, TierControl tierLinkedList) {
        boolean exit = false;

        while (!exit) {
            System.out.println("1. New Member");
            System.out.println("2. Remove Member");
            System.out.println("3. Update Member Info");
            System.out.println("4. Add Point for Member");
            System.out.println("5. Point Redeem");
            System.out.println("6. Member List");
            System.out.println("0. Return Main Menu");
            System.out.print("Please Enter A number:");

            int userEntry = scanner.nextInt();
            switch (userEntry) {
                case 1: {
                    scanner.nextLine();

                    String name = promptText(scanner, "Enter User Name: ");

                    int point = promptInt(scanner, "Enter Current Member Point: ");

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

                    System.out.print("Enter Member ID:");
                    String memberId = scanner.nextLine();

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

                    System.out.print("Enter Member ID to Update:");
                    String memberId = scanner.nextLine();

                    if (memberLinkedList.findMember(memberId)) {
                        System.out.print("Enter Member New Name:");
                        String newName = scanner.nextLine();
                        System.out.print("Enter Member New Point:");
                        int newPoint = scanner.nextInt();

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

                    String memberId = promptText(scanner, "Enter a Member ID:");

                    if (memberLinkedList.findMember(memberId)) {
                        int addPoint = promptInt(scanner, "Please Enter a Added Point: ");

                        int newPoint = memberLinkedList.addMemberPoint(memberId, addPoint);

                        System.out.println("New point " + Integer.toString(addPoint) + " added successful");
                        System.out.println("Current point : " + Integer.toString(newPoint));
                    }
                    break;
                }

                case 5: {
                    if (memberLinkedList.size() < 1) {
                        System.out.println("Not Member Record");
                        break;
                    }
                    scanner.nextLine();

                    String memberId = promptText(scanner, "Enter a Member ID:");

                    if (memberLinkedList.findMember(memberId)) {
                        int redeemPoint = promptInt(scanner, "Please Enter a Redeem Point: ");

                        int newPoint = memberLinkedList.redeemPoint(memberId, redeemPoint);

                        System.out.println(Integer.toString(redeemPoint) + " point redeem successful");
                        System.out.println("Current point : " + Integer.toString(newPoint));
                    }
                    break;
                }

                case 6: {
                    memberLinkedList.displayAllMember();
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

    // Helper Function
    private static String promptText(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private static int promptInt(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextInt();
    }
}
