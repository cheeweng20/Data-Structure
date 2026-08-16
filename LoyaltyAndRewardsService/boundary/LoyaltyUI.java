package LoyaltyAndRewardsService.boundary;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Scanner;

import LoyaltyAndRewardsService.control.LoyaltyServiceControl;
import LoyaltyAndRewardsService.entity.Member;
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

        displayStartupNotifications(serviceControl);

        while (!exit) {
            displayMenu();
            int menuSelected = scanner.nextInt();
            switch (menuSelected) {
                case 1:
                    memberOperator(scanner, serviceControl);
                    break;
                case 2:
                    tierOperator(scanner, serviceControl);
                    break;
                case 3:
                    rewardOperator(scanner, serviceControl);
                    break;
                case 4:
                    reportOperator(scanner, serviceControl);
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
                "|  2. | Tier Management   |\r\n" + //
                ":-----+-------------------:\r\n" + //
                "|  3. | Rewards Management|\r\n" + //
                ":-----+-------------------:\r\n" + //
                "|  4. | Report            |\r\n" + //
                "'-----'-------------------'\r\n" + //
                "\r\n" + //
                "");
        System.out.print("Enter Number of Function(0 to exit current program): ");
    }

    public static void displayStartupNotifications(LoyaltyServiceControl serviceControl) {
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

    public static void memberOperator(Scanner scanner, LoyaltyServiceControl serviceControl) {
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
                    + "| 4.  | Add Point for Member |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 5.  | Point Redemption     |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 6.  | Member List          |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 7.  | Member Promotion     |\r\n"
                    + "'-----'----------------------'\r\n");

            int userEntry = InputHelper.inputInt(scanner, "Please enter a number (0 to exit): ");
            switch (userEntry) {
                case 1:
                    addMember(scanner, serviceControl);
                    break;
                case 2:
                    removeMember(scanner, serviceControl);
                    break;
                case 3:
                    updateMember(scanner, serviceControl);
                    break;
                case 4:
                    addMemberPoints(scanner, serviceControl);
                    break;
                case 5:
                    if (serviceControl.isMemberEmpty()) {
                        MessageUI.displayInfo("No member records found.");
                        break;
                    }
                    scanner.nextLine();
                    requestOperator(scanner, serviceControl);
                    break;
                case 6:
                    displayMemberTable(serviceControl);
                    break;
                case 7:
                    displayPromotion(scanner, serviceControl);
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

    private static void addMember(Scanner scanner, LoyaltyServiceControl serviceControl) {
        scanner.nextLine();
        String name = InputHelper.inputString(scanner, "Enter member name: ");
        String passport = InputHelper.inputString(scanner, "Enter passport number: ");
        String phoneNumber = InputHelper.inputString(scanner, "Enter phone number: ");
        int point = InputHelper.inputInt(scanner, "Enter current member points: ");

        if (!Verification.verifyMemberPoint(point)
                || !Verification.verifyMemberName(name, serviceControl)
                || !Verification.verifyPassport(passport)
                || !Verification.verifyPhoneNumber(phoneNumber)) {
            return;
        }

        String memberId = serviceControl.createMember(name, passport, phoneNumber, point);
        MessageUI.displaySuccess("Member " + memberId + " added successfully.");
    }

    private static void removeMember(Scanner scanner, LoyaltyServiceControl serviceControl) {
        if (serviceControl.isMemberEmpty()) {
            MessageUI.displayInfo("No member records found.");
            return;
        }

        scanner.nextLine();
        displayMemberTable(serviceControl);
        String memberId = InputHelper.inputString(scanner, "Enter member ID: ");
        if (serviceControl.removeMember(memberId)) {
            MessageUI.displaySuccess("Member deleted successfully.");
        } else {
            MessageUI.displayError("Member not found.");
        }
    }

    private static void updateMember(Scanner scanner, LoyaltyServiceControl serviceControl) {
        if (serviceControl.isMemberEmpty()) {
            MessageUI.displayInfo("No member records found.");
            return;
        }

        scanner.nextLine();
        displayMemberTable(serviceControl);
        String memberId = InputHelper.inputString(scanner, "Enter member ID to update: ");
        if (!serviceControl.findMember(memberId)) {
            MessageUI.displayError("Member not found.");
            return;
        }

        String newName = InputHelper.inputString(scanner, "Enter new member name: ");
        String newPassport = InputHelper.inputString(scanner, "Enter new passport number: ");
        String newPhoneNumber = InputHelper.inputString(scanner, "Enter new phone number: ");
        int newPoint = InputHelper.inputInt(scanner, "Enter new member points: ");
        if (!Verification.verifyMemberPoint(newPoint)
                || !Verification.verifyMemberName(newName, memberId, serviceControl)
                || !Verification.verifyPassport(newPassport)
                || !Verification.verifyPhoneNumber(newPhoneNumber)) {
            return;
        }

        serviceControl.updateMember(
                memberId, newName, newPassport, newPhoneNumber, newPoint);
        MessageUI.displaySuccess("Member updated successfully.");
    }

    private static void addMemberPoints(Scanner scanner, LoyaltyServiceControl serviceControl) {
        if (serviceControl.isMemberEmpty()) {
            MessageUI.displayInfo("No member records found.");
            return;
        }

        scanner.nextLine();
        String memberId = InputHelper.inputString(scanner, "Enter member ID: ");
        int addedPoint = InputHelper.inputInt(scanner, "Enter points to add: ");
        if (addedPoint <= 0) {
            MessageUI.displayError("Points to add must be greater than zero.");
            return;
        }

        serviceControl.addPoints(memberId, addedPoint);
    }

    private static void displayPromotion(Scanner scanner, LoyaltyServiceControl serviceControl) {
        scanner.nextLine();
        String memberId = InputHelper.inputString(scanner, "Enter member ID: ");
        if (!serviceControl.findMember(memberId)) {
            MessageUI.displayError("Member not found.");
            return;
        }
        MessageUI.displayInfo(serviceControl.generatePersonalizedPromotion(memberId));
    }

    private static void displayMemberTable(LoyaltyServiceControl serviceControl) {
        String table = serviceControl.getMemberTable();
        if (table.isEmpty()) {
            MessageUI.displayInfo("No member records found.");
        } else {
            System.out.println(table);
        }
    }

    public static void requestOperator(Scanner scanner, LoyaltyServiceControl serviceControl) {
        boolean exit = false;

        while (!exit) {
            System.out.println("\r\n"
                    + ".-----.---------------------------.\r\n"
                    + "| No. |         Function          |\r\n"
                    + ":-----+---------------------------:\r\n"
                    + "|  1. | Submit Redemption Request |\r\n"
                    + ":-----+---------------------------:\r\n"
                    + "|  2. | Process Next Request      |\r\n"
                    + ":-----+---------------------------:\r\n"
                    + "|  3. | View Pending Requests     |\r\n"
                    + ":-----+---------------------------:\r\n"
                    + "|  4. | View Request History      |\r\n"
                    + "'-----'---------------------------'\r\n");

            int userEntry = InputHelper.inputInt(scanner, "Please enter a number (0 to exit): ");
            scanner.nextLine();

            switch (userEntry) {
                case 1:
                    submitRequest(scanner, serviceControl);
                    break;
                case 2:
                    processRequest(scanner, serviceControl);
                    break;
                case 3:
                    displayRequestTable(serviceControl.getPendingRequestTable());
                    break;
                case 4:
                    displayRequestTable(serviceControl.getRequestHistoryTable());
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

    private static void submitRequest(Scanner scanner, LoyaltyServiceControl serviceControl) {
        if (serviceControl.isRewardEmpty()) {
            MessageUI.displayInfo("No reward records found. Please create a reward first.");
            return;
        }

        System.out.println(serviceControl.getRewardTable());
        String rewardId = InputHelper.inputString(scanner, "Enter reward ID: ");
        String memberId = InputHelper.inputString(scanner, "Enter member ID: ");
        serviceControl.submitRewardRequest(memberId, rewardId);
    }

    private static void processRequest(Scanner scanner, LoyaltyServiceControl serviceControl) {
        String nextRequest = serviceControl.getNextRequestTable();
        if (nextRequest.isEmpty()) {
            MessageUI.displayInfo("No pending requests.");
            return;
        }
        System.out.println(nextRequest);

        String decision = InputHelper.inputString(scanner, "Approve this request? (Y/N): ");
        if (!decision.equalsIgnoreCase("Y") && !decision.equalsIgnoreCase("N")) {
            MessageUI.displayError("Please enter Y or N.");
            return;
        }

        serviceControl.processNextRequestAndSave(decision.equalsIgnoreCase("Y"));
    }

    private static void displayRequestTable(String table) {
        if (table.isEmpty()) {
            MessageUI.displayInfo("No request records found.");
        } else {
            System.out.println(table);
        }
    }

    public static void tierOperator(Scanner scanner, LoyaltyServiceControl serviceControl) {
        boolean exit = false;

        while (!exit) {
            System.out.println("\r\n"
                    + ".-----.------------------------.\r\n"
                    + "| No. |        Function        |\r\n"
                    + ":-----+------------------------:\r\n"
                    + "|  1. | New Tier Level         |\r\n"
                    + ":-----+------------------------:\r\n"
                    + "|  2. | Remove Tier Level      |\r\n"
                    + ":-----+------------------------:\r\n"
                    + "|  3. | Update Tier Level Info |\r\n"
                    + ":-----+------------------------:\r\n"
                    + "|  4. | Tier List              |\r\n"
                    + "'-----'------------------------'\r\n");

            int userEntry = InputHelper.inputInt(scanner, "Please enter a number (0 to exit): ");
            switch (userEntry) {
                case 1:
                    addTier(scanner, serviceControl);
                    break;
                case 2:
                    removeTier(scanner, serviceControl);
                    break;
                case 3:
                    updateTier(scanner, serviceControl);
                    break;
                case 4:
                    displayTierTable(serviceControl);
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

    private static void addTier(Scanner scanner, LoyaltyServiceControl serviceControl) {
        scanner.nextLine();
        String tierName = InputHelper.inputString(scanner, "Enter tier name: ");
        int minimumPoint = InputHelper.inputInt(scanner, "Enter minimum points: ");

        if (serviceControl.isTierEmpty() && minimumPoint != 0) {
            MessageUI.displayError("The first tier must start at 0 points.");
            return;
        }
        if (!Verification.verifyTierName(tierName, serviceControl)
                || !Verification.verifyTierPoints(minimumPoint, 0)) {
            return;
        }
        if (!serviceControl.isMinimumPointAvailable(minimumPoint, null)) {
            MessageUI.displayError("Another tier already uses that minimum point.");
            return;
        }

        serviceControl.createTier(tierName, minimumPoint);
    }

    private static void removeTier(Scanner scanner, LoyaltyServiceControl serviceControl) {
        if (serviceControl.isTierEmpty()) {
            MessageUI.displayInfo("No tier records found.");
            return;
        }

        scanner.nextLine();
        displayTierTable(serviceControl);
        String tierId = InputHelper.inputString(scanner, "Enter tier ID: ");
        if (!serviceControl.findTier(tierId)) {
            MessageUI.displayError("Tier level not found.");
            return;
        }
        if (serviceControl.getTierCount() > 1 && serviceControl.isBaseTier(tierId)) {
            MessageUI.displayError("The base tier cannot be deleted while higher tiers exist.");
            return;
        }

        serviceControl.removeTier(tierId);
    }

    private static void updateTier(Scanner scanner, LoyaltyServiceControl serviceControl) {
        if (serviceControl.isTierEmpty()) {
            MessageUI.displayInfo("No tier records found.");
            return;
        }

        scanner.nextLine();
        displayTierTable(serviceControl);
        String tierId = InputHelper.inputString(scanner, "Enter tier ID to update: ");
        if (!serviceControl.findTier(tierId)) {
            MessageUI.displayError("Tier level not found.");
            return;
        }

        String newName = InputHelper.inputString(scanner, "Enter new tier name: ");
        int minimumPoint = InputHelper.inputInt(scanner, "Enter new minimum points: ");
        if (serviceControl.isBaseTier(tierId) && minimumPoint != 0) {
            MessageUI.displayError("The base tier must continue to start at 0 points.");
            return;
        }
        if (!Verification.verifyTierPoints(minimumPoint, 0)
                || !Verification.verifyTierName(newName, tierId, serviceControl)) {
            return;
        }
        if (!serviceControl.isMinimumPointAvailable(minimumPoint, tierId)) {
            MessageUI.displayError("Another tier already uses that minimum point.");
            return;
        }

        serviceControl.updateTier(tierId, newName, minimumPoint);
    }

    private static void displayTierTable(LoyaltyServiceControl serviceControl) {
        String table = serviceControl.getTierTable();
        if (table.isEmpty()) {
            MessageUI.displayInfo("No tier records found.");
        } else {
            System.out.println(table);
        }
    }

    public static void rewardOperator(Scanner scanner, LoyaltyServiceControl serviceControl) {
        boolean exit = false;

        while (!exit) {
            System.out.println("\r\n"
                    + ".-----.----------------------.\r\n"
                    + "| No. |       Function       |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 1.  | New Reward           |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 2.  | Remove Reward        |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 3.  | Update Reward Info   |\r\n"
                    + ":-----+----------------------:\r\n"
                    + "| 4.  | Reward List          |\r\n"
                    + "'-----'----------------------'\r\n");

            int userEntry = InputHelper.inputInt(scanner, "Please enter a number (0 to exit): ");
            scanner.nextLine();

            switch (userEntry) {
                case 1:
                    addReward(scanner, serviceControl);
                    break;
                case 2:
                    removeReward(scanner, serviceControl);
                    break;
                case 3:
                    updateReward(scanner, serviceControl);
                    break;
                case 4:
                    displayRewardTable(serviceControl);
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

    private static void addReward(Scanner scanner, LoyaltyServiceControl serviceControl) {
        String rewardName = InputHelper.inputString(scanner, "Enter reward name: ");
        int pointRequired = InputHelper.inputInt(scanner, "Enter points required to redeem: ");
        if (!Verification.verifyRewardName(rewardName)
                || !Verification.verifyRewardPoints(pointRequired)) {
            return;
        }

        String rewardId = serviceControl.createReward(rewardName, pointRequired);
        MessageUI.displaySuccess("Reward " + rewardId + " added successfully.");
    }

    private static void removeReward(Scanner scanner, LoyaltyServiceControl serviceControl) {
        if (serviceControl.isRewardEmpty()) {
            MessageUI.displayInfo("No reward records found.");
            return;
        }

        displayRewardTable(serviceControl);
        String rewardId = InputHelper.inputString(scanner, "Enter reward ID to remove: ");
        if (serviceControl.removeReward(rewardId)) {
            MessageUI.displaySuccess("Reward deleted successfully.");
        } else {
            MessageUI.displayError("Reward not found.");
        }
    }

    private static void updateReward(Scanner scanner, LoyaltyServiceControl serviceControl) {
        if (serviceControl.isRewardEmpty()) {
            MessageUI.displayInfo("No reward records found.");
            return;
        }

        displayRewardTable(serviceControl);
        String rewardId = InputHelper.inputString(scanner, "Enter reward ID to update: ");
        if (!serviceControl.findReward(rewardId)) {
            MessageUI.displayError("Reward not found.");
            return;
        }

        String rewardName = InputHelper.inputString(scanner, "Enter new reward name: ");
        int pointRequired = InputHelper.inputInt(scanner, "Enter new points required: ");
        if (!Verification.verifyRewardName(rewardName)
                || !Verification.verifyRewardPoints(pointRequired)) {
            return;
        }

        serviceControl.updateReward(rewardId, rewardName, pointRequired);
        MessageUI.displaySuccess("Reward updated successfully.");
    }

    private static void displayRewardTable(LoyaltyServiceControl serviceControl) {
        String table = serviceControl.getRewardTable();
        if (table.isEmpty()) {
            MessageUI.displayInfo("No reward records found.");
        } else {
            System.out.println(table);
        }
    }

    public static void reportOperator(Scanner scanner, LoyaltyServiceControl serviceControl) {
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
                    displayMemberRanking(scanner, serviceControl);
                    break;
                case 2:
                    displayExpiringPoints(scanner, serviceControl);
                    break;
                case 3:
                    displayBusinessCycleSummary(scanner, serviceControl);
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

    private static void displayMemberRanking(Scanner scanner, LoyaltyServiceControl serviceControl) {
        int minimumPoint = InputHelper.inputInt(scanner, "Enter minimum member points: ");
        if (minimumPoint < 0) {
            MessageUI.displayError("Minimum point cannot be negative.");
            return;
        }

        scanner.nextLine();
        String tierId =
                InputHelper.inputString(scanner, "Enter tier ID to filter (blank for all): ");
        String report = serviceControl.generateMemberRankingReport(minimumPoint, tierId);
        if (displayReport(report, "No members match the criteria.")) {
            offerPdfExport(scanner, "Member Point Ranking Report", report,
                    ChartType.MEMBER_POINTS);
        }
    }

    private static void displayExpiringPoints(Scanner scanner, LoyaltyServiceControl serviceControl) {
        int withinDays =
                InputHelper.inputInt(scanner, "Alert for points expiring within how many days: ");
        if (withinDays < 0) {
            MessageUI.displayError("Number of days cannot be negative.");
            return;
        }

        String report = serviceControl.generateExpiringPointsReport(withinDays);
        if (displayReport(report, "No points are expiring within the selected period.")) {
            scanner.nextLine();
            offerPdfExport(scanner, "Expiring Points Alert", report,
                    ChartType.EXPIRING_POINTS);
        }
    }

    private static void displayBusinessCycleSummary(Scanner scanner,
            LoyaltyServiceControl serviceControl) {
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

        String report = serviceControl.generateBusinessCycleSummary(
                startDate, endDate, tierId, minimumPoint);
        System.out.println(report);
        scanner.nextLine();
        offerPdfExport(scanner, "Business Cycle Summary Report", report,
                ChartType.BUSINESS_SUMMARY);
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

    private static boolean displayReport(String report, String emptyMessage) {
        if (report.isEmpty()) {
            MessageUI.displayInfo(emptyMessage);
            return false;
        } else {
            System.out.println(report);
            return true;
        }
    }

    private static void offerPdfExport(Scanner scanner, String title, String report,
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
