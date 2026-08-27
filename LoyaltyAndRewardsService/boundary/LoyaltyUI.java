package LoyaltyAndRewardsService.boundary;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Scanner;

import adt.ArrayList;
import LoyaltyAndRewardsService.control.LoyaltyServiceControl;
import LoyaltyAndRewardsService.entity.Member;
import LoyaltyAndRewardsService.entity.PointTransaction;
import LoyaltyAndRewardsService.entity.RedemptionRequest;
import LoyaltyAndRewardsService.utility.MessageUI;
import LoyaltyAndRewardsService.utility.ReportPdfExporter;
import LoyaltyAndRewardsService.utility.ReportPdfExporter.ChartType;
import LoyaltyAndRewardsService.utility.TierPolicy;
import common.src.InputHelper;
import common.src.InputHelper.EndOfInputException;
import common.src.Logo;
import common.src.ConsoleStyle;

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
                        if (serviceControl.isMemberEmpty()) {
                            MessageUI.displayInfo("No member records found.");
                            break;
                        }
                        requestOperator();
                        openedSubmenu = true;
                        break;
                    case 2:
                        displayMemberTable();
                        break;
                    case 3:
                        displayTierTable();
                        break;
                    case 4:
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
        Logo.displayLoyaltyAndRewardsService();
        System.out.println(ConsoleStyle.menu("\r\n" + //
                ".-----.----------------------.\r\n" + //
                "| No. |       Function       |\r\n" + //
                ":-----+----------------------:\r\n" + //
                "|  1. | Redemption Requests  |\r\n" + //
                ":-----+----------------------:\r\n" + //
                "|  2. | Member List          |\r\n" + //
                ":-----+----------------------:\r\n" + //
                "|  3. | Tier Progression     |\r\n" + //
                ":-----+----------------------:\r\n" + //
                "|  4. | Report               |\r\n" + //
                ":-----+----------------------:\r\n" + //
                "|  0. | Back                 |\r\n" + //
                "'-----'----------------------'\r\n" + //
                "\r\n" + //
                ""));
    }

    private void displayStartupNotifications() {
        int expiringTransactionCount =
                serviceControl.getExpiringTransactionCount(DEFAULT_EXPIRY_ALERT_DAYS);
        int expiringPointTotal =
                serviceControl.getExpiringPointTotal(DEFAULT_EXPIRY_ALERT_DAYS);
        int pendingRequestCount = serviceControl.getPendingRequestCount();

        System.out.println();
        System.out.println(ConsoleStyle.title("=== Loyalty Notifications ==="));
        if (serviceControl.getRecentlyExpiredPointTotal() > 0) {
            MessageUI.displayPointsExpired(serviceControl.getRecentlyExpiredPointTotal());
        }
        if (expiringTransactionCount > 0) {
            MessageUI.displayInfo(expiringPointTotal + " unredeemed point(s) from "
                    + expiringTransactionCount + " transaction(s) will expire within "
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

        String border = "+------------+----------------------+------------------+------------------+------------+------------+----------------+";
        System.out.println(ConsoleStyle.tableBorder(border));
        System.out.print(ConsoleStyle.tableHeader(String.format(
                "| %-10s | %-20s | %-16s | %-16s | %10s | %10s | %-14s |%n",
                "Member ID", "Name", "Passport", "Phone Number", "Available", "Lifetime",
                "Tier")));
        System.out.println(ConsoleStyle.tableBorder(border));
        for (int i = 1; i <= serviceControl.getMemberCount(); i++) {
            Member member = serviceControl.getMemberEntry(i);
            System.out.printf(
                    "| %-10.10s | %-20.20s | %-16.16s | %-16.16s | %10d | %10d | %-14.14s |%n",
                    member.getMemberId(), member.getName(), member.getPassport(),
                    member.getPhoneNumber(), member.getPoint(),
                    member.getLifetimePointsEarned(),
                    serviceControl.getTierName(member));
        }
        System.out.println(ConsoleStyle.tableBorder(border));
    }

    private void requestOperator() {
        boolean exit = false;

        while (!exit) {
            InputHelper.clearScreen();
            System.out.println(ConsoleStyle.menu("\r\n"
                    + ".-----.---------------------------.\r\n"
                    + "| No. |         Function          |\r\n"
                    + ":-----+---------------------------:\r\n"
                    + "|  1. | Process Next Request      |\r\n"
                    + ":-----+---------------------------:\r\n"
                    + "|  2. | View Pending Requests     |\r\n"
                    + ":-----+---------------------------:\r\n"
                    + "|  3. | View Request History      |\r\n"
                    + "'-----'---------------------------'\r\n"));

            int userEntry = InputHelper.inputInt(scanner, "Please enter a number (0 to exit): ");
            switch (userEntry) {
                case 1:
                    processRequest();
                    break;
                case 2:
                    displayRequestTable("Pending Point-Payment Requests",
                            serviceControl.getPendingRequestIterator());
                    break;
                case 3:
                    displayRequestTable("Point-Payment Request History",
                            serviceControl.getRequestIterator());
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
        RedemptionRequest nextRequest = serviceControl.getNextPendingRequest();
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

        RedemptionRequest processed =
                serviceControl.processNextRequestAndSave(decision.equalsIgnoreCase("Y"));
        if (processed != null) {
            MessageUI.displayRequestProcessed(processed.getStatus());
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
        for (String tierId : TierPolicy.getTierIds()) {
            int maximumPoints = TierPolicy.getMaximumPoints(tierId);
            String maxPoints = maximumPoints == 0
                    ? "No limit" : String.valueOf(maximumPoints);
            System.out.printf("| %-10.10s | %-20.20s | %10d | %10s |%n",
                    tierId, TierPolicy.getTierNameById(tierId),
                    TierPolicy.getMinimumPoints(tierId), maxPoints);
        }
        System.out.println(ConsoleStyle.tableBorder(border));
    }

    private void reportOperator() {
        boolean exit = false;

        while (!exit) {
            InputHelper.clearScreen();
            System.out.println(ConsoleStyle.menu("\r\n"
                    + ".-----.-----------------------------.\r\n"
                    + "| No. |          Function           |\r\n"
                    + ":-----+-----------------------------:\r\n"
                    + "|  1. | Expiring Points Alert       |\r\n"
                    + ":-----+-----------------------------:\r\n"
                    + "|  2. | Business Cycle Summary      |\r\n"
                    + "'-----'-----------------------------'\r\n"));

            int selection = InputHelper.inputInt(scanner, "Enter a number (0 to exit): ");
            switch (selection) {
                case 1:
                    displayExpiringPoints();
                    break;
                case 2:
                    displayBusinessCycleSummary();
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

        String report = buildExpiringPointsReport(
                serviceControl.generateExpiringReport(withinDays));
        if (displayReport(report, "No points are expiring within the selected period.")) {
            offerPdfExport("Expiring Points Alert", report,
                    ChartType.EXPIRING_POINTS);
        }
    }

    private void displayBusinessCycleSummary() {
        LocalDate startDate = readDate("Enter cycle start date (YYYY-MM-DD): ");
        if (startDate == null) {
            return;
        }
        LocalDate endDate = readDate("Enter cycle end date (YYYY-MM-DD): ");
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

        ArrayList<Member> rankedMembers =
                serviceControl.generateRankingReport(minimumPoint, tierId);
        ArrayList<PointTransaction> transactions =
                serviceControl.generateTransactionReport(startDate, endDate);
        ArrayList<RedemptionRequest> requests =
                serviceControl.generateRequestReport(startDate, endDate);
        String report = buildBusinessCycleSummary(startDate, endDate,
                tierId, minimumPoint, rankedMembers, transactions, requests);
        System.out.println(report);
        offerPdfExport("Business Cycle Summary Report", report,
                ChartType.BUSINESS_SUMMARY);
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

    private String buildExpiringPointsReport(
            ArrayList<PointTransaction> transactions) {
        if (transactions.isEmpty()) {
            return "";
        }

        StringBuilder output = new StringBuilder();
        String border = "+----------------+------------+-----------------+--------------+";
        output.append("=== Expiring Points Alert ===\n");
        output.append(border).append("\n");
        output.append(String.format("| %-14s | %-10s | %15s | %-12s |%n",
                "Transaction ID", "Member ID", "Points Expiring", "Expiry Date"));
        output.append(border).append("\n");

        Iterator<PointTransaction> iterator = transactions.iterator();
        while (iterator.hasNext()) {
            PointTransaction transaction = iterator.next();
            output.append(String.format("| %-14.14s | %-10.10s | %15d | %-12s |%n",
                    transaction.getTransactionId(), transaction.getMemberId(),
                    transaction.getPointsRemaining(), transaction.getExpiryDate()));
        }
        output.append(border);
        return output.toString();
    }

    private String buildBusinessCycleSummary(
            LocalDate startDate, LocalDate endDate,
            String tierId, int minimumPoint, ArrayList<Member> rankedMembers,
            ArrayList<PointTransaction> transactions,
            ArrayList<RedemptionRequest> requests) {
        StringBuilder output = new StringBuilder();
        output.append("=== Business Cycle Summary Report ===\n");
        output.append("Cycle Period: ").append(startDate).append(" to ").append(endDate).append("\n");
        output.append("Applied Filters: tier=")
                .append(tierId == null || tierId.isBlank() ? "All" : tierId)
                .append(", minimum point=").append(minimumPoint)
                .append("\n\n");

        appendMemberSummary(output, rankedMembers);
        appendTransactionSummary(output, transactions);
        appendRequestSummary(output, requests);
        return output.toString();
    }

    private void appendMemberSummary(StringBuilder output,
            ArrayList<Member> members) {
        String border = "+------------+----------------------+------------+----------+";
        output.append("=== Top Members by Current Points ===\n");
        output.append(border).append("\n");
        output.append(String.format("| %-10s | %-20s | %-10s | %8s |%n",
                "Member ID", "Member Name", "Tier", "Points"));
        output.append(border).append("\n");

        if (members.isEmpty()) {
            output.append(String.format("| %-10s | %-20s | %-10s | %8s |%n",
                    "-", "No matching members", "-", "-"));
        } else {
            Iterator<Member> iterator = members.iterator();
            while (iterator.hasNext()) {
                Member member = iterator.next();
                output.append(String.format("| %-10.10s | %-20.20s | %-10.10s | %8d |%n",
                        member.getMemberId(), member.getName(),
                        serviceControl.getTierName(member), member.getPoint()));
            }
        }
        output.append(border).append("\n");
    }

    private void appendTransactionSummary(StringBuilder output,
            ArrayList<PointTransaction> transactions) {
        output.append("\n=== Transaction Summary ===\n");
        output.append("Transactions in cycle: ")
                .append(transactions.getNumberOfEntries()).append("\n");
        output.append("Total points earned in cycle: ")
                .append(serviceControl.calculateTotalPointsEarned(transactions)).append("\n");

        String border = "+----------------+------------+----------------+-------------+";
        output.append(border).append("\n");
        output.append(String.format("| %-14s | %-10s | %-14s | %-11s |%n",
                "Transaction ID", "Member ID", "Points Earned", "Earned Date"));
        output.append(border).append("\n");

        Iterator<PointTransaction> iterator = transactions.iterator();
        while (iterator.hasNext()) {
            PointTransaction transaction = iterator.next();
            output.append(String.format("| %-14.14s | %-10.10s | %14d | %-11s |%n",
                    transaction.getTransactionId(), transaction.getMemberId(),
                    transaction.getPointsEarned(), transaction.getEarnedDate()));
        }
        output.append(border).append("\n");
    }

    private void appendRequestSummary(StringBuilder output,
            ArrayList<RedemptionRequest> requests) {
        int pending = serviceControl.countRequestsByStatus(requests, "Pending");
        int approved = serviceControl.countRequestsByStatus(requests, "Approved");
        int rejected = serviceControl.countRequestsByStatus(requests, "Rejected");

        output.append("\n=== Point-Payment Redemption Request Summary ===\n");
        output.append("Requests in cycle: ").append(requests.getNumberOfEntries()).append("\n");
        output.append("Pending: ").append(pending)
                .append(", Approved: ").append(approved)
                .append(", Rejected: ").append(rejected).append("\n");

        String border =
                "+------------+------------+----------------------+------------------+--------------------+--------------------------------+";
        output.append(border).append("\n");
        output.append(String.format("| %-10s | %-10s | %-20s | %-16s | %-18s | %-30s |%n",
                "Request ID", "Member ID", "Confirmation No.", "Points Requested",
                "Request Date", "Status"));
        output.append(border).append("\n");

        Iterator<RedemptionRequest> iterator = requests.iterator();
        while (iterator.hasNext()) {
            RedemptionRequest request = iterator.next();
            output.append(String.format(
                    "| %-10.10s | %-10.10s | %-20.20s | %16d | %-18s | %-30.30s |%n",
                    request.getRequestId(), request.getMemberId(),
                    request.getConfirmationNumber(), request.getPointsRequested(),
                    request.getRequestDate(), request.getStatus()));
        }
        output.append(border);
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
            pdfPath = ReportPdfExporter.export(title, report, chartType);
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
