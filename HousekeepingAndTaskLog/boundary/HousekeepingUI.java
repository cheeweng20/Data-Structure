package HousekeepingAndTaskLog.boundary;

import HousekeepingAndTaskLog.control.HousekeepingControl;
import HousekeepingAndTaskLog.entity.HousekeepingTask;
import HousekeepingAndTaskLog.entity.StatusChange;
import HousekeepingAndTaskLog.entity.TaskStatus;
import HousekeepingAndTaskLog.utility.HousekeepingValidator;
import adt.ListInterface;
import common.src.Logo;
import common.src.ConsoleStyle;
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
        }
    }

    private void displayMenu() {
        System.out.println();
        Logo.displayHousekeepingAndTaskLog();
        System.out.println(ConsoleStyle.menu("\n+--------------------------------------------------------------+\n"
                + "|                    HOUSEKEEPING MENU                         |\n"
                + "+----+---------------------------------------------------------+\n"
                + "| 1  | Add Cleaning Task                                       |\n"
                + "| 2  | Update Room Cleaning Status                             |\n"
                + "| 3  | Roll Back Last Status Change                            |\n"
                + "| 4  | Search Task by Room                                     |\n"
                + "| 5  | View Reports                                            |\n"
                + "+----+---------------------------------------------------------+\n"
                + "| 0  | Back to Main Menu                                       |\n"
                + "+----+---------------------------------------------------------+"));
        System.out.print(ConsoleStyle.inputPrompt("Select an option [0-5]: "));
    }

    private void addCleaningTask() {
        System.out.println("\n--- Add Cleaning Task ---");
        String roomNumber = promptRoomNumber();
        String remarks = promptText("Remarks: ");

        HousekeepingTask task = housekeepingControl.addTask(roomNumber, remarks);

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
            System.out.println("2. List All Tasks");
            System.out.println("0. Back");
            System.out.print("Select an option: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    displayStatusSummaryReport();
                    break;
                case "2":
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
        System.out.println("Status            : " + task.getStatus());
        System.out.println("Created At        : " + task.getCreatedAt());
        System.out.println("Remarks           : " + task.getRemarks());
    }

    private void printTableHeader() {
        printTableBorder();
        System.out.printf("| %-8s | %-12s | %-22s | %-19s |%n",
                "Task ID", "Room", "Status", "Created At");
        printTableBorder();
    }

    private void printTaskLine(HousekeepingTask task) {
        System.out.printf("| %-8s | %-12s | %-22s | %-19s |%n",
                task.getTaskId(),
                task.getRoomNumber(),
                task.getStatus(),
                task.getCreatedAt());
    }

    private void printTableBorder() {
        System.out.println("+----------+--------------+------------------------+---------------------+");
    }

    public static void main(String[] args) {
        new HousekeepingUI().start();
    }
}
