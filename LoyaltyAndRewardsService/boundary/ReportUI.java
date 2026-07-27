package LoyaltyAndRewardsService.boundary;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

import LoyaltyAndRewardsService.control.ReportControl;
import LoyaltyAndRewardsService.utility.MessageUI;
import common.src.InputHelper;

/**
 * Handles report criteria input and displays reports produced by ReportControl.
 *
 * @author Chee Weng
 */
public class ReportUI {
    public static void reportOperator(Scanner scanner, ReportControl reportControl) {
        boolean exit = false;

        while (!exit) {
            System.out.println("\r\n"
                    + ".-----.-----------------------------.\r\n"
                    + "| No. |          Function           |\r\n"
                    + ":-----+-----------------------------:\r\n"
                    + "|  1. | Member Point Ranking Report |\r\n"
                    + ":-----+-----------------------------:\r\n"
                    + "|  2. | Expiring Points Alert       |\r\n"
                    + ":-----+-----------------------------:\r\n"
                    + "|  3. | Business Cycle Summary      |\r\n"
                    + "'-----'-----------------------------'\r\n");

            int selection = InputHelper.inputInt(scanner, "Enter a number (0 to exit): ");
            switch (selection) {
                case 1:
                    displayMemberRanking(scanner, reportControl);
                    break;
                case 2:
                    displayExpiringPoints(scanner, reportControl);
                    break;
                case 3:
                    displayBusinessCycleSummary(scanner, reportControl);
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

    private static void displayMemberRanking(Scanner scanner, ReportControl reportControl) {
        int minimumPoint = InputHelper.inputInt(scanner, "Enter minimum member points: ");
        if (minimumPoint < 0) {
            MessageUI.displayError("Minimum point cannot be negative.");
            return;
        }

        scanner.nextLine();
        String tierId =
                InputHelper.inputString(scanner, "Enter tier ID to filter (blank for all): ");
        displayReport(
                reportControl.generateMemberRankingReport(minimumPoint, tierId),
                "No members match the criteria.");
    }

    private static void displayExpiringPoints(Scanner scanner, ReportControl reportControl) {
        int withinDays =
                InputHelper.inputInt(scanner, "Alert for points expiring within how many days: ");
        if (withinDays < 0) {
            MessageUI.displayError("Number of days cannot be negative.");
            return;
        }

        displayReport(
                reportControl.generateExpiringPointsReport(withinDays),
                "No points are expiring within the selected period.");
    }

    private static void displayBusinessCycleSummary(Scanner scanner,
            ReportControl reportControl) {
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

        String tierId =
                InputHelper.inputString(scanner, "Enter tier ID to filter (blank for all): ");
        int minimumPoint =
                InputHelper.inputInt(scanner, "Enter minimum current point for ranking: ");
        if (minimumPoint < 0) {
            MessageUI.displayError("Minimum point cannot be negative.");
            return;
        }

        System.out.println(reportControl.generateBusinessCycleSummary(
                startDate, endDate, tierId, minimumPoint));
    }

    private static LocalDate readDate(Scanner scanner, String prompt) {
        String input = InputHelper.inputString(scanner, prompt);
        try {
            return LocalDate.parse(input);
        } catch (DateTimeParseException exception) {
            MessageUI.displayError("Invalid date format. Please use YYYY-MM-DD.");
            return null;
        }
    }

    private static void displayReport(String report, String emptyMessage) {
        if (report.isEmpty()) {
            MessageUI.displayInfo(emptyMessage);
        } else {
            System.out.println(report);
        }
    }
}
