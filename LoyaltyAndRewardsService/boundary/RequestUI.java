package LoyaltyAndRewardsService.boundary;

import java.util.Scanner;

import LoyaltyAndRewardsService.control.RequestControl;
import LoyaltyAndRewardsService.control.RequestControl.RequestOperationResult;
import LoyaltyAndRewardsService.control.RewardControl;
import LoyaltyAndRewardsService.utility.MessageUI;
import common.src.InputHelper;

/**
 * Handles actor interaction for redemption-request use cases.
 *
 * @author Chee Weng
 */
public class RequestUI {
    public static void requestOperator(Scanner scanner, RequestControl requestControl,
            RewardControl rewardControl) {
        boolean exit = false;

        while (!exit) {
            System.out.println("\r\n"
                    + ".-----.---------------------------.\r\n"
                    + "| No. |         Function          |\r\n"
                    + ":-----+---------------------------:\r\n"
                    + "|  1. | Submit Redemption Request |\r\n"
                    + ":-----+---------------------------:\r\n"
                    + "|  2. | Process Next Request      |\r\n"
                    + ":-----+---------------------------:\r\n"
                    + "|  3. | View Pending Requests     |\r\n"
                    + ":-----+---------------------------:\r\n"
                    + "|  4. | View Request History      |\r\n"
                    + "'-----'---------------------------'\r\n");

            int userEntry = InputHelper.inputInt(scanner, "Please enter a number (0 to exit): ");
            scanner.nextLine();

            switch (userEntry) {
                case 1:
                    submitRequest(scanner, requestControl, rewardControl);
                    break;
                case 2:
                    processRequest(scanner, requestControl);
                    break;
                case 3:
                    displayRequestTable(requestControl.getPendingRequestTable());
                    break;
                case 4:
                    displayRequestTable(requestControl.getRequestHistoryTable());
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

    private static void submitRequest(Scanner scanner, RequestControl requestControl,
            RewardControl rewardControl) {
        if (rewardControl.isEmpty()) {
            MessageUI.displayInfo("No reward records found. Please create a reward first.");
            return;
        }

        System.out.println(rewardControl.getRewardTable());
        String rewardId = InputHelper.inputString(scanner, "Enter reward ID: ");
        String memberId = InputHelper.inputString(scanner, "Enter member ID: ");
        RequestOperationResult result =
                requestControl.submitRewardRequest(memberId, rewardId, rewardControl);
        if (result.isSuccessful()) {
            MessageUI.displaySuccess(result.getMessage());
        } else {
            MessageUI.displayError(result.getMessage());
        }
    }

    private static void processRequest(Scanner scanner, RequestControl requestControl) {
        String nextRequest = requestControl.getNextRequestTable();
        if (nextRequest.isEmpty()) {
            MessageUI.displayInfo("No pending requests.");
            return;
        }
        System.out.println(nextRequest);

        String decision = InputHelper.inputString(scanner, "Approve this request? (Y/N): ");
        if (!decision.equalsIgnoreCase("Y") && !decision.equalsIgnoreCase("N")) {
            MessageUI.displayError("Please enter Y or N.");
            return;
        }

        RequestOperationResult result =
                requestControl.processNextRequestAndSave(decision.equalsIgnoreCase("Y"));
        if (!result.isSuccessful()) {
            MessageUI.displayError(result.getMessage());
        } else if (result.isApproved()) {
            MessageUI.displaySuccess(result.getMessage());
        } else {
            MessageUI.displayInfo(result.getMessage());
        }

        if (result.hasTierChange()) {
            MessageUI.displayInfo(result.getTierChangeMessage());
        }
    }

    private static void displayRequestTable(String table) {
        if (table.isEmpty()) {
            MessageUI.displayInfo("No request records found.");
        } else {
            System.out.println(table);
        }
    }
}
