package HousekeepingAndTaskLog.boundary;

import HousekeepingAndTaskLog.control.HousekeepingControl;
import HousekeepingAndTaskLog.entity.HousekeepingTask;
import HousekeepingAndTaskLog.entity.StatusChange;
import HousekeepingAndTaskLog.entity.TaskStatus;
import adt.ListInterface;
import common.src.Logo;
import common.src.ConsoleStyle;
import common.src.ConsoleProgress;
import common.src.ConsoleAnimation;
import common.src.InputHelper;
import common.src.InputHelper.EndOfInputException;
import common.utility.Validation;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Scanner;

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
        Logo.displayHousekeepingAndTaskLog();
        System.out.println(ConsoleStyle.menu(ConsoleStyle.menuBox("HOUSEKEEPING MENU",
                "1|Add Cleaning Task",
                "2|Update Room Cleaning Status",
                "3|Roll Back Last Status Change",
                "4|Search Task by Room",
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

        HousekeepingTask task = ConsoleProgress.run(
                () -> housekeepingControl.addTask(roomNumber, remarks),
                "Processing cleaning task...",
                "Creating task record...",
                "Saving task log...");

        if (task == null) {
            ConsoleAnimation.error("Unable to add cleaning task.");
        } else {
            ConsoleAnimation.success("Cleaning task added.");
            displayTaskDetails(task);
        }
    }

    private void updateCleaningStatus() {
        System.out.println("\n--- Update Cleaning Status ---");
        String taskId = promptRequiredText("Task ID: ");
        HousekeepingTask task = housekeepingControl.findTaskById(taskId);

        if (task == null) {
            System.out.println("Task not found.");
            return;
        }

        displayTaskDetails(task);
        TaskStatus newStatus = promptStatus();

        boolean updated = ConsoleProgress.run(
                () -> housekeepingControl.updateTaskStatus(taskId, newStatus),
                "Processing status update...",
                "Updating room cleaning status...",
                "Saving task log...");
        if (updated) {
            ConsoleAnimation.success("Status updated.");
            displayTaskDetails(housekeepingControl.findTaskById(taskId));
        } else {
            ConsoleAnimation.error("Status update failed.");
        }
    }

    private void rollbackLastChange() {
        System.out.println("\n--- Roll Back Last Status Change ---");
        StatusChange statusChange = ConsoleProgress.run(
                housekeepingControl::rollbackLastChange,
                "Restoring previous task status...",
                "Updating room status...",
                "Saving task log...");

        if (statusChange == null) {
            ConsoleAnimation.error("No change is available to roll back.");
            return;
        }

        ConsoleAnimation.success("Rolled back task " + statusChange.getTaskId()
                + " from " + statusChange.getNewStatus()
                + " to " + statusChange.getPreviousStatus() + ".");
        System.out.println("Original change: " + statusChange.getChangedAt()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + ".");
    }

    private void searchByRoom() {
        System.out.println("\n--- Search Task by Room ---");
        String roomNumber = promptRoomNumber();
        displayTasks(ConsoleAnimation.runWithSpinner(
                () -> housekeepingControl.searchByRoom(roomNumber),
                "Searching housekeeping tasks"));
    }

    private void displayReportMenu() {
        boolean back = false;

        while (!back) {
            InputHelper.clearScreen();
            Logo.displayHousekeepingAndTaskLog();
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
                    displayTasks(ConsoleProgress.run(
                            housekeepingControl::getTasks,
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
        int[] totals = ConsoleProgress.run(() -> {
            int[] result = new int[TaskStatus.values().length];
            for (TaskStatus status : TaskStatus.values()) {
                result[status.ordinal()] = housekeepingControl.countByStatus(status);
            }
            return result;
        },
                "Fetching housekeeping information...",
                "Calculating task status totals...",
                "Preparing report...");

        System.out.println("\n+----------------------------+----------+");
        System.out.println("|      TASK STATUS SUMMARY REPORT       |");
        System.out.println("+----------------------------+----------+");
        System.out.printf("| %-26s | %8s |%n", "Status", "Total");
        System.out.println("+----------------------------+----------+");

        for (TaskStatus status : TaskStatus.values()) {
            System.out.printf("| %-26s | %8d |%n", status, totals[status.ordinal()]);
        }

        System.out.println("+----------------------------+----------+");
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
        displayTasks(ConsoleProgress.run(
                () -> housekeepingControl.filterTasksByCreatedDateRange(startDate, selectedEndDate),
                "Fetching housekeeping information...",
                "Filtering tasks by date range...",
                "Preparing results..."));
    }

    private String promptRoomNumber() {
        while (true) {
            String roomNumber = InputHelper.inputString(scanner, "Room number: ").trim();

            if (Validation.isValidRoomNumber(roomNumber)) {
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

            if (Validation.isNonBlank(value)) {
                return value;
            }

            System.out.println("This field is required.");
        }
    }

    private String promptText(String prompt) {
        String value = InputHelper.inputString(scanner, prompt).trim();

        if (Validation.isBlank(value)) {
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

    private TaskStatus promptStatus() {
        while (true) {
            System.out.println(ConsoleStyle.menu(ConsoleStyle.menuBox("CLEANING STATUS",
                    "1|Dirty", "2|Cleaning In Progress", "3|Inspected",
                    "4|Ready for Check-In")));
            String choice = InputHelper.inputString(
                    scanner, "Select status [1-4]: ").trim();

            switch (choice) {
                case "1":
                    return TaskStatus.DIRTY;
                case "2":
                    return TaskStatus.CLEANING_IN_PROGRESS;
                case "3":
                    return TaskStatus.INSPECTED;
                case "4":
                    return TaskStatus.READY_FOR_CHECK_IN;
                default:
                    System.out.println("Invalid status. Please try again.");
            }
        }
    }

    private void displayTasks(ListInterface<HousekeepingTask> tasks) {
        if (tasks.isEmpty()) {
            System.out.println("No housekeeping task record found.");
            return;
        }

        printTableHeader();
        Iterator<HousekeepingTask> iterator = tasks.iterator();

        while (iterator.hasNext()) {
            printTaskLine(iterator.next());
        }

        printTableBorder();
    }

    private void displayTaskDetails(HousekeepingTask task) {
        System.out.println("\n--- Housekeeping Task Details ---");
        System.out.println("Task ID           : " + task.getTaskId());
        System.out.println("Room Number       : " + task.getRoomNumber());
        System.out.println("Status            : " + task.getStatus());
        System.out.println("Created At        : " + task.getCreatedAt());
        System.out.println("Completed At      : " + task.getCompletedAt());
        System.out.println("Expected Ready At : " + task.getExpectedReadyAt());
        System.out.println("Remarks           : " + task.getRemarks());
    }

    private void printTableHeader() {
        printTableBorder();
        System.out.printf("| %-8s | %-12s | %-22s | %-19s | %-19s | %-19s | %-48s |%n",
                "Task ID", "Room", "Status", "Created At", "Completed At", "Expected Ready",
                "Late Check-Out / Remarks");
        printTableBorder();
    }

    private void printTaskLine(HousekeepingTask task) {
        String remarks = task.getRemarks() == null ? "-" : task.getRemarks();
        System.out.printf("| %-8s | %-12s | %-22s | %-19s | %-19s | %-19s | %-48.48s |%n",
                task.getTaskId(),
                task.getRoomNumber(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getCompletedAt(),
                task.getExpectedReadyAt(),
                remarks);
    }

    private void printTableBorder() {
        System.out.println("+----------+--------------+------------------------+---------------------+---------------------+---------------------+--------------------------------------------------+");
    }

    public static void main(String[] args) {
        new HousekeepingUI(new Scanner(System.in)).start();
    }
}
