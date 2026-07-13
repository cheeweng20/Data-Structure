package LoyaltyAndRewardsService.boundary;

import java.util.Scanner;

import LoyaltyAndRewardsService.control.MemberControl;
import LoyaltyAndRewardsService.control.RequestControl;
import LoyaltyAndRewardsService.dao.RequestDao;
import LoyaltyAndRewardsService.entity.RedemptionRequest;

public class RequestUI {

    public static void requestOperator(Scanner scanner, RequestControl requestControl, MemberControl memberControl) {
        boolean exit = false;
        System.out.print("\033[H\033[2J");
        System.out.flush();

        while (!exit) {
            System.out.println("\r\n" + //
                    ".-----.---------------------------.\r\n" + //
                    "| No. |         Function          |\r\n" + //
                    ":-----+---------------------------:\r\n" + //
                    "|  1. | Submit Redemption Request |\r\n" + //
                    ":-----+---------------------------:\r\n" + //
                    "|  2. | Process Next Request      |\r\n" + //
                    "'-----'---------------------------'\r\n" + //
                    "\r\n" + //
                    "");

            int userEntry = promptInt(scanner, "Please Enter A number(0 to exit):");
            scanner.nextLine();

            switch (userEntry) {
                case 1: {
                    String memberId = promptText(scanner, "Enter Member ID: ");

                    if (!memberControl.findMember(memberId)) {
                        System.out.println("Member Not Found");
                        break;
                    }

                    int points = promptInt(scanner, "Enter Points to Redeem: ");
                    scanner.nextLine();

                    boolean success = requestControl.submitRequest(memberId, points);
                    if (success) {
                        RequestDao.saveToRequestFile(requestControl);
                        System.out.println("Request submitted, waiting to be processed.");
                    } else {
                        System.out.println("Insufficient points, request not accepted.");
                    }
                    break;
                }

                case 2: {
                    if (requestControl.isEmpty()) {
                        System.out.println("No pending requests.");
                        break;
                    }

                    RedemptionRequest next = requestControl.peekNextRequest();
                    System.out.println("Next Request -> Member: " + next.getMemberId()
                            + ", Points: " + next.getPointsRequested());

                    String decision = promptText(scanner, "Approve this request? (Y/N): ");
                    boolean approve = decision.equalsIgnoreCase("Y");

                    RedemptionRequest processed = requestControl.processNextRequest(approve);
                    RequestDao.saveToRequestFile(requestControl);
                    System.out.println("Request " + processed.getStatus());
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

    private static String promptText(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private static int promptInt(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextInt();
    }
}
