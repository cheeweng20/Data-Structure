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
import LoyaltyAndRewardsService.entity.Tier;
import LoyaltyAndRewardsService.utility.MessageUI;
import LoyaltyAndRewardsService.utility.ReportPdfExporter;
import LoyaltyAndRewardsService.utility.ReportPdfExporter.ChartType;
import LoyaltyAndRewardsService.utility.Verification;
import common.src.InputHelper;
import common.src.Logo;

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
        boolean exit = false;

        displayStartupNotifications();

        while (!exit) {
            displayMenu();
            int menuSelected = scanner.nextInt();
            switch (menuSelected) {
                case 1:
                    memberOperator();
                    break;
                case 2:
                    displayTierTable();
                    break;
                case 3:
                    reportOperator();
                    break;
                case 0:
                    exit = true;
                    break;
                default:
                    break;
            }
        }

        serviceControl.saveAll();
    }

    private void displayMenu() {
        Logo.displayLoyaltyAndRewardsService();
        System.out.println("\r\n" + //
                ".-----.-------------------.\r\n" + //
                "| No. |     Function      |\r\n" + //
                ":-----+-------------------:\r\n" + //
                "|  1. | Member Management |\r\n" + //
                ":-----+-------------------:\r\n" + //
                "|  2. | Tier Progression  |\r\n" + //
                ":-----+-------------------:\r\n" + //
                "|  3. | Report            |\r\n" + //
                "'-----'-------------------'\r\n" + //
                "\r\n" + //
                "");
        System.out.print("Enter Number of Function(0 to exit current program): ");
    }

    private void displayStartupNotifications() {
        int expiringTransactionCount =
                serviceControl.getExpiringTransactionCount(DEFAULT_EXPIRY_ALERT_DAYS);
        int expiringPointTotal =
                serviceControl.getExpiringPointTotal(DEFAULT_EXPIRY_ALERT_DAYS);
        int pendingRequestCount = serviceControl.getPendingRequestCount();

        System.out.println();
        System.out.println("=== Loyalty Notifications ===");
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

        int unreadUpgradeCount = serviceControl.getUnreadTierUpgradeCount();
        if (unreadUpgradeCount > 0) {
            Iterator<Member> iterator = serviceControl.getUnreadTierUpgradeIterator();
            while (iterator.hasNext()) {
                Member member = iterator.next();
                MessageUI.displayTierUpgradeAlert(
                        member.getMemberId(),
                        serviceControl.getTierName(member.getLastNotifiedTierId()),
                        serviceControl.getTierName(member.getTierId()));
            }
            serviceControl.markTierUpgradesAsRead();
        } else {
            MessageUI.displayInfo("No unread tier-upgrade notifications.");
        }
        System.out.println();
    }

    private void memberOperator() {
        boolean exit = false;

        while (!exit) {
            System.out.println("\r\n"
                    + ".-----.----------------------.\r\n"
                    + "| No. |       Function       |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 1.  | New Member           |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 2.  | Remove Member        |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 3.  | Update Member Info   |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 4.  | Redemption Requests  |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 5.  | Member List          |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 6.  | Member Promotion     |\r\n"
                    + "'-----'----------------------'\r\n");

            int userEntry = InputHelper.inputInt(scanner, "Please enter a number (0 to exit): ");
            switch (userEntry) {
                case 1:
                    addMember();
                    break;
                case 2:
                    removeMember();
                    break;
                case 3:
                    updateMember();
                    break;
                case 4:
                    if (serviceControl.isMemberEmpty()) {
                        MessageUI.displayInfo("No member records found.");
                        break;
                    }
                    scanner.nextLine();
                    requestOperator();
                    break;
                case 5:
                    displayMemberTable();
                    break;
                case 6:
                    displayPromotion();
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

    private void addMember() {
        scanner.nextLine();
        String name = InputHelper.inputString(scanner, "Enter member name: ");
        String passport = InputHelper.inputString(scanner, "Enter passport number: ");
        String phoneNumber = InputHelper.inputString(scanner, "Enter phone number: ");

        if (!validateMemberDetails(name, passport, phoneNumber, null)) {
            return;
        }

        String memberId = serviceControl.createMember(name, passport, phoneNumber);
        MessageUI.displaySuccess("Member " + memberId + " added successfully.");
    }

    private void removeMember() {
        if (serviceControl.isMemberEmpty()) {
            MessageUI.displayInfo("No member records found.");
            return;
        }

        scanner.nextLine();
        displayMemberTable();
        String memberId = InputHelper.inputString(scanner, "Enter member ID: ");
        if (serviceControl.removeMember(memberId)) {
            MessageUI.displaySuccess("Member deleted successfully.");
        } else {
            MessageUI.displayError(
                    "Member not found or has a pending redemption request.");
        }
    }

    private void updateMember() {
        if (serviceControl.isMemberEmpty()) {
            MessageUI.displayInfo("No member records found.");
            return;
        }

        scanner.nextLine();
        displayMemberTable();
        String memberId = InputHelper.inputString(scanner, "Enter member ID to update: ");
        if (!serviceControl.findMember(memberId)) {
            MessageUI.displayError("Member not found.");
            return;
        }

        String newName = InputHelper.inputString(scanner, "Enter new member name: ");
        String newPassport = InputHelper.inputString(scanner, "Enter new passport number: ");
        String newPhoneNumber = InputHelper.inputString(scanner, "Enter new phone number: ");
        if (!validateMemberDetails(
                newName, newPassport, newPhoneNumber, memberId)) {
            return;
        }

        serviceControl.updateMember(
                memberId, newName, newPassport, newPhoneNumber);
        MessageUI.displaySuccess("Member updated successfully.");
    }

    private boolean validateMemberDetails(String name, String passport,
            String phoneNumber, String excludedMemberId) {
        if (!Verification.isValidMemberName(name)) {
            MessageUI.displayError(
                    "Member name must contain 3 to 20 valid name characters.");
            return false;
        }
        if (!serviceControl.isMemberNameAvailable(name, excludedMemberId)) {
            MessageUI.displayError(
                    "Member name already exists. Please enter a different member name.");
            return false;
        }
        if (!Verification.isValidPassport(passport)) {
            MessageUI.displayError(
                    "Passport number must contain 5 to 20 letters or numbers.");
            return false;
        }
        if (!Verification.isValidPhoneNumber(phoneNumber)) {
            MessageUI.displayError(
                    "Phone number must contain 7 to 20 digits; +, spaces, and hyphens are allowed.");
            return false;
        }
        return true;
    }

    private void displayPromotion() {
        scanner.nextLine();
        String memberId = InputHelper.inputString(scanner, "Enter member ID: ");
        if (!serviceControl.findMember(memberId)) {
            MessageUI.displayError("Member not found.");
            return;
        }
        MessageUI.displayInfo(serviceControl.generatePersonalizedPromotion(memberId));
    }

    private void displayMemberTable() {
        if (serviceControl.isMemberEmpty()) {
            MessageUI.displayInfo("No member records found.");
            return;
        }

        String border = "+------------+----------------------+------------------+------------------+------------+------------+----------------+";
        System.out.println(border);
        System.out.printf(
                "| %-10s | %-20s | %-16s | %-16s | %10s | %10s | %-14s |%n",
                "Member ID", "Name", "Passport", "Phone Number", "Available", "Lifetime",
                "Tier");
        System.out.println(border);
        for (int i = 1; i <= serviceControl.getMemberCount(); i++) {
            Member member = serviceControl.getMemberEntry(i);
            System.out.printf(
                    "| %-10.10s | %-20.20s | %-16.16s | %-16.16s | %10d | %10d | %-14.14s |%n",
                    member.getMemberId(), member.getName(), member.getPassport(),
                    member.getPhoneNumber(), member.getPoint(),
                    member.getLifetimePointsEarned(),
                    serviceControl.getTierNameById(member.getTierId()));
        }
        System.out.println(border);
    }

    private void requestOperator() {
        boolean exit = false;

        while (!exit) {
            System.out.println("\r\n"
                    + ".-----.---------------------------.\r\n"
                    + "| No. |         Function          |\r\n"
                    + ":-----+---------------------------:\r\n"
                    + "|  1. | Process Next Request      |\r\n"
                    + ":-----+---------------------------:\r\n"
                    + "|  2. | View Pending Requests     |\r\n"
                    + ":-----+---------------------------:\r\n"
                    + "|  3. | View Request History      |\r\n"
                    + "'-----'---------------------------'\r\n");

            int userEntry = InputHelper.inputInt(scanner, "Please enter a number (0 to exit): ");
            scanner.nextLine();

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
        System.out.println("=== " + title + " ===");
        printRequestTableBorder();
        System.out.printf("| %-10s | %-10s | %-20s | %16s | %-10s | %-30s |%n",
                "Request ID", "Member ID", "Confirmation No.", "Points", "Date", "Status");
        printRequestTableBorder();
    }

    private void printRequestTableLine(RedemptionRequest request) {
        System.out.printf("| %-10.10s | %-10.10s | %-20.20s | %16d | %-10s | %-30.30s |%n",
                request.getRequestId(), request.getMemberId(),
                request.getConfirmationNumber(), request.getPointsRequested(),
                request.getRequestDate(), request.getStatus());
    }

    private void printRequestTableBorder() {
        System.out.println(
                "+------------+------------+----------------------+------------------+------------+--------------------------------+");
    }

    private void displayTierTable() {
        Iterator<Tier> iterator = serviceControl.getTierIterator();
        if (!iterator.hasNext()) {
            MessageUI.displayInfo("No tier records found.");
            return;
        }

        String border = "+------------+----------------------+------------+------------+";
        System.out.println(border);
        System.out.printf("| %-10s | %-20s | %10s | %10s |%n",
                "Tier ID", "Tier Level", "Min Points", "Max Points");
        System.out.println(border);
        while (iterator.hasNext()) {
            Tier tier = iterator.next();
            String maxPoints = tier.getMaxPoint() == 0
                    ? "No limit" : String.valueOf(tier.getMaxPoint());
            System.out.printf("| %-10.10s | %-20.20s | %10d | %10s |%n",
                    tier.getTierId(), tier.getTierLevel(), tier.getMinPoint(), maxPoints);
        }
        System.out.println(border);
    }

    private void reportOperator() {
        boolean exit = false;

        while (!exit) {
            System.out.println("\r\n"
                    + ".-----.-----------------------------.\r\n"
                    + "| No. |          Function           |\r\n"
                    + ":-----+-----------------------------:\r\n"
                    + "|  1. | Expiring Points Alert       |\r\n"
                    + ":-----+-----------------------------:\r\n"
                    + "|  2. | Business Cycle Summary      |\r\n"
                    + "'-----'-----------------------------'\r\n");

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
            scanner.nextLine();
            offerPdfExport("Expiring Points Alert", report,
                    ChartType.EXPIRING_POINTS);
        }
    }

    private void displayBusinessCycleSummary() {
        scanner.nextLine();
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
        scanner.nextLine();
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
                        serviceControl.getTierNameById(member.getTierId()), member.getPoint()));
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
