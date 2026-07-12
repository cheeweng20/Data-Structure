package LoyaltyAndRewardsService.boundary;

import adt.ArrayList;
import java.util.Scanner;

import LoyaltyAndRewardsService.control.*;
import LoyaltyAndRewardsService.entity.*;

public class ReportUI {

    public static void memberRankingReport(Scanner scanner, MemberControl memberControl, TierControl tierControl) {

        int minPoint = promptInt(scanner, "Enter minimum point of member: ");
        scanner.nextLine();
        String targetTierId = promptText(scanner, "Enter Tier ID to filter (or leave blank for all): ");

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

    public static void lowPointMemberReport(Scanner scanner, MemberControl memberControl, TierControl tierControl) {
        int maxPoint = promptInt(scanner, "Enter maximum point threshold: ");
        scanner.nextLine();
        String excludeTierId = promptText(scanner, "Enter Tier ID to exclude (or leave blank): ");

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

    private static String promptText(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private static int promptInt(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextInt();
    }
}
