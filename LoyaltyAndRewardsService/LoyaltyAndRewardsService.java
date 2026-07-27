package LoyaltyAndRewardsService;

import java.util.Scanner;

import LoyaltyAndRewardsService.boundary.*;
import LoyaltyAndRewardsService.control.LoyaltyServiceControl;
import common.src.Logo;

/**
 * @author Chee Weng
 */
public class LoyaltyAndRewardsService {

    public static void displayMenu(Scanner scanner) {

        Logo.displayLoyaltyAndRewardsService();
        System.out.println("\r\n" + //
                ".-----.-------------------.\r\n" + //
                "| No. |     Function      |\r\n" + //
                ":-----+-------------------:\r\n" + //
                "|  1. | Member Management |\r\n" + //
                ":-----+-------------------:\r\n" + //
                "|  2. | Tier Management   |\r\n" + //
                ":-----+-------------------:\r\n" + //
                "|  3. | Rewards Management|\r\n" + //
                ":-----+-------------------:\r\n" + //
                "|  4. | Report            |\r\n" + //
                "'-----'-------------------'\r\n" + //
                "\r\n" + //
                "");
        System.out.print("Enter Number of Function(0 to exit current program): ");
    }

    public static void LoyaltyAndRewardsServiceMain(Scanner input) {
        boolean exit = false;

        LoyaltyServiceControl serviceControl = new LoyaltyServiceControl();

        NotificationUI.displayStartupNotifications(
                serviceControl.getTransactionControl(), serviceControl.getRequestControl());

        while (!exit) {
            displayMenu(input);
            int menuSelected = input.nextInt();
            switch (menuSelected) {
                case 1:
                    MemberUI.memberOperator(input,
                            serviceControl.getMemberControl(),
                            serviceControl.getTransactionControl(),
                            serviceControl.getRequestControl(),
                            serviceControl.getRewardControl());
                    break;
                case 2:
                    TierUI.tierOperator(input,
                            serviceControl.getTierControl(), serviceControl.getMemberControl());
                    break;
                case 3:
                    RewardUI.rewardOperator(input, serviceControl.getRewardControl());
                    break;
                case 4:
                    ReportUI.reportOperator(input, serviceControl.getReportControl());
                    break;
                case 0:
                    exit = true;
                    break;
                default:
                    break;
            }
        }

        serviceControl.saveAll();

        return;
    }
}
