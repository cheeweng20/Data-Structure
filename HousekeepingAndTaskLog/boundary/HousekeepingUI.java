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
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void displayMenu() {
        System.out.println("\n--- Housekeeping and Task Log ---");
        System.out.println("1. Add Cleaning Task");
        System.out.println("2. Update Room Cleaning Status");
        System.out.println("3. Roll Back Last Status Change");
        System.out.println("4. Record Late Check-Out Delay");
        System.out.println("5. Search Task by Room");
        System.out.println("6. View Reports");
        System.out.println("0. Back");
        System.out.print("Select an option: ");
    }

    private void addCleaningTask() {
        System.out.println("\n--- Add Cleaning Task ---");
        String roomNumber = promptRoomNumber();
        String assignedStaff = promptRequiredText("Assigned staff: ");
        LocalDateTime expectedReadyAt = promptDateTime("Expected ready time (yyyy-MM-ddTHH:mm): ");
        String remarks = promptText("Remarks: ");

        HousekeepingTask task = housekeepingControl.addTask(roomNumber, assignedStaff,
                expectedReadyAt, remarks);

        System.out.println("Cleaning task added.");
        displayTaskDetails(task);
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
        String reason = promptText("Note: ");

        if (housekeepingControl.updateTaskStatus(taskId, newStatus, reason)) {
            System.out.println("Status updated.");
            displayTaskDetails(housekeepingControl.findTaskById(taskId));
        } else {
            System.out.println("Status update failed.");
        }
    }

    private void rollbackLastChange() {
        System.out.println("\n--- Roll Back Last Status Change ---");
        StatusChange statusChange = housekeepingControl.rollbackLastChange();

        if (statusChange == null) {
            System.out.println("No change is available to roll back.");
            return;
        }

        System.out.println("Rolled back task " + statusChange.getTaskId()
                + " from " + statusChange.getNewStatus()
                + " to " + statusChange.getPreviousStatus() + ".");
    }

    private void recordLateCheckout() {
        System.out.println("\n--- Record Late Check-Out Delay ---");
        String taskId = promptRequiredText("Task ID: ");
        HousekeepingTask task = housekeepingControl.findTaskById(taskId);

        if (task == null) {
            System.out.println("Task not found.");
            return;
        }

        displayTaskDetails(task);
        LocalDateTime newExpectedReadyAt = promptDateTime("New expected ready time (yyyy-MM-ddTHH:mm): ");
        String reason = promptRequiredText("Delay reason: ");

        if (housekeepingControl.recordLateCheckout(taskId, newExpectedReadyAt, reason)) {
            System.out.println("Late check-out delay recorded.");
            displayTaskDetails(housekeepingControl.findTaskById(taskId));
        } else {
            System.out.println("Late check-out update failed.");
        }
    }

    private void searchByRoom() {
        System.out.println("\n--- Search Task by Room ---");
        String roomNumber = promptRoomNumber();
        displayTasks(housekeepingControl.searchByRoom(roomNumber));
    }

    private void displayReportMenu() {
        boolean back = false;

        while (!back) {
            System.out.println("\n--- Housekeeping Reports ---");
            System.out.println("1. Task Status Summary");
            System.out.println("2. Overdue Cleaning Tasks");
            System.out.println("3. List All Tasks");
            System.out.println("0. Back");
            System.out.print("Select an option: ");
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
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void displayStatusSummaryReport() {
        System.out.println("\n--- Task Status Summary Report ---");
        System.out.printf("%-25s %8s%n", "Status", "Total");
        System.out.println("-----------------------------------");

        for (TaskStatus status : TaskStatus.values()) {
            System.out.printf("%-25s %8d%n", status, housekeepingControl.countByStatus(status));
        }
    }

    private String promptRoomNumber() {
        while (true) {
            System.out.print("Room number: ");
            String roomNumber = scanner.nextLine().trim();

            if (HousekeepingValidator.isValidRoomNumber(roomNumber)) {
                return roomNumber;
            }

            System.out.println("Invalid room number. Use letters, numbers, or hyphen only.");
        }
    }

    private String promptRequiredText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();

            if (HousekeepingValidator.isNonBlank(value)) {
                return value;
            }

            System.out.println("This field is required.");
        }
    }

    private String promptText(String prompt) {
        System.out.print(prompt);
        String value = scanner.nextLine().trim();

        if (HousekeepingValidator.isBlank(value)) {
            value = "-";
        }

        return value;
    }

    private LocalDateTime promptDateTime(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                LocalDateTime dateTime = LocalDateTime.parse(input);

                if (HousekeepingValidator.isFutureOrPresent(dateTime)) {
                    return dateTime;
                }

                System.out.println("Date and time cannot be in the past.");
            } catch (DateTimeParseException ex) {
                System.out.println("Use yyyy-MM-ddTHH:mm format. Example: 2026-07-30T14:30");
            }
        }
    }

    private TaskStatus promptStatus() {
        while (true) {
            System.out.println("\n--- Cleaning Status ---");
            System.out.println("1. Dirty");
            System.out.println("2. Cleaning In Progress");
            System.out.println("3. Inspected");
            System.out.println("4. Ready for Check-In");
            System.out.println("5. Blocked");
            System.out.print("Select status: ");
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
        System.out.println("Assigned Staff    : " + task.getAssignedStaff());
        System.out.println("Status            : " + task.getStatus());
        System.out.println("Created At        : " + task.getCreatedAt());
        System.out.println("Expected Ready At : " + task.getExpectedReadyAt());
        System.out.println("Remarks           : " + task.getRemarks());
    }

    private void printTableHeader() {
        printTableBorder();
        System.out.printf("| %-8s | %-6s | %-16s | %-22s | %-16s |%n",
                "Task ID", "Room", "Staff", "Status", "Ready Time");
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
        System.out.println("+----------+--------+------------------+------------------------+------------------+");
    }

    public static void main(String[] args) {
        new HousekeepingUI().start();
    }
}
