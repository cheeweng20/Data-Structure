package LoyaltyAndRewardsService.boundary;

import java.util.Scanner;

import LoyaltyAndRewardsService.control.MemberControl;
import LoyaltyAndRewardsService.control.RequestControl;
import LoyaltyAndRewardsService.control.RewardControl;
import LoyaltyAndRewardsService.control.TierControl;
import LoyaltyAndRewardsService.dao.RequestDao;
import LoyaltyAndRewardsService.dao.MemberDao;
import LoyaltyAndRewardsService.entity.RedemptionRequest;
import LoyaltyAndRewardsService.entity.Reward;
import LoyaltyAndRewardsService.utility.MessageUI;
import common.src.*;


/**
 * @author Chee Weng
 */
public class RequestUI {

    public static void requestOperator(Scanner scanner, RequestControl requestControl, MemberControl memberControl,
            RewardControl rewardControl, TierControl tierControl) {
        boolean exit = false;

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

            int userEntry = InputHelper.inputInt(scanner, "Please Enter A number(0 to exit):");
            scanner.nextLine();

            switch (userEntry) {
                case 1: {
                    if (rewardControl.isEmpty()) {
                        MessageUI.displayInfo("No reward records found. Please create a reward first.");
                        break;
                    }

                    rewardControl.displayAllRewards();
                    String rewardId = InputHelper.inputString(scanner, "Enter Reward ID: ");
                    Reward reward = rewardControl.getRewardById(rewardId);
                    if (reward == null) {
                        MessageUI.displayError("Reward not found.");
                        break;
                    }

                    String memberId = InputHelper.inputString(scanner, "Enter Member ID: ");

                    if (!memberControl.findMember(memberId)) {
                        MessageUI.displayError("Member not found.");
                        break;
                    }

                    boolean success = requestControl.submitRequest(memberId, reward.getPointRequired());
                    if (success) {
                        RequestDao.saveToRequestFile(requestControl);
                        MessageUI.displaySuccess("Request for " + reward.getRewardName()
                                + " submitted and is waiting to be processed.");
                    } else {
                        MessageUI.displayError("Insufficient available points; request not accepted.");
                    }
                    break;
                }

                case 2: {
                    if (requestControl.isEmpty()) {
                        MessageUI.displayInfo("No pending requests.");
                        break;
                    }

                    RedemptionRequest next = requestControl.peekNextRequest();
                    displayPendingRequest(next);

                    String decision = InputHelper.inputString(scanner, "Approve this request? (Y/N): ");
                    boolean approve = decision.equalsIgnoreCase("Y");
                    String previousTierId = memberControl.getMemberById(next.getMemberId()).getTierId();

                    RedemptionRequest processed = requestControl.processNextRequest(approve);
                    RequestDao.saveToRequestFile(requestControl);
                    if (approve) {
                        MemberDao.saveToMemberFile(memberControl);
                    }
                    if ("Approved".equalsIgnoreCase(processed.getStatus())) {
                        MessageUI.displaySuccess("Request approved.");
                    } else {
                        MessageUI.displayInfo("Request " + processed.getStatus() + ".");
                    }
                    displayTierChange(memberControl, tierControl, processed, previousTierId);
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

    private static void displayTierChange(MemberControl memberControl, TierControl tierControl,
            RedemptionRequest request, String previousTierId) {
        if (!"Approved".equalsIgnoreCase(request.getStatus())) {
            return;
        }

        String newTierId = memberControl.getMemberById(request.getMemberId()).getTierId();
        if (previousTierId != null && !previousTierId.equalsIgnoreCase(newTierId)) {
            MessageUI.displayInfo("Tier changed: " + tierControl.getTierNameById(previousTierId)
                    + " -> " + tierControl.getTierNameById(newTierId));
        }
    }

    private static void displayPendingRequest(RedemptionRequest request) {
        String border = "+------------+------------+------------------+------------+";
        System.out.println(border);
        System.out.printf("| %-10s | %-10s | %16s | %-10s |%n", "Request ID", "Member ID", "Points Requested", "Status");
        System.out.println(border);
        System.out.printf("| %-10.10s | %-10.10s | %16d | %-10.10s |%n", request.getRequestId(), request.getMemberId(),
                request.getPointsRequested(), request.getStatus());
        System.out.println(border);
    }

}
