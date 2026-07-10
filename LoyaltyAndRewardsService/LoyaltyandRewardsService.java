package LoyaltyAndRewardsService;

import java.util.Scanner;

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
        tierLevelList.loadFromTierFile();

        MemberControl memberList = new MemberControl(tierLevelList);
        memberList.loadFromMemberFile();
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

        memberList.saveToMemberFile();
        tierLevelList.saveToTierFile();
        input.close();
    }
}
