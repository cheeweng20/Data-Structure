package LoyaltyAndRewardsService;

import java.util.Scanner;

import LoyaltyAndRewardsService.boundary.MemberUI;
import LoyaltyAndRewardsService.boundary.TierUI;
import LoyaltyAndRewardsService.control.MemberControl;
import LoyaltyAndRewardsService.control.TierControl;
import LoyaltyAndRewardsService.dao.MemberDao;
import LoyaltyAndRewardsService.dao.TierDao;

public class LoyaltyandRewardsService {

    public static void displayMenu(Scanner scanner) {
        System.out.println("1. Member");
        System.out.println("2. Tier");
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
        while (!exit) {
            displayMenu(input);
            int menuSelected = input.nextInt();
            switch (menuSelected) {
                case 1:
                    MemberUI.memberOperator(input, memberList, tierLevelList);
                    break;
                case 2:
                    TierUI.tierOperator(input, tierLevelList);
                    break;
                case 0:
                    exit = true;
                default:
                    break;
            }
        }

        MemberDao.saveToMemberFile(memberList);
        TierDao.saveToTierFile(tierLevelList);
        input.close();
    }
}
