package HousekeepingAndTaskLog.boundary;

import HousekeepingAndTaskLog.control.HousekeepingControl;
import HousekeepingAndTaskLog.entity.HousekeepingTask;
import HousekeepingAndTaskLog.entity.StatusChange;
import HousekeepingAndTaskLog.entity.TaskStatus;
import HousekeepingAndTaskLog.utility.HousekeepingValidator;
import adt.ListInterface;
import common.src.Logo;
import java.time.LocalDate;
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
        System.out.println("\n+--------------------------------------------------------------+");
        System.out.println("|                    HOUSEKEEPING MENU                         |");
        System.out.println("+----+---------------------------------------------------------+");
        System.out.println("| 1  | Add Cleaning Task                                       |");
        System.out.println("| 2  | Update Room Cleaning Status                             |");
        System.out.println("| 3  | Roll Back Last Status Change                            |");
        System.out.println("| 4  | Search Task by Room                                     |");
        System.out.println("| 5  | View Reports                                            |");
        System.out.println("+----+---------------------------------------------------------+");
        System.out.println("| 0  | Back to Main Menu                                       |");
        System.out.println("+----+---------------------------------------------------------+");
        System.out.print("Select an option [0-5]: ");
    }

    private void addCleaningTask() {
        System.out.println("\n--- Add Cleaning Task ---");
        String roomNumber = promptRoomNumber();

        if (housekeepingControl.hasActiveTaskForRoom(roomNumber)) {
            System.out.println("An active cleaning task already exists for room " + roomNumber + ".");
            return;
        }

        String remarks = promptText("Remarks: ");

        HousekeepingTask task = housekeepingControl.addTask(roomNumber, remarks);

        if (task == null) {
            System.out.println("Unable to add cleaning task.");
        } else {
            System.out.println("Cleaning task added.");
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

        if (housekeepingControl.updateTaskStatus(taskId, newStatus)) {
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
            System.out.println("\n+--------------------------------------------------------------+");
            System.out.println("|                     HOUSEKEEPING REPORTS                     |");
            System.out.println("+----+---------------------------------------------------------+");
            System.out.println("| 1  | Task Status Summary                                     |");
            System.out.println("| 2  | List All Tasks                                          |");
            System.out.println("| 3  | Filter Tasks by Created Date Range                      |");
            System.out.println("+----+---------------------------------------------------------+");
            System.out.println("| 0  | Back                                                    |");
            System.out.println("+----+---------------------------------------------------------+");
            System.out.print("Select an option [0-3]: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    displayStatusSummaryReport();
                    break;
                case "2":
                    displayTasks(housekeepingControl.getTasks());
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
        }
    }

    private void displayStatusSummaryReport() {
        System.out.println("\n+----------------------------+----------+");
        System.out.println("|      TASK STATUS SUMMARY REPORT       |");
        System.out.println("+----------------------------+----------+");
        System.out.printf("| %-26s | %8s |%n", "Status", "Total");
        System.out.println("+----------------------------+----------+");

        for (TaskStatus status : TaskStatus.values()) {
            System.out.printf("| %-26s | %8d |%n", status, housekeepingControl.countByStatus(status));
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

        displayTasks(housekeepingControl.filterTasksByCreatedDateRange(startDate, endDate));
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

    private LocalDate promptDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();

            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException ex) {
                System.out.println("Use yyyy-MM-dd format. Example: 2026-08-25");
            }
        }
    }

    private TaskStatus promptStatus() {
        while (true) {
            System.out.println("\n+----+----------------------------+");
            System.out.println("|          CLEANING STATUS        |");
            System.out.println("+----+----------------------------+");
            System.out.println("| 1  | Dirty                      |");
            System.out.println("| 2  | Cleaning In Progress       |");
            System.out.println("| 3  | Inspected                  |");
            System.out.println("| 4  | Ready for Check-In         |");
            System.out.println("+----+----------------------------+");
            System.out.print("Select status [1-4]: ");
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
        System.out.println("Remarks           : " + task.getRemarks());
    }

    private void printTableHeader() {
        printTableBorder();
        System.out.printf("| %-8s | %-12s | %-22s | %-19s | %-19s |%n",
                "Task ID", "Room", "Status", "Created At", "Completed At");
        printTableBorder();
    }

    private void printTaskLine(HousekeepingTask task) {
        System.out.printf("| %-8s | %-12s | %-22s | %-19s | %-19s |%n",
                task.getTaskId(),
                task.getRoomNumber(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getCompletedAt());
    }

    private void printTableBorder() {
        System.out.println("+----------+--------------+------------------------+---------------------+---------------------+");
    }

    public static void main(String[] args) {
        new HousekeepingUI().start();
    }
}
