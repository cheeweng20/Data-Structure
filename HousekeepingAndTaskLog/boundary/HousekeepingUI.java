package HousekeepingAndTaskLog.boundary;

import HousekeepingAndTaskLog.control.HousekeepingControl;
import common.ui.Logo;
import common.ui.ConsoleStyle;
import common.ui.ConsoleProgress;
import common.ui.ConsoleAnimation;
import common.ui.InputHelper;
import common.ui.InputHelper.EndOfInputException;
import java.time.LocalDate;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/**
 * @author Zhe Sheng
 */

public class HousekeepingUI {

    private final Scanner scanner;
    private final HousekeepingControl housekeepingControl;

    public HousekeepingUI(Scanner scanner) {
        this.scanner = scanner;
        housekeepingControl = new HousekeepingControl();
    }

    public void start() {
        try {
            boolean exit = false;

            while (!exit) {
                InputHelper.clearScreen();
                displayMenu();
                String choice = InputHelper.inputString(
                        scanner, "Select an option [0-5]: ").trim();

                switch (choice) {
                    case "1":
                        addCleaningTask();
                        break;
                    case "2":
                        updateCleaningStatus();
                        break;
                    case "3":
                        rollbackLastChange();
                        break;
                    case "4":
                        searchByRoom();
                        break;
                    case "5":
                        displayReportMenu();
                        break;
                    case "0":
                        exit = true;
                        break;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
                if (!exit && !choice.equals("5")) {
                    InputHelper.pressEnterToContinue(scanner);
                }
            }
        } catch (EndOfInputException exception) {
            // EOF behaves like selecting Back.
        }
    }

    private void displayMenu() {
        System.out.println();
        Logo.display();
        System.out.println(ConsoleStyle.menu(ConsoleStyle.menuBox("HOUSEKEEPING MENU",
                "section|TASK MANAGEMENT",
                "1|Add Cleaning Task",
                "2|Update Room Cleaning Status",
                "3|Roll Back Last Status Change",
                "4|Search Task by Room",
                "section|REPORTING",
                "5|View Reports",
                "0|Back to Main Menu")));
    }

    private void addCleaningTask() {
        System.out.println("\n--- Add Cleaning Task ---");
        String roomNumber = promptRoomNumber();

        if (housekeepingControl.hasActiveTaskForRoom(roomNumber)) {
            System.out.println("An active cleaning task already exists for room " + roomNumber + ".");
            return;
        }

        String remarks = promptText("Remarks: ");

        String taskDetails = ConsoleProgress.run(
                () -> housekeepingControl.addTaskAndGetDetails(roomNumber, remarks),
                "Processing cleaning task...",
                "Creating task record...",
                "Saving task log...");

        if (taskDetails == null) {
            ConsoleAnimation.error("Unable to add cleaning task.");
        } else {
            ConsoleAnimation.success("Cleaning task added.");
            System.out.println(taskDetails);
        }
    }

    private void updateCleaningStatus() {
        System.out.println("\n--- Update Cleaning Status ---");
        String taskId = promptRequiredText("Task ID: ");
        String taskDetails = housekeepingControl.getTaskDetails(taskId);

        if (taskDetails == null) {
            System.out.println("Task not found.");
            return;
        }

        System.out.println(taskDetails);
        String newStatus = promptStatus();

        boolean updated = ConsoleProgress.run(
                () -> housekeepingControl.updateTaskStatus(taskId, newStatus),
                "Processing status update...",
                "Updating room cleaning status...",
                "Saving task log...");
        if (updated) {
            ConsoleAnimation.success("Status updated.");
            System.out.println(housekeepingControl.getTaskDetails(taskId));
        } else {
            ConsoleAnimation.error("Status update failed.");
        }
    }

    private void rollbackLastChange() {
        System.out.println("\n--- Roll Back Last Status Change ---");
        String statusChangeSummary = ConsoleProgress.run(
                housekeepingControl::rollbackLastChangeSummary,
                "Restoring previous task status...",
                "Updating room status...",
                "Saving task log...");

        if (statusChangeSummary == null) {
            ConsoleAnimation.error("No change is available to roll back.");
            return;
        }

        ConsoleAnimation.success(statusChangeSummary);
    }

    private void searchByRoom() {
        System.out.println("\n--- Search Task by Room ---");
        String roomNumber = promptRoomNumber();
        System.out.println(ConsoleAnimation.runWithSpinner(
                () -> housekeepingControl.getTasksByRoomDisplay(roomNumber),
                "Searching housekeeping tasks"));
    }

    private void displayReportMenu() {
        boolean back = false;

        while (!back) {
            InputHelper.clearScreen();
            Logo.display();
            System.out.println(ConsoleStyle.menu(ConsoleStyle.menuBox("HOUSEKEEPING REPORTS",
                    "1|Task Status Summary",
                    "2|List All Tasks",
                    "3|Filter Tasks by Created Date Range",
                    "0|Back")));
            String choice = InputHelper.inputString(
                    scanner, "Select an option [0-3]: ").trim();

            switch (choice) {
                case "1":
                    displayStatusSummaryReport();
                    break;
                case "2":
                    System.out.println(ConsoleProgress.run(
                            housekeepingControl::getAllTasksDisplay,
                            "Fetching housekeeping information...",
                            "Loading task records...",
                            "Preparing results..."));
                    break;
                case "3":
                    filterTasksByCreatedDateRange();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
            if (!back) {
                InputHelper.pressEnterToContinue(scanner);
            }
        }
    }

    private void displayStatusSummaryReport() {
        String report = ConsoleProgress.run(housekeepingControl::getTaskStatusSummaryReport,
                "Fetching housekeeping information...",
                "Calculating task status totals...",
                "Preparing report...");

        System.out.println(report);
        offerPdfExport("Task Status Summary Report", report);
    }

    private void filterTasksByCreatedDateRange() {
        System.out.println("\n--- Filter Tasks by Created Date Range ---");
        LocalDate startDate = promptDate("Start date (yyyy-MM-dd): ");
        LocalDate endDate;

        do {
            endDate = promptDate("End date (yyyy-MM-dd): ");

            if (endDate.isBefore(startDate)) {
                System.out.println("End date cannot be before the start date.");
            }
        } while (endDate.isBefore(startDate));

        final LocalDate selectedEndDate = endDate;
        String report = ConsoleProgress.run(
                () -> housekeepingControl.getTasksCreatedBetweenReport(startDate, selectedEndDate),
                "Fetching housekeeping information...",
                "Filtering tasks by date range...",
                "Preparing results...");
        System.out.println(report);
        offerPdfExport("Tasks Created " + startDate + " to " + endDate, report);
    }

    private void offerPdfExport(String title, String report) {
        String selection = InputHelper.inputString(scanner,
                "Generate chart PDF and open it? (Y/N): ").trim();
        if (!selection.equalsIgnoreCase("Y") && !selection.equalsIgnoreCase("Yes")) {
            return;
        }

        try {
            Path pdfPath = housekeepingControl.exportReport(title, report);
            System.out.println("PDF generated: " + pdfPath);
            if (!housekeepingControl.openReport(pdfPath)) {
                System.out.println("Open the PDF manually from the path shown above.");
            }
        } catch (IOException exception) {
            System.out.println("Unable to generate or open PDF: " + exception.getMessage());
        }
    }

    private String promptRoomNumber() {
        while (true) {
            String roomNumber = InputHelper.inputString(scanner, "Room number: ").trim();

            if (housekeepingControl.isValidRoomNumber(roomNumber)) {
                if (housekeepingControl.roomExists(roomNumber)) {
                    return roomNumber;
                }
                System.out.println("Room number does not exist.");
            } else {
                System.out.println("Invalid room number. Use letters, numbers, or hyphen only.");
            }
        }
    }

    private String promptRequiredText(String prompt) {
        while (true) {
            String value = InputHelper.inputString(scanner, prompt).trim();

            if (housekeepingControl.isNonBlank(value)) {
                return value;
            }

            System.out.println("This field is required.");
        }
    }

    private String promptText(String prompt) {
        String value = InputHelper.inputString(scanner, prompt).trim();

        if (!housekeepingControl.isNonBlank(value)) {
            value = "-";
        }

        return value;
    }

    private LocalDate promptDate(String prompt) {
        while (true) {
            String input = InputHelper.inputString(scanner, prompt).trim();

            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException ex) {
                System.out.println("Use yyyy-MM-dd format. Example: 2026-08-25");
            }
        }
    }

    private String promptStatus() {
        while (true) {
            System.out.println(ConsoleStyle.menu(ConsoleStyle.menuBox("CLEANING STATUS",
                    "1|Dirty", "2|Cleaning In Progress", "3|Inspected",
                    "4|Ready for Check-In")));
            String choice = InputHelper.inputString(
                    scanner, "Select status [1-4]: ").trim();

            switch (choice) {
                case "1":
                    return "DIRTY";
                case "2":
                    return "CLEANING_IN_PROGRESS";
                case "3":
                    return "INSPECTED";
                case "4":
                    return "READY_FOR_CHECK_IN";
                default:
                    System.out.println("Invalid status. Please try again.");
            }
        }
    }

    public static void main(String[] args) {
        new HousekeepingUI(new Scanner(System.in)).start();
    }
}
