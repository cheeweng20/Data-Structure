package LoyaltyAndRewardsService.boundary;

import adt.ArrayList;
import common.src.InputHelper;
import common.src.Logo;

import java.util.Scanner;

import LoyaltyAndRewardsService.control.*;
import LoyaltyAndRewardsService.entity.*;

/**
 * @author Chee Weng
 */
public class ReportUI {

    public static void reportOperator(Scanner scanner, MemberControl memberControl, TierControl tierControl) {

        boolean exit = false;
        while (!exit) {
            Logo.displayLoyaltyAndRewardsService();

            System.out.println("\r\n" + //
                    ".-----.-----------------------------.\r\n" + //
                    "| No. |          Function           |\r\n" + //
                    ":-----+-----------------------------:\r\n" + //
                    "|  1. | Member Point Ranking Report |\r\n" + //
                    ":-----+-----------------------------:\r\n" + //
                    "|  2. | Member Low Point Report     |\r\n" + //
                    "'-----'-----------------------------'\r\n" + //
                    "\r\n" + //
                    "");
            int userSelection = InputHelper.inputInt(scanner, "Enter a number (0 to exit): ");
            switch (userSelection) {
                case 1:
                    memberRankingReport(scanner, memberControl, tierControl);
                    break;
                case 2:
                    lowPointMemberReport(scanner, memberControl, tierControl);
                    break;
                case 0:
                    exit = true;
                    break;
                default:
                    break;
            }
        }

    }

    private static void memberRankingReport(Scanner scanner, MemberControl memberControl, TierControl tierControl) {

        int minPoint = InputHelper.inputInt(scanner, "Enter minimum point of member: ");
        scanner.nextLine();
        String targetTierId = InputHelper.inputString(scanner, "Enter Tier ID to filter (or leave blank for all): ");

        ArrayList<Member> result = memberControl.generateRankingReport(minPoint, targetTierId);

        if (result.isEmpty()) {
            System.out.println("No members match the criteria.");
            return;
        }

        System.out.printf("%-10s %-15s %-10s %-8s%n", "TierName", "Name", "MemberId", "Point");
        System.out.println("---------------------------------------------");

        for (int i = 1; i <= result.getNumberOfEntries(); i++) {
            Member member = result.getEntry(i);

            String tierName = tierControl.getTierNameById(member.getTierId());

            System.out.printf("%-10s %-15s %-10s %-8d%n",
                    tierName, member.getName(), member.getMemberId(), member.getPoint());
        }
    }

    private static void lowPointMemberReport(Scanner scanner, MemberControl memberControl, TierControl tierControl) {
        int maxPoint = InputHelper.inputInt(scanner, "Enter maximum point threshold: ");
        scanner.nextLine();
        String excludeTierId = InputHelper.inputString(scanner, "Enter Tier ID to exclude (or leave blank): ");

        ArrayList<Member> result = memberControl.generateLowPointReport(maxPoint, excludeTierId);

        if (result.isEmpty()) {
            System.out.println("No members match the criteria.");
            return;
        }

        System.out.println("=== Low Point Member Report (Re-engagement Candidates) ===");
        System.out.printf("%-10s %-15s %-10s %-8s%n", "TierName", "Name", "MemberId", "Point");
        System.out.println("---------------------------------------------");

        for (int i = 1; i <= result.getNumberOfEntries(); i++) {
            Member member = result.getEntry(i);
            String tierName = tierControl.getTierNameById(member.getTierId());

            System.out.printf("%-10s %-15s %-10s %-8d%n",
                    tierName, member.getName(), member.getMemberId(), member.getPoint());
        }
    }
}
