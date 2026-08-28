package VIPPriorityRoomAllocation.boundary;

import VIPPriorityRoomAllocation.control.ReservationManager;
import VIPPriorityRoomAllocation.reporting.ReportPdfExporter;
import VIPPriorityRoomAllocation.reporting.ReportPdfExporter.ChartType;
import VIPPriorityRoomAllocation.reporting.ReservationReportFormatter;
import common.ui.ConsoleAnimation;
import common.ui.ConsoleProgress;
import common.ui.ConsoleStyle;
import common.ui.InputHelper;
import common.ui.Logo;
import common.ui.MessageUI;
import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/** Handles the reservation report menu, display, and optional PDF export. */
final class ReservationReportUI {

    private final Scanner scanner;
    private final ReservationManager reservationManager;

    ReservationReportUI(Scanner scanner, ReservationManager reservationManager) {
        this.scanner = scanner;
        this.reservationManager = reservationManager;
    }

    void start() {
        boolean back = false;

        while (!back) {
            InputHelper.clearScreen();
            displayMenu();
            String choice = InputHelper.inputString(scanner, "Select an option: ").trim();

            switch (choice) {
                case "1":
                    displayMonthlyReservationSummary();
                    break;
                case "2":
                    displayMonthlyRoomAllocationReport();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    MessageUI.displayError("Invalid option. Please try again.");
            }
            if (!back) {
                InputHelper.pressEnterToContinue(scanner);
            }
        }
    }

    private void displayMenu() {
        Logo.display();
        System.out.println(ConsoleStyle.menu(ConsoleStyle.menuBox("RESERVATION REPORTS",
                "1|Monthly Reservation Summary",
                "2|Monthly Room Allocation Report",
                "0|Back")));
    }

    private void displayMonthlyReservationSummary() {
        YearMonth reportMonth = promptReportMonth();
        String report = ConsoleProgress.run(
                () -> ReservationReportFormatter.buildMonthlyReservationSummary(
                        reservationManager.getReservations(), reportMonth),
                "Fetching reservation information...",
                "Calculating reservation summary...",
                "Preparing report...");

        if (displayReport(report,
                "No reservation records found for " + reportMonth + ".")) {
            offerPdfExport("Monthly Reservation Summary", report,
                    ChartType.RESERVATION_STATUS);
        }
    }

    private void displayMonthlyRoomAllocationReport() {
        YearMonth reportMonth = promptReportMonth();
        String report = ConsoleProgress.run(
                () -> ReservationReportFormatter.buildMonthlyRoomAllocationReport(
                        reservationManager.getReservations(), reportMonth),
                "Fetching room allocation information...",
                "Calculating allocation summary...",
                "Preparing report...");

        if (displayReport(report,
                "No allocated room records found for " + reportMonth + ".")) {
            offerPdfExport("Monthly Room Allocation Report", report,
                    ChartType.TIER_ALLOCATION);
        }
    }

    private boolean displayReport(String report, String emptyMessage) {
        if (report.isEmpty()) {
            MessageUI.displayInfo(emptyMessage);
            return false;
        }

        System.out.println(removeChartData(report));
        return true;
    }

    private void offerPdfExport(String title, String report, ChartType chartType) {
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
            MessageUI.displayInfo(
                    "The PDF was generated but could not be opened automatically: "
                            + exception.getMessage());
        }
    }

    private String removeChartData(String report) {
        int chartDataIndex = report.indexOf("=== Reservation Status Chart Data ===");
        if (chartDataIndex < 0) {
            chartDataIndex = report.indexOf(
                    "=== Loyalty Tier Allocation Chart Data ===");
        }
        return chartDataIndex < 0
                ? report : report.substring(0, chartDataIndex).trim();
    }

    private YearMonth promptReportMonth() {
        while (true) {
            String input = InputHelper.inputString(
                    scanner, "Enter report month (yyyy-MM): ").trim();
            try {
                return YearMonth.parse(input);
            } catch (DateTimeParseException exception) {
                MessageUI.displayError("Invalid month. Please use yyyy-MM format.");
            }
        }
    }
}
