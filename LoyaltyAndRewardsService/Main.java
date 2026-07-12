package LoyaltyAndRewardsService;

import java.util.Scanner;

import LoyaltyAndRewardsService.boundary.*;
import LoyaltyAndRewardsService.control.*;
import LoyaltyAndRewardsService.dao.*;

public class Main {

    public static void displayMenu(Scanner scanner) {
        System.out.println("1. Member");
        System.out.println("2. Tier");
        System.out.println("3. Member Point Ranking Report");
        System.out.println("4. Member Low Point Report");
        System.out.println("5. Member Point Redemption Request");
        System.out.println("0. Exit");
        System.out.print("Please Enter A number:");
    }

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
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
                    MemberUI.memberOperator(input, memberList, tierLevelList, transactionList);
                    break;
                case 2:
                    TierUI.tierOperator(input, tierLevelList);
                    break;
                case 3:
                    ReportUI.memberRankingReport(input, memberList, tierLevelList);
                    break;
                case 4:
                    ReportUI.lowPointMemberReport(input, memberList, tierLevelList);
                    break;
                case 5:
                    RequestUI.requestOperator(input, requestControl, memberList);
                    break;
                case 0:
                    exit = true;
                default:
                    break;
            }
        }

        MemberDao.saveToMemberFile(memberList);
        TierDao.saveToTierFile(tierLevelList);
        PointTransactionDao.saveToTransactionFile(transactionList);
        RequestDao.saveToRequestFile(requestControl);
        input.close();
    }
}
