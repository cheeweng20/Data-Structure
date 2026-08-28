package LoyaltyAndRewardsService.boundary;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Scanner;

import adt.SortedArrayList;
import LoyaltyAndRewardsService.control.LoyaltyServiceControl;
import LoyaltyAndRewardsService.control.LoyaltyServiceControl.ExpiringPointSummary;
import LoyaltyAndRewardsService.entity.Member;
import LoyaltyAndRewardsService.entity.PointTransaction;
import LoyaltyAndRewardsService.entity.RedemptionRequest;
import LoyaltyAndRewardsService.entity.Tier;
import LoyaltyAndRewardsService.reporting.LoyaltyReportFormatter;
import LoyaltyAndRewardsService.reporting.ReportPdfExporter;
import LoyaltyAndRewardsService.reporting.ReportPdfExporter.ChartType;
import common.ui.InputHelper;
import common.ui.InputHelper.EndOfInputException;
import common.ui.Logo;
import common.ui.MessageUI;
import common.ui.ConsoleStyle;
import common.ui.ConsoleProgress;
import common.ui.ConsoleAnimation;
import common.utility.Validation;

/**
 * Handles all actor interaction for loyalty and rewards use cases.
 *
 * @author Chee Weng
 */
public final class LoyaltyUI {
    private static final int DEFAULT_EXPIRY_ALERT_DAYS = 30;

    private final Scanner scanner;
    private final LoyaltyServiceControl serviceControl;

    public LoyaltyUI(Scanner scanner) {
        this.scanner = scanner;
        serviceControl = new LoyaltyServiceControl();
    }

    public void start() {
        try {
            boolean exit = false;

            displayStartupNotifications();
            InputHelper.pressEnterToContinue(scanner);

            while (!exit) {
                InputHelper.clearScreen();
                displayMenu();
                int menuSelected = InputHelper.inputInt(scanner,
                        "Enter number of function (0 to exit current program): ");
                boolean openedSubmenu = false;
                switch (menuSelected) {
                    case 1:
                        registerMember();
                        break;
                    case 2:
                        viewMemberLoyaltyInformation();
                        break;
                    case 3:
                        if (serviceControl.isMemberEmpty()) {
                            MessageUI.displayInfo("No member records found.");
                            break;
                        }
                        requestOperator();
                        openedSubmenu = true;
                        break;
                    case 4:
                        displayMemberTable();
                        break;
                    case 5:
                        displayTierTable();
                        break;
                    case 6:
                        reportOperator();
                        openedSubmenu = true;
                        break;
                    case 0:
                        exit = true;
                        break;
                    default:
                        MessageUI.displayError("Invalid option.");
                        break;
                }
                if (!exit && !openedSubmenu) {
                    InputHelper.pressEnterToContinue(scanner);
                }
            }
        } catch (EndOfInputException exception) {
            // EOF behaves like selecting Back.
        } finally {
            serviceControl.saveAll();
        }
    }

    private void displayMenu() {
        Logo.display();
        System.out.println(ConsoleStyle.menu(ConsoleStyle.menuBox("LOYALTY AND REWARDS",
                "section|MEMBER SERVICES",
                "1|Register as Member", "2|View Member / Loyalty Information",
                "section|LOYALTY MANAGEMENT",
                "3|Redemption Requests", "4|Member List", "5|Tier Progression",
                "section|REPORTING",
                "6|Loyalty Reports", "0|Back")));
    }

    private void registerMember() {
        System.out.println(ConsoleStyle.title("\n--- Register as Member ---"));

        String name = promptValidMemberName();
        String passport = promptValidPassport();
        if (!serviceControl.isPassportAvailable(passport)) {
            MessageUI.displayError("This passport is already registered.");
            return;
        }

        String phoneNumber = promptValidPhoneNumber();
        if (!serviceControl.isPhoneNumberAvailable(phoneNumber)) {
            MessageUI.displayError("This phone number is already registered.");
            return;
        }

        if (!confirmYes("Confirm registration? (Y/N): ")) {
            MessageUI.displayInfo("Member registration cancelled.");
            return;
        }

        String memberId = ConsoleProgress.run(
                () -> serviceControl.createMember(name, passport, phoneNumber),
                "Processing member details...",
                "Creating member profile...",
                "Saving member record...");
        MessageUI.displaySuccess("Member registered successfully.");
        System.out.println("Member ID: " + memberId);
    }

    private void viewMemberLoyaltyInformation() {
        if (serviceControl.isMemberEmpty()) {
            MessageUI.displayInfo("No member records found.");
            return;
        }

        String memberId = InputHelper.inputString(scanner, "Enter Member ID: ").trim();
        Member member = serviceControl.getMemberById(memberId);
        if (member == null) {
            MessageUI.displayError("Member not found.");
            return;
        }

        System.out.println(ConsoleStyle.title("\n--- Member / Loyalty Information ---"));
        System.out.println("Member ID       : " + member.getMemberId());
        System.out.println("Name            : " + member.getName());
        System.out.println("Passport        : " + member.getPassport());
        System.out.println("Phone Number    : " + member.getPhoneNumber());
        System.out.println("Available Points: " + member.getPoint());
        System.out.printf("Total Expenses  : RM%,.2f%n", (double) member.getTotalExpenses());
        System.out.println("Loyalty Tier    : " + serviceControl.getTierName(member));
        System.out.println("\n" + serviceControl.generatePersonalizedPromotion(memberId));
    }

    private String promptValidMemberName() {
        while (true) {
            String name = InputHelper.inputString(scanner, "Name: ").trim();
            if (Validation.isValidMemberName(name)) {
                return name;
            }
            MessageUI.displayError(
                    "Invalid name. Use 3-20 letters, spaces, apostrophes, hyphens, or dots.");
        }
    }

    private String promptValidPassport() {
        while (true) {
            String passport = InputHelper.inputString(scanner, "Passport: ").trim();
            if (Validation.isValidPassport(passport)) {
                return passport;
            }
            MessageUI.displayError("Invalid passport. Use 5-20 letters or digits.");
        }
    }

    private String promptValidPhoneNumber() {
        while (true) {
            String phoneNumber = InputHelper.inputString(scanner, "Phone Number: ").trim();
            if (Validation.isValidPhoneNumber(phoneNumber)) {
                return phoneNumber;
            }
            MessageUI.displayError("Invalid phone number.");
        }
    }

    private boolean confirmYes(String prompt) {
        while (true) {
            String choice = InputHelper.inputString(scanner, prompt).trim();
            if (choice.equalsIgnoreCase("Y")) {
                return true;
            }
            if (choice.equalsIgnoreCase("N")) {
                return false;
            }
            MessageUI.displayError("Please enter Y or N.");
        }
    }

    private void displayStartupNotifications() {
        ExpiringPointSummary expiringSummary =
                serviceControl.getExpiringPointSummary(DEFAULT_EXPIRY_ALERT_DAYS);
        int recentlyExpiredPoints = serviceControl.getRecentlyExpiredPointTotal();
        int pendingRequestCount = serviceControl.getPendingRequestCount();

        System.out.println();
        System.out.println(ConsoleStyle.title("=== Loyalty Notifications ==="));
        if (recentlyExpiredPoints > 0) {
            MessageUI.displayInfo(recentlyExpiredPoints
                    + " unredeemed transaction point(s) expired; member balances were updated.");
        }
        if (expiringSummary.transactionCount() > 0) {
            MessageUI.displayInfo(expiringSummary.pointTotal() + " unredeemed point(s) from "
                    + expiringSummary.transactionCount() + " transaction(s) will expire within "
                    + DEFAULT_EXPIRY_ALERT_DAYS + " days.");
        } else {
            MessageUI.displayInfo("No unredeemed points expire within "
                    + DEFAULT_EXPIRY_ALERT_DAYS + " days.");
        }

        if (pendingRequestCount > 0) {
            MessageUI.displayInfo(pendingRequestCount
                    + " redemption request(s) are waiting for processing.");
        } else {
            MessageUI.displayInfo("No redemption requests are waiting for processing.");
        }

        System.out.println();
    }

    private void displayMemberTable() {
        if (serviceControl.isMemberEmpty()) {
            MessageUI.displayInfo("No member records found.");
            return;
        }

        int memberCount = ConsoleProgress.run(
                serviceControl::getMemberCount,
                "Fetching member information...",
                "Loading loyalty profiles...",
                "Preparing member list...");

        String border = "+------------+----------------------+------------------+------------------+------------+------------+----------------+";
        System.out.println(ConsoleStyle.tableBorder(border));
        System.out.print(ConsoleStyle.tableHeader(String.format(
                "| %-10s | %-20s | %-16s | %-16s | %10s | %10s | %-14s |%n",
                "Member ID", "Name", "Passport", "Phone Number", "Available", "Expenses",
                "Tier")));
        System.out.println(ConsoleStyle.tableBorder(border));
        for (int i = 1; i <= memberCount; i++) {
            Member member = serviceControl.getMemberEntry(i);
            System.out.printf(
                    "| %-10.10s | %-20.20s | %-16.16s | %-16.16s | %10d | %10d | %-14.14s |%n",
                    member.getMemberId(), member.getName(), member.getPassport(),
                    member.getPhoneNumber(), member.getPoint(),
                    member.getTotalExpenses(),
                    serviceControl.getTierName(member));
        }
        System.out.println(ConsoleStyle.tableBorder(border));
    }

    private void requestOperator() {
        boolean exit = false;

        while (!exit) {
            InputHelper.clearScreen();
            System.out.println(ConsoleStyle.menu(ConsoleStyle.menuBox("REDEMPTION REQUESTS",
                    "1|Process Next Request", "2|View Pending Requests",
                    "3|View Request History", "0|Back")));

            int userEntry = InputHelper.inputInt(scanner, "Please enter a number (0 to exit): ");
            switch (userEntry) {
                case 1:
                    processRequest();
                    break;
                case 2:
                    displayRequestTable("Pending Point-Payment Requests",
                            ConsoleAnimation.runWithSpinner(
                                    serviceControl::getPendingRequestIterator,
                                    "Fetching pending requests"));
                    break;
                case 3:
                    displayRequestTable("Point-Payment Request History",
                            ConsoleAnimation.runWithSpinner(
                                    serviceControl::getRequestIterator,
                                    "Fetching request history"));
                    break;
                case 0:
                    exit = true;
                    break;
                default:
                    MessageUI.displayError("Invalid option.");
                    break;
            }
            if (!exit) {
                InputHelper.pressEnterToContinue(scanner);
            }
        }
    }

    private void processRequest() {
        RedemptionRequest nextRequest = ConsoleProgress.run(
                serviceControl::getNextPendingRequest,
                "Fetching request information...",
                "Loading the next pending request...",
                "Preparing request details...");
        if (nextRequest == null) {
            MessageUI.displayInfo("No pending requests.");
            return;
        }
        displayRequestTable("Next Point-Payment Request", nextRequest);

        String decision = InputHelper.inputString(scanner, "Approve this request? (Y/N): ");
        if (!decision.equalsIgnoreCase("Y") && !decision.equalsIgnoreCase("N")) {
            MessageUI.displayError("Please enter Y or N.");
            return;
        }

        RedemptionRequest processed = ConsoleProgress.run(
                () -> serviceControl.processNextRequestAndSave(decision.equalsIgnoreCase("Y")),
                "Processing redemption request...",
                "Updating points balance...",
                "Saving request result...");
        if (processed != null) {
            displayRequestProcessed(processed.getStatus());
        }
    }

    private void displayRequestProcessed(String status) {
        if ("Approved".equalsIgnoreCase(status)) {
            MessageUI.displaySuccess("Request approved.");
        } else {
            MessageUI.displayInfo("Request " + status + ".");
        }
    }

    private void displayRequestTable(String title,
            Iterator<RedemptionRequest> iterator) {
        if (!iterator.hasNext()) {
            MessageUI.displayInfo("No request records found.");
            return;
        }

        printRequestTableHeader(title);
        while (iterator.hasNext()) {
            printRequestTableLine(iterator.next());
        }
        printRequestTableBorder();
    }

    private void displayRequestTable(String title, RedemptionRequest request) {
        printRequestTableHeader(title);
        printRequestTableLine(request);
        printRequestTableBorder();
    }

    private void printRequestTableHeader(String title) {
        System.out.println(ConsoleStyle.title("=== " + title + " ==="));
        printRequestTableBorder();
        System.out.print(ConsoleStyle.tableHeader(String.format(
                "| %-10s | %-10s | %-20s | %16s | %-10s | %-30s |%n",
                "Request ID", "Member ID", "Confirmation No.", "Points", "Date", "Status")));
        printRequestTableBorder();
    }

    private void printRequestTableLine(RedemptionRequest request) {
        System.out.printf("| %-10.10s | %-10.10s | %-20.20s | %16d | %-10s | %-30.30s |%n",
                request.getRequestId(), request.getMemberId(),
                request.getConfirmationNumber(), request.getPointsRequested(),
                request.getRequestDate(), request.getStatus());
    }

    private void printRequestTableBorder() {
        System.out.println(ConsoleStyle.tableBorder(
                "+------------+------------+----------------------+------------------+------------+--------------------------------+"));
    }

    private void displayTierTable() {
        String border = "+------------+----------------------+------------+------------+";
        System.out.println(ConsoleStyle.tableBorder(border));
        System.out.print(ConsoleStyle.tableHeader(String.format(
                "| %-10s | %-20s | %10s | %10s |%n",
                "Tier ID", "Tier Level", "Min Points", "Max Points")));
        System.out.println(ConsoleStyle.tableBorder(border));
        for (Tier tier : Tier.getTiers()) {
            int maximumPoints = tier.getMaxPoint();
            String maxPoints = maximumPoints == 0
                    ? "No limit" : String.valueOf(maximumPoints);
            System.out.printf("| %-10.10s | %-20.20s | %10d | %10s |%n",
                    tier.getTierId(), tier.getTierLevel(),
                    tier.getMinPoint(), maxPoints);
        }
        System.out.println(ConsoleStyle.tableBorder(border));
    }

    private void reportOperator() {
        boolean exit = false;

        while (!exit) {
            InputHelper.clearScreen();
            System.out.println(ConsoleStyle.menu(ConsoleStyle.menuBox("LOYALTY REPORTS",
                    "1|Expiring Points Alert", "2|Points Transaction Report",
                    "0|Back")));

            int selection = InputHelper.inputInt(scanner, "Enter a number (0 to exit): ");
            switch (selection) {
                case 1:
                    displayExpiringPoints();
                    break;
                case 2:
                    displayPointsTransactionReport();
                    break;
                case 0:
                    exit = true;
                    break;
                default:
                    MessageUI.displayError("Invalid option.");
                    break;
            }
            if (!exit) {
                InputHelper.pressEnterToContinue(scanner);
            }
        }
    }

    private void displayExpiringPoints() {
        int withinDays =
                InputHelper.inputInt(scanner, "Alert for points expiring within how many days: ");
        if (withinDays < 0) {
            MessageUI.displayError("Number of days cannot be negative.");
            return;
        }

        String report = ConsoleProgress.run(
                () -> LoyaltyReportFormatter.buildExpiringPointsReport(
                        serviceControl.generateExpiringReport(withinDays)),
                "Fetching points information...",
                "Calculating expiry details...",
                "Preparing report...");
        if (displayReport(report, "No points are expiring within the selected period.")) {
            offerPdfExport("Expiring Points Alert", report,
                    ChartType.EXPIRING_POINTS);
        }
    }

    private void displayPointsTransactionReport() {
        LocalDate startDate = readDate("Enter report start date (YYYY-MM-DD): ");
        if (startDate == null) {
            return;
        }
        LocalDate endDate = readDate("Enter report end date (YYYY-MM-DD): ");
        if (endDate == null) {
            return;
        }
        if (endDate.isBefore(startDate)) {
            MessageUI.displayError("End date cannot be earlier than start date.");
            return;
        }

        SortedArrayList<PointTransaction> transactions = ConsoleProgress.run(
                () -> serviceControl.generateTransactionReport(startDate, endDate),
                "Fetching point transactions...",
                "Calculating transaction totals...",
                "Preparing report data...");
        String report = LoyaltyReportFormatter.buildPointsTransactionReport(
                startDate, endDate, transactions);
        if (displayReport(report,
                "No point transactions found within the selected period.")) {
            offerPdfExport("Points Transaction Report", report,
                    ChartType.POINTS_TRANSACTION);
        }
    }

    private LocalDate readDate(String prompt) {
        String input = InputHelper.inputString(scanner, prompt);
        try {
            return LocalDate.parse(input);
        } catch (DateTimeParseException exception) {
            MessageUI.displayError("Invalid date format. Please use YYYY-MM-DD.");
            return null;
        }
    }

    private boolean displayReport(String report, String emptyMessage) {
        if (report.isEmpty()) {
            MessageUI.displayInfo(emptyMessage);
            return false;
        } else {
            System.out.println(report);
            return true;
        }
    }

    private void offerPdfExport(String title, String report,
            ChartType chartType) {
        String selection = InputHelper.inputString(
                scanner, "Generate chart PDF and open it? (Y/N): ");
        if (!selection.equalsIgnoreCase("Y") && !selection.equalsIgnoreCase("Yes")) {
            return;
        }

        Path pdfPath;
        try {
            pdfPath = ConsoleAnimation.runIoWithSpinner(
                    () -> ReportPdfExporter.export(title, report, chartType),
                    "Generating report PDF");
        } catch (IOException exception) {
            MessageUI.displayError("Unable to generate PDF: " + exception.getMessage());
            return;
        }

        MessageUI.displaySuccess("PDF generated: " + pdfPath);
        try {
            if (!ReportPdfExporter.open(pdfPath)) {
                MessageUI.displayInfo("Open the PDF manually from the path shown above.");
            }
        } catch (IOException exception) {
            MessageUI.displayInfo("The PDF was generated but could not be opened automatically: "
                    + exception.getMessage());
        }
    }

}
