package LoyaltyAndRewardsService;

import java.util.Scanner;

import LoyaltyAndRewardsService.boundary.*;
import LoyaltyAndRewardsService.control.*;
import LoyaltyAndRewardsService.dao.*;
import common.src.Logo;

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
                "|  3. | Report            |\r\n" + //
                "'-----'-------------------'\r\n" + //
                "\r\n" + //
                "");
        System.out.print("Enter Number of Function(0 to exit current program): ");
    }

    public static void LoyaltyAndRewardsServiceMain(Scanner input) {
        boolean exit = false;

        TierControl tierLevelList = new TierControl();
        TierDao.loadFromTierFile(tierLevelList);

        MemberControl memberList = new MemberControl(tierLevelList);
        MemberDao.loadFromMemberFile(memberList);

        TransactionControl transactionList = new TransactionControl();
        PointTransactionDao.loadFromTransactionFile(transactionList);

        RequestControl requestControl = new RequestControl(memberList);

        RequestDao.loadFromRequestFile(requestControl);
        while (!exit) {
            displayMenu(input);
            int menuSelected = input.nextInt();
            switch (menuSelected) {
                case 1:
                    MemberUI.memberOperator(input, memberList, tierLevelList, transactionList, requestControl);
                    break;
                case 2:
                    TierUI.tierOperator(input, tierLevelList);
                    break;
                case 3:
                    ReportUI.reportOperator(input, memberList, tierLevelList);
                    break;
                case 0:
                    exit = true;
                    break;
                default:
                    break;
            }
        }

        MemberDao.saveToMemberFile(memberList);
        TierDao.saveToTierFile(tierLevelList);
        PointTransactionDao.saveToTransactionFile(transactionList);
        RequestDao.saveToRequestFile(requestControl);

        return;
    }
}
