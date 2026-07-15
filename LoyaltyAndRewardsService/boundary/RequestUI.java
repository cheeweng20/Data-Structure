package LoyaltyAndRewardsService.boundary;

import java.util.Scanner;

import LoyaltyAndRewardsService.control.MemberControl;
import LoyaltyAndRewardsService.control.RequestControl;
import LoyaltyAndRewardsService.control.RewardControl;
import LoyaltyAndRewardsService.dao.RequestDao;
import LoyaltyAndRewardsService.dao.MemberDao;
import LoyaltyAndRewardsService.entity.RedemptionRequest;
import LoyaltyAndRewardsService.entity.Reward;
import common.src.*;


public class RequestUI {

    public static void requestOperator(Scanner scanner, RequestControl requestControl, MemberControl memberControl,
            RewardControl rewardControl) {
        boolean exit = false;

        while (!exit) {
            Logo.displayLoyaltyAndRewardsService();
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
                        System.out.println("No reward record found. Please create a reward first.");
                        break;
                    }

                    rewardControl.displayAllRewards();
                    String rewardId = InputHelper.inputString(scanner, "Enter Reward ID: ");
                    Reward reward = rewardControl.getRewardById(rewardId);
                    if (reward == null) {
                        System.out.println("Reward Not Found");
                        break;
                    }

                    String memberId = InputHelper.inputString(scanner, "Enter Member ID: ");

                    if (!memberControl.findMember(memberId)) {
                        System.out.println("Member Not Found");
                        break;
                    }

                    boolean success = requestControl.submitRequest(memberId, reward.getPointRequired());
                    if (success) {
                        RequestDao.saveToRequestFile(requestControl);
                        System.out.println("Request for " + reward.getRewardName() + " submitted, waiting to be processed.");
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

                    String decision = InputHelper.inputString(scanner, "Approve this request? (Y/N): ");
                    boolean approve = decision.equalsIgnoreCase("Y");

                    RedemptionRequest processed = requestControl.processNextRequest(approve);
                    RequestDao.saveToRequestFile(requestControl);
                    if (approve) {
                        MemberDao.saveToMemberFile(memberControl);
                    }
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

}
