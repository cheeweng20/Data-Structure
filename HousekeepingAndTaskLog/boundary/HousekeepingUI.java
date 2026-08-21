package HousekeepingAndTaskLog.boundary;

import HousekeepingAndTaskLog.control.HousekeepingControl;
import HousekeepingAndTaskLog.entity.HousekeepingTask;
import HousekeepingAndTaskLog.entity.StatusChange;
import HousekeepingAndTaskLog.entity.TaskStatus;
import HousekeepingAndTaskLog.utility.HousekeepingValidator;
import adt.ListInterface;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Scanner;
import common.src.ConsoleStyle;

public class HousekeepingUI {

    private final Scanner scanner;
    private final HousekeepingControl housekeepingControl;

    public HousekeepingUI() {
        this(new Scanner(System.in));
    }

    public HousekeepingUI(Scanner scanner) {
        this.scanner = scanner;
        housekeepingControl = new HousekeepingControl();
    }

    public void start() {
        boolean exit = false;

        while (!exit) {
            displayMenu();
            String choice = scanner.nextLine().trim();

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
                    recordLateCheckout();
                    break;
                case "5":
                    searchByRoom();
                    break;
                case "6":
                    displayReportMenu();
                    break;
                case "0":
                    exit = true;
                    break;
                default:
                    printError("Invalid option. Please try again.");
            }
        }
    }

    private void displayMenu() {
        printTitle("Housekeeping and Task Log");
        System.out.println(ConsoleStyle.menu("1. Add Cleaning Task\n"
                + "2. Update Room Cleaning Status\n"
                + "3. Roll Back Last Status Change\n"
                + "4. Record Late Check-Out Delay\n"
                + "5. Search Task by Room\n"
                + "6. View Reports\n"
                + "0. Back"));
        printPrompt("Select an option: ");
    }

    private void addCleaningTask() {
        printTitle("Add Cleaning Task");
        String roomNumber = promptRoomNumber();
        String assignedStaff = promptRequiredText("Assigned staff: ");
        LocalDateTime expectedReadyAt = promptDateTime("Expected ready time (yyyy-MM-ddTHH:mm): ");
        String remarks = promptText("Remarks: ");

        HousekeepingTask task = housekeepingControl.addTask(roomNumber, assignedStaff,
                expectedReadyAt, remarks);

        printSuccess("Cleaning task added.");
        displayTaskDetails(task);
    }

    private void updateCleaningStatus() {
        printTitle("Update Cleaning Status");
        String taskId = promptRequiredText("Task ID: ");
        HousekeepingTask task = housekeepingControl.findTaskById(taskId);

        if (task == null) {
            printError("Task not found.");
            return;
        }

        displayTaskDetails(task);
        TaskStatus newStatus = promptStatus();
        String reason = promptText("Note: ");

        if (housekeepingControl.updateTaskStatus(taskId, newStatus, reason)) {
            printSuccess("Status updated.");
            displayTaskDetails(housekeepingControl.findTaskById(taskId));
        } else {
            printError("Status update failed.");
        }
    }

    private void rollbackLastChange() {
        printTitle("Roll Back Last Status Change");
        StatusChange statusChange = housekeepingControl.rollbackLastChange();

        if (statusChange == null) {
            System.out.println(ConsoleStyle.info("No change is available to roll back."));
            return;
        }

        printSuccess("Rolled back task " + statusChange.getTaskId()
                + " from " + statusChange.getNewStatus()
                + " to " + statusChange.getPreviousStatus() + ".");
    }

    private void recordLateCheckout() {
        printTitle("Record Late Check-Out Delay");
        String taskId = promptRequiredText("Task ID: ");
        HousekeepingTask task = housekeepingControl.findTaskById(taskId);

        if (task == null) {
            printError("Task not found.");
            return;
        }

        displayTaskDetails(task);
        LocalDateTime newExpectedReadyAt = promptDateTime("New expected ready time (yyyy-MM-ddTHH:mm): ");
        String reason = promptRequiredText("Delay reason: ");

        if (housekeepingControl.recordLateCheckout(taskId, newExpectedReadyAt, reason)) {
            printSuccess("Late check-out delay recorded.");
            displayTaskDetails(housekeepingControl.findTaskById(taskId));
        } else {
            printError("Late check-out update failed.");
        }
    }

    private void searchByRoom() {
        printTitle("Search Task by Room");
        String roomNumber = promptRoomNumber();
        displayTasks(housekeepingControl.searchByRoom(roomNumber));
    }

    private void displayReportMenu() {
        boolean back = false;

        while (!back) {
            printTitle("Housekeeping Reports");
            System.out.println(ConsoleStyle.menu("1. Task Status Summary\n"
                    + "2. Overdue Cleaning Tasks\n"
                    + "3. List All Tasks\n"
                    + "0. Back"));
            printPrompt("Select an option: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    displayStatusSummaryReport();
                    break;
                case "2":
                    displayTasks(housekeepingControl.getOverdueTasks());
                    break;
                case "3":
                    displayTasks(housekeepingControl.getTasks());
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    printError("Invalid option. Please try again.");
            }
        }
    }

    private void displayStatusSummaryReport() {
        printTitle("Task Status Summary Report");
        System.out.print(ConsoleStyle.tableHeader(String.format("%-25s %8s%n", "Status", "Total")));
        System.out.println(ConsoleStyle.tableBorder("-----------------------------------"));

        for (TaskStatus status : TaskStatus.values()) {
            System.out.printf("%-25s %8d%n", status, housekeepingControl.countByStatus(status));
        }
    }

    private String promptRoomNumber() {
        while (true) {
            printPrompt("Room number: ");
            String roomNumber = scanner.nextLine().trim();

            if (HousekeepingValidator.isValidRoomNumber(roomNumber)) {
                if (housekeepingControl.roomExists(roomNumber)) {
                    return roomNumber;
                }
                printError("Room number does not exist.");
            } else {
                printError("Invalid room number. Use letters, numbers, or hyphen only.");
            }
        }
    }

    private String promptRequiredText(String prompt) {
        while (true) {
            printPrompt(prompt);
            String value = scanner.nextLine().trim();

            if (HousekeepingValidator.isNonBlank(value)) {
                return value;
            }

            printError("This field is required.");
        }
    }

    private String promptText(String prompt) {
        printPrompt(prompt);
        String value = scanner.nextLine().trim();

        if (HousekeepingValidator.isBlank(value)) {
            value = "-";
        }

        return value;
    }

    private LocalDateTime promptDateTime(String prompt) {
        while (true) {
            printPrompt(prompt);
            String input = scanner.nextLine().trim();

            try {
                LocalDateTime dateTime = LocalDateTime.parse(input);

                if (HousekeepingValidator.isFutureOrPresent(dateTime)) {
                    return dateTime;
                }

                printError("Date and time cannot be in the past.");
            } catch (DateTimeParseException ex) {
                printError("Use yyyy-MM-ddTHH:mm format. Example: 2026-07-30T14:30");
            }
        }
    }

    private TaskStatus promptStatus() {
        while (true) {
            printTitle("Cleaning Status");
            System.out.println(ConsoleStyle.menu("1. Dirty\n"
                    + "2. Cleaning In Progress\n"
                    + "3. Inspected\n"
                    + "4. Ready for Check-In\n"
                    + "5. Blocked"));
            printPrompt("Select status: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    return TaskStatus.DIRTY;
                case "2":
                    return TaskStatus.CLEANING_IN_PROGRESS;
                case "3":
                    return TaskStatus.INSPECTED;
                case "4":
                    return TaskStatus.READY_FOR_CHECK_IN;
                case "5":
                    return TaskStatus.BLOCKED;
                default:
                    printError("Invalid status. Please try again.");
            }
        }
    }

    private void displayTasks(ListInterface<HousekeepingTask> tasks) {
        if (tasks.isEmpty()) {
            System.out.println(ConsoleStyle.info("No housekeeping task record found."));
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
        printTitle("Housekeeping Task Details");
        System.out.println("Task ID           : " + task.getTaskId());
        System.out.println("Room Number       : " + task.getRoomNumber());
        System.out.println("Assigned Staff    : " + task.getAssignedStaff());
        System.out.println("Status            : " + task.getStatus());
        System.out.println("Created At        : " + task.getCreatedAt());
        System.out.println("Expected Ready At : " + task.getExpectedReadyAt());
        System.out.println("Remarks           : " + task.getRemarks());
    }

    private void printTableHeader() {
        printTableBorder();
        System.out.print(ConsoleStyle.tableHeader(String.format(
                "| %-8s | %-6s | %-16s | %-22s | %-16s |%n",
                "Task ID", "Room", "Staff", "Status", "Ready Time")));
        printTableBorder();
    }

    private void printTaskLine(HousekeepingTask task) {
        System.out.printf("| %-8s | %-6s | %-16s | %-22s | %-16s |%n",
                task.getTaskId(),
                task.getRoomNumber(),
                task.getAssignedStaff(),
                task.getStatus(),
                task.getExpectedReadyAt());
    }

    private void printTableBorder() {
        System.out.println(ConsoleStyle.tableBorder(
                "+----------+--------+------------------+------------------------+------------------+"));
    }

    private void printTitle(String title) {
        System.out.println(ConsoleStyle.title("\n--- " + title + " ---"));
    }

    private void printPrompt(String prompt) {
        System.out.print(ConsoleStyle.prompt(prompt));
    }

    private void printSuccess(String message) {
        System.out.println(ConsoleStyle.successBadge() + " " + message);
    }

    private void printError(String message) {
        System.out.println(ConsoleStyle.failedBadge() + " " + message);
    }

    public static void main(String[] args) {
        new HousekeepingUI().start();
    }
}
