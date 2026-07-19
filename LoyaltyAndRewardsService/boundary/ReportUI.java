package LoyaltyAndRewardsService.boundary;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import adt.ArrayList;
import common.src.InputHelper;

import java.util.Scanner;

import LoyaltyAndRewardsService.control.*;
import LoyaltyAndRewardsService.entity.*;
import LoyaltyAndRewardsService.utility.MessageUI;

/**
 * @author Chee Weng
 */
public class ReportUI {

    public static void reportOperator(Scanner scanner, MemberControl memberControl, TierControl tierControl,
            TransactionControl transactionControl, RequestControl requestControl) {

        boolean exit = false;
        while (!exit) {
            System.out.println("\r\n" + //
                    ".-----.-----------------------------.\r\n" + //
                    "| No. |          Function           |\r\n" + //
                    ":-----+-----------------------------:\r\n" + //
                    "|  1. | Member Point Ranking Report |\r\n" + //
                    ":-----+-----------------------------:\r\n" + //
                    "|  2. | Expiring Points Alert       |\r\n" + //
                    ":-----+-----------------------------:\r\n" + //
                    "|  3. | Business Cycle Summary      |\r\n" + //
                    "'-----'-----------------------------'\r\n" + //
                    "\r\n" + //
                    "");
            int userSelection = InputHelper.inputInt(scanner, "Enter a number (0 to exit): ");
            switch (userSelection) {
                case 1:
                    memberRankingReport(scanner, memberControl, tierControl);
                    break;
                case 2:
                    expiringPointsReport(scanner, transactionControl);
                    break;
                case 3:
                    businessCycleSummaryReport(scanner, memberControl, tierControl, transactionControl,
                            requestControl);
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

    private static void memberRankingReport(Scanner scanner, MemberControl memberControl, TierControl tierControl) {

        int minPoint = InputHelper.inputInt(scanner, "Enter minimum point of member: ");
        if (minPoint < 0) {
            MessageUI.displayError("Minimum point cannot be negative.");
            return;
        }
        scanner.nextLine();
        String targetTierId = InputHelper.inputString(scanner, "Enter Tier ID to filter (or leave blank for all): ");

        ArrayList<Member> result = memberControl.generateRankingReport(minPoint, targetTierId);

        if (result.isEmpty()) {
            MessageUI.displayInfo("No members match the criteria.");
            return;
        }

        displayMemberReportHeader("Member Point Ranking Report");

        for (int i = 1; i <= result.getNumberOfEntries(); i++) {
            Member member = result.getEntry(i);

            String tierName = tierControl.getTierNameById(member.getTierId());

            System.out.printf("| %-14.14s | %-20.20s | %-10.10s | %8d |%n",
                    tierName, member.getName(), member.getMemberId(), member.getPoint());
        }
        displayMemberReportFooter();
    }

    private static void expiringPointsReport(Scanner scanner, TransactionControl transactionControl) {
        int withinDays = InputHelper.inputInt(scanner, "Alert for points expiring within how many days: ");
        if (withinDays < 0) {
            MessageUI.displayError("Number of days cannot be negative.");
            return;
        }

        ArrayList<PointTransaction> result = transactionControl.generateExpiringReport(withinDays);
        if (result.isEmpty()) {
            MessageUI.displayInfo("No points are expiring within the selected period.");
            return;
        }

        System.out.println("=== Expiring Points Alert ===");
        String border = "+--------------+------------+----------------+--------------+";
        System.out.println(border);
        System.out.printf("| %-12s | %-10s | %14s | %-12s |%n", "Transaction ID", "Member ID", "Points Earned",
                "Expiry Date");
        System.out.println(border);
        for (int i = 1; i <= result.getNumberOfEntries(); i++) {
            PointTransaction transaction = result.getEntry(i);
            System.out.printf("| %-12.12s | %-10.10s | %14d | %-12s |%n", transaction.getTransactionId(),
                    transaction.getMemberId(), transaction.getPointsEarned(), transaction.getExpiryDate());
        }
        System.out.println(border);
    }

    private static void businessCycleSummaryReport(Scanner scanner, MemberControl memberControl,
            TierControl tierControl, TransactionControl transactionControl, RequestControl requestControl) {

        scanner.nextLine();
        LocalDate startDate = readDate(scanner, "Enter cycle start date (YYYY-MM-DD): ");
        if (startDate == null) {
            return;
        }

        LocalDate endDate = readDate(scanner, "Enter cycle end date (YYYY-MM-DD): ");
        if (endDate == null) {
            return;
        }

        if (endDate.isBefore(startDate)) {
            MessageUI.displayError("End date cannot be earlier than start date.");
            return;
        }

        String tierFilter = InputHelper.inputString(scanner,
                "Enter Tier ID to filter members (or leave blank for all): ");
        int minimumPoint = InputHelper.inputInt(scanner, "Enter minimum current point for ranking: ");
        if (minimumPoint < 0) {
            MessageUI.displayError("Minimum point cannot be negative.");
            return;
        }

        ArrayList<Member> rankedMembers = memberControl.generateRankingReport(minimumPoint, tierFilter);
        ArrayList<PointTransaction> cycleTransactions = filterTransactionsByDateRange(transactionControl, startDate,
                endDate);
        ArrayList<RedemptionRequest> cycleRequests = filterRequestsByDateRange(requestControl, startDate, endDate);

        System.out.println("=== Business Cycle Summary Report ===");
        System.out.println("Cycle Period: " + startDate + " to " + endDate);
        System.out.println("Applied Filters: tier=" + (tierFilter == null || tierFilter.isBlank() ? "All" : tierFilter)
                + ", minimum point=" + minimumPoint);
        System.out.println();

        displayMemberSummarySection("Top Members by Current Points", rankedMembers, tierControl);
        displayTransactionSummarySection(cycleTransactions);
        displayRequestSummarySection(cycleRequests);
    }

    private static LocalDate readDate(Scanner scanner, String prompt) {
        String input = InputHelper.inputString(scanner, prompt);

        try {
            return LocalDate.parse(input);
        } catch (DateTimeParseException e) {
            MessageUI.displayError("Invalid date format. Please use YYYY-MM-DD.");
            return null;
        }
    }

    private static ArrayList<PointTransaction> filterTransactionsByDateRange(TransactionControl transactionControl,
            LocalDate startDate, LocalDate endDate) {
        ArrayList<PointTransaction> filteredResult = new ArrayList<>();

        for (int i = 1; i <= transactionControl.size(); i++) {
            PointTransaction current = transactionControl.getEntry(i);
            if (!current.getEarnedDate().isBefore(startDate) && !current.getEarnedDate().isAfter(endDate)) {
                filteredResult.add(current);
            }
        }

        selectionSortTransactionsByEarnedDate(filteredResult);
        return filteredResult;
    }

    private static ArrayList<RedemptionRequest> filterRequestsByDateRange(RequestControl requestControl,
            LocalDate startDate, LocalDate endDate) {
        ArrayList<RedemptionRequest> filteredResult = new ArrayList<>();

        java.util.Iterator<RedemptionRequest> iterator = requestControl.getRequestIterator();
        while (iterator.hasNext()) {
            RedemptionRequest current = iterator.next();
            if (!current.getRequestDate().isBefore(startDate) && !current.getRequestDate().isAfter(endDate)) {
                filteredResult.add(current);
            }
        }

        selectionSortRequestsByDate(filteredResult);
        return filteredResult;
    }

    private static void displayMemberSummarySection(String title, ArrayList<Member> members, TierControl tierControl) {
        System.out.println("=== " + title + " ===");
        String border = "+------------+----------------------+------------+----------+";
        System.out.println(border);
        System.out.printf("| %-10s | %-20s | %-10s | %8s |%n", "Member ID", "Member Name", "Tier", "Points");
        System.out.println(border);

        if (members.isEmpty()) {
            System.out.println("| %-10s | %-20s | %-10s | %8s |".formatted("-", "No matching members", "-", "-"));
            System.out.println(border);
            return;
        }

        for (int i = 1; i <= members.getNumberOfEntries(); i++) {
            Member member = members.getEntry(i);
            String tierName = tierControl.getTierNameById(member.getTierId());
            System.out.printf("| %-10.10s | %-20.20s | %-10.10s | %8d |%n", member.getMemberId(), member.getName(),
                    tierName, member.getPoint());
        }
        System.out.println(border);
    }

    private static void displayTransactionSummarySection(ArrayList<PointTransaction> transactions) {
        int totalPointsEarned = 0;
        for (int i = 1; i <= transactions.getNumberOfEntries(); i++) {
            totalPointsEarned += transactions.getEntry(i).getPointsEarned();
        }

        System.out.println();
        System.out.println("=== Transaction Summary ===");
        System.out.println("Transactions in cycle: " + transactions.getNumberOfEntries());
        System.out.println("Total points earned in cycle: " + totalPointsEarned);

        String border = "+--------------+------------+----------------+------------+";
        System.out.println(border);
        System.out.printf("| %-12s | %-10s | %-14s | %-10s |%n", "Transaction ID", "Member ID", "Points Earned",
                "Earned Date");
        System.out.println(border);

        for (int i = 1; i <= transactions.getNumberOfEntries(); i++) {
            PointTransaction transaction = transactions.getEntry(i);
            System.out.printf("| %-12.12s | %-10.10s | %14d | %-10s |%n", transaction.getTransactionId(),
                    transaction.getMemberId(), transaction.getPointsEarned(), transaction.getEarnedDate());
        }
        System.out.println(border);
    }

    private static void displayRequestSummarySection(ArrayList<RedemptionRequest> requests) {
        int pending = 0;
        int approved = 0;
        int rejected = 0;

        for (int i = 1; i <= requests.getNumberOfEntries(); i++) {
            RedemptionRequest request = requests.getEntry(i);
            if ("Pending".equalsIgnoreCase(request.getStatus())) {
                pending++;
            } else if ("Approved".equalsIgnoreCase(request.getStatus())) {
                approved++;
            } else {
                rejected++;
            }
        }

        System.out.println();
        System.out.println("=== Redemption Request Summary ===");
        System.out.println("Requests in cycle: " + requests.getNumberOfEntries());
        System.out.println("Pending: " + pending + ", Approved: " + approved + ", Rejected: " + rejected);

        String border = "+------------+------------+----------------+--------------------+--------------+";
        System.out.println(border);
        System.out.printf("| %-10s | %-10s | %-14s | %-18s | %-12s |%n", "Request ID", "Member ID",
                "Points Requested", "Request Date", "Status");
        System.out.println(border);

        for (int i = 1; i <= requests.getNumberOfEntries(); i++) {
            RedemptionRequest request = requests.getEntry(i);
            System.out.printf("| %-10.10s | %-10.10s | %14d | %-18s | %-12.12s |%n", request.getRequestId(),
                    request.getMemberId(), request.getPointsRequested(), request.getRequestDate(), request.getStatus());
        }
        System.out.println(border);
    }

    private static void selectionSortTransactionsByEarnedDate(ArrayList<PointTransaction> list) {
        for (int i = 1; i <= list.getNumberOfEntries() - 1; i++) {
            int targetPosition = i;
            PointTransaction targetValue = list.getEntry(i);

            for (int j = i + 1; j <= list.getNumberOfEntries(); j++) {
                PointTransaction current = list.getEntry(j);
                if (current.getEarnedDate().isBefore(targetValue.getEarnedDate())) {
                    targetValue = current;
                    targetPosition = j;
                }
            }

            if (targetPosition != i) {
                PointTransaction temp = list.getEntry(i);
                list.replace(i, targetValue);
                list.replace(targetPosition, temp);
            }
        }
    }

    private static void selectionSortRequestsByDate(ArrayList<RedemptionRequest> list) {
        for (int i = 1; i <= list.getNumberOfEntries() - 1; i++) {
            int targetPosition = i;
            RedemptionRequest targetValue = list.getEntry(i);

            for (int j = i + 1; j <= list.getNumberOfEntries(); j++) {
                RedemptionRequest current = list.getEntry(j);
                if (current.getRequestDate().isBefore(targetValue.getRequestDate())) {
                    targetValue = current;
                    targetPosition = j;
                }
            }

            if (targetPosition != i) {
                RedemptionRequest temp = list.getEntry(i);
                list.replace(i, targetValue);
                list.replace(targetPosition, temp);
            }
        }
    }

    private static void displayMemberReportHeader(String title) {
        String border = "+----------------+----------------------+------------+----------+";
        System.out.println("=== " + title + " ===");
        System.out.println(border);
        System.out.printf("| %-14s | %-20s | %-10s | %8s |%n", "Tier Name", "Member Name", "Member ID", "Points");
        System.out.println(border);
    }

    private static void displayMemberReportFooter() {
        System.out.println("+----------------+----------------------+------------+----------+");
    }
}
