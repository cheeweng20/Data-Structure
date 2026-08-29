package HousekeepingAndTaskLog.control;

import HousekeepingAndTaskLog.dao.HousekeepingTaskDAO;
import HousekeepingAndTaskLog.entity.HousekeepingTask;
import HousekeepingAndTaskLog.entity.StatusChange;
import HousekeepingAndTaskLog.entity.TaskStatus;
import HousekeepingAndTaskLog.reporting.HousekeepingReportFormatter;
import HousekeepingAndTaskLog.reporting.ReportPdfExporter;
import FrontDeskService.dao.LateCheckoutExtensionDAO;
import VIPPriorityRoomAllocation.dao.ReservationDAO;
import VIPPriorityRoomAllocation.dao.RoomDAO;
import VIPPriorityRoomAllocation.entity.Reservation;
import VIPPriorityRoomAllocation.entity.ReservationStatus;
import VIPPriorityRoomAllocation.entity.Room;
import VIPPriorityRoomAllocation.entity.Room.RoomStatus;
import adt.ArrayList;
import adt.ArrayStack;
import adt.ListInterface;
import adt.StackInterface;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import common.utility.Validation;

/**
 * @author Zhe Sheng
 */

public class HousekeepingControl {

    private static final String CHECKED_OUT_REMARK_PREFIX = "Checked-out reservation: ";
    private final HousekeepingTaskDAO housekeepingTaskDAO;
    private final RoomDAO roomDAO;
    private final ReservationDAO reservationDAO;
    private final LateCheckoutExtensionDAO lateCheckoutExtensionDAO;
    private final ListInterface<HousekeepingTask> tasks;
    private final ListInterface<Room> rooms;
    private final ListInterface<Reservation> reservations;
    private final StackInterface<StatusChange> rollbackLog;

    public HousekeepingControl() {
        this(true);
    }

    public HousekeepingControl(boolean createCheckedOutReservationTasks) {
        this(new HousekeepingTaskDAO(), new RoomDAO(), new ReservationDAO(),
                createCheckedOutReservationTasks);
    }

    public HousekeepingControl(HousekeepingTaskDAO housekeepingTaskDAO, RoomDAO roomDAO) {
        this(housekeepingTaskDAO, roomDAO, new ReservationDAO(), true);
    }

    public HousekeepingControl(HousekeepingTaskDAO housekeepingTaskDAO, RoomDAO roomDAO,
            ReservationDAO reservationDAO) {
        this(housekeepingTaskDAO, roomDAO, reservationDAO, true);
    }

    public HousekeepingControl(HousekeepingTaskDAO housekeepingTaskDAO, RoomDAO roomDAO,
            ReservationDAO reservationDAO, boolean createCheckedOutReservationTasks) {
        if (housekeepingTaskDAO == null || roomDAO == null || reservationDAO == null) {
            throw new IllegalArgumentException("Housekeeping, room, and reservation data access objects are required.");
        }

        this.housekeepingTaskDAO = housekeepingTaskDAO;
        this.roomDAO = roomDAO;
        this.reservationDAO = reservationDAO;
        lateCheckoutExtensionDAO = new LateCheckoutExtensionDAO();
        tasks = housekeepingTaskDAO.retrieveFromFile();
        rooms = roomDAO.retrieveFromFile();
        reservations = reservationDAO.retrieveFromFile();
        rollbackLog = new ArrayStack<>();

        if (createCheckedOutReservationTasks) {
            createTasksForCheckedOutReservations();
        }
    }

    public HousekeepingTask addTask(String roomNumber, String remarks) {
        if (!roomExists(roomNumber) || hasActiveTaskForRoom(roomNumber)) {
            return null;
        }

        HousekeepingTask task = new HousekeepingTask(generateTaskId(), roomNumber,
                TaskStatus.DIRTY, LocalDateTime.now(), null, remarks);
        tasks.add(task);
        saveData();
        return task;
    }

    public boolean updateTaskStatus(String taskId, TaskStatus newStatus) {
        HousekeepingTask task = findTaskById(taskId);

        if (task == null || task.getStatus() == newStatus) {
            return false;
        }

        Room room = findRoomByNumber(task.getRoomNumber());
        RoomStatus previousRoomStatus = room == null ? null : room.getStatus();

        rollbackLog.push(new StatusChange(taskId, task.getStatus(), newStatus,
                task.getCompletedAtValue(), previousRoomStatus, LocalDateTime.now()));
        task.setStatus(newStatus);
        task.setCompletedAt(newStatus == TaskStatus.READY_FOR_CHECK_IN
                ? LocalDateTime.now() : null);

        if (room != null && newStatus == TaskStatus.READY_FOR_CHECK_IN
                && room.getStatus() == RoomStatus.NEEDS_CLEANING) {
            room.setStatus(RoomStatus.AVAILABLE);
            roomDAO.saveToFile(rooms);
        }

        saveData();
        return true;
    }

    public StatusChange rollbackLastChange() {
        if (rollbackLog.isEmpty()) {
            return null;
        }

        StatusChange lastChange = rollbackLog.pop();
        HousekeepingTask task = findTaskById(lastChange.getTaskId());

        if (task != null) {
            task.setStatus(lastChange.getPreviousStatus());
            task.setCompletedAt(lastChange.getPreviousCompletedAt());

            Room room = findRoomByNumber(task.getRoomNumber());
            if (room != null
                    && lastChange.getPreviousRoomStatus() == RoomStatus.NEEDS_CLEANING) {
                room.setStatus(RoomStatus.NEEDS_CLEANING);
                roomDAO.saveToFile(rooms);
            }

            saveData();
        }

        return lastChange;
    }

    public HousekeepingTask findTaskById(String taskId) {
        Iterator<HousekeepingTask> iterator = tasks.iterator();

        while (iterator.hasNext()) {
            HousekeepingTask task = iterator.next();

            if (task.getTaskId().equalsIgnoreCase(taskId)) {
                return task;
            }
        }

        return null;
    }

    public ListInterface<HousekeepingTask> searchByRoom(String roomNumber) {
        ListInterface<HousekeepingTask> result = new ArrayList<>();
        Iterator<HousekeepingTask> iterator = tasks.iterator();

        while (iterator.hasNext()) {
            HousekeepingTask task = iterator.next();

            if (task.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                result.add(task);
            }
        }

        return result;
    }

    /**
     * Rollback the automatic task created for a reservation when Front Desk
     * records a late checkout before the guest has actually checked out.
     */
    public boolean removeAutoTaskForReservation(String roomNumber, String confirmationNumber) {
        if (roomNumber == null || confirmationNumber == null) {
            return false;
        }

        for (int position = 1; position <= tasks.getNumberOfEntries(); position++) {
            HousekeepingTask task = tasks.getEntry(position);

            if (task.getRoomNumber().equalsIgnoreCase(roomNumber)
                    && isTaskForReservation(task, confirmationNumber)) {
                tasks.remove(position);
                saveData();
                return true;
            }
        }

        return false;
    }

    public boolean roomExists(String roomNumber) {
        return findRoomByNumber(roomNumber) != null;
    }

    /**
     * A room may have only one unfinished cleaning task. A task becomes
     * inactive only after the room is ready for check-in.
     */
    public boolean hasActiveTaskForRoom(String roomNumber) {
        Iterator<HousekeepingTask> iterator = tasks.iterator();

        while (iterator.hasNext()) {
            HousekeepingTask task = iterator.next();

            if (task.getRoomNumber().equalsIgnoreCase(roomNumber)
                    && task.getStatus() != TaskStatus.READY_FOR_CHECK_IN) {
                return true;
            }
        }

        return false;
    }

    public ListInterface<HousekeepingTask> filterByStatus(TaskStatus status) {
        ListInterface<HousekeepingTask> result = new ArrayList<>();
        Iterator<HousekeepingTask> iterator = tasks.iterator();

        while (iterator.hasNext()) {
            HousekeepingTask task = iterator.next();

            if (task.getStatus() == status) {
                result.add(task);
            }
        }

        return result;
    }

    public ListInterface<HousekeepingTask> filterTasksByCreatedDateRange(
            LocalDate startDate, LocalDate endDate) {
        ListInterface<HousekeepingTask> result = new ArrayList<>();

        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            return result;
        }

        Iterator<HousekeepingTask> iterator = tasks.iterator();

        while (iterator.hasNext()) {
            HousekeepingTask task = iterator.next();

            LocalDate taskCreatedDate = task.getCreatedAtValue().toLocalDate();

            if (!taskCreatedDate.isBefore(startDate) && !taskCreatedDate.isAfter(endDate)) {
                result.add(task);
            }
        }

        return result;
    }

    public int countByStatus(TaskStatus status) {
        return filterByStatus(status).getNumberOfEntries();
    }

    public ListInterface<HousekeepingTask> getTasks() {
        return tasks;
    }

    public void saveData() {
        housekeepingTaskDAO.saveToFile(tasks);
    }

    private void createTasksForCheckedOutReservations() {
        boolean taskCreated = false;
        Iterator<Reservation> iterator = reservations.iterator();

        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();

            if (isCleaningRequired(reservation)
                    && !hasTaskForReservation(reservation.getConfirmationNumber())) {
                HousekeepingTask task = addTask(reservation.getAssignedRoom().getRoomNumber(),
                        CHECKED_OUT_REMARK_PREFIX + reservation.getConfirmationNumber());
                if (task != null) {
                    taskCreated = true;
                }
            }
        }

        if (taskCreated) {
            saveData();
        }
    }

    private boolean isCleaningRequired(Reservation reservation) {
        return reservation != null
                && reservation.getCheckOutDate().equals(LocalDate.now())
                && reservation.getAssignedRoom() != null
                && lateCheckoutExtensionDAO.findByConfirmationNumber(
                        reservation.getConfirmationNumber()) == null;
    }

    private boolean hasTaskForReservation(String confirmationNumber) {
        Iterator<HousekeepingTask> iterator = tasks.iterator();

        while (iterator.hasNext()) {
            HousekeepingTask task = iterator.next();

            if (isTaskForReservation(task, confirmationNumber)) {
                return true;
            }
        }

        return false;
    }

    private boolean isTaskForReservation(HousekeepingTask task, String confirmationNumber) {
        if (task == null || task.getRemarks() == null
                || confirmationNumber == null || confirmationNumber.trim().isEmpty()) {
            return false;
        }

        String normalizedConfirmationNumber = confirmationNumber.trim();
        return task.getRemarks().equals(CHECKED_OUT_REMARK_PREFIX + normalizedConfirmationNumber);
    }

    private Room findRoomByNumber(String roomNumber) {
        Iterator<Room> iterator = rooms.iterator();

        while (iterator.hasNext()) {
            Room room = iterator.next();

            if (room.getRoomNumber().equalsIgnoreCase(roomNumber)) {
                return room;
            }
        }

        return null;
    }

    private String generateTaskId() {
        int largestNumber = 1000;
        Iterator<HousekeepingTask> iterator = tasks.iterator();

        while (iterator.hasNext()) {
            String taskId = iterator.next().getTaskId();

            if (taskId.length() > 2) {
                try {
                    int currentNumber = Integer.parseInt(taskId.substring(2));
                    if (currentNumber > largestNumber) {
                        largestNumber = currentNumber;
                    }
                } catch (NumberFormatException ex) {
                    // Ignore manual IDs that do not follow the HK number format.
                }
            }
        }

        return "HK" + (largestNumber + 1);
    }

    /**
     * Returns the existing task-detail presentation without exposing an entity
     * object to the console boundary.
     *
     * @param taskId task identifier
     * @return formatted task details, or {@code null} when the task is absent
     */
    public String getTaskDetails(String taskId) {
        HousekeepingTask task = findTaskById(taskId);
        return task == null ? null : formatTaskDetails(task);
    }

    /** Adds a task and returns the same details formerly rendered by the UI. */
    public String addTaskAndGetDetails(String roomNumber, String remarks) {
        HousekeepingTask task = addTask(roomNumber, remarks);
        return task == null ? null : formatTaskDetails(task);
    }

    /** Updates task status from its UI-safe name. */
    public boolean updateTaskStatus(String taskId, String statusName) {
        try {
            return updateTaskStatus(taskId, TaskStatus.valueOf(statusName));
        } catch (IllegalArgumentException | NullPointerException exception) {
            return false;
        }
    }

    /** Rolls back the latest change and returns its existing display message. */
    public String rollbackLastChangeSummary() {
        StatusChange statusChange = rollbackLastChange();
        if (statusChange == null) {
            return null;
        }
        return "Rolled back task " + statusChange.getTaskId()
                + " from " + statusChange.getNewStatus()
                + " to " + statusChange.getPreviousStatus() + "."
                + System.lineSeparator()
                + "Original change: " + statusChange.getChangedAt()
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")) + ".";
    }

    /** Returns the task-table display for all matching room records. */
    public String getTasksByRoomDisplay(String roomNumber) {
        return formatTaskTable(searchByRoom(roomNumber));
    }

    /** Returns the task-table display for all records. */
    public String getAllTasksDisplay() {
        return formatTaskTable(tasks);
    }

    /** Returns the existing status summary report text. */
    public String getTaskStatusSummaryReport() {
        int[] totals = new int[TaskStatus.values().length];
        for (TaskStatus status : TaskStatus.values()) {
            totals[status.ordinal()] = countByStatus(status);
        }
        return HousekeepingReportFormatter.buildTaskStatusSummary(totals);
    }

    /** Returns the existing date-range report text. */
    public String getTasksCreatedBetweenReport(LocalDate startDate, LocalDate endDate) {
        return HousekeepingReportFormatter.buildTaskListReport(
                filterTasksByCreatedDateRange(startDate, endDate));
    }

    public boolean isValidRoomNumber(String roomNumber) {
        return Validation.isValidRoomNumber(roomNumber);
    }

    public boolean isNonBlank(String value) {
        return Validation.isNonBlank(value);
    }

    public Path exportReport(String title, String report) throws IOException {
        return ReportPdfExporter.export(title, report);
    }

    public boolean openReport(Path pdfPath) throws IOException {
        return ReportPdfExporter.open(pdfPath);
    }

    private String formatTaskDetails(HousekeepingTask task) {
        return "\n--- Housekeeping Task Details ---" + System.lineSeparator()
                + "Task ID           : " + task.getTaskId() + System.lineSeparator()
                + "Room Number       : " + task.getRoomNumber() + System.lineSeparator()
                + "Status            : " + task.getStatus() + System.lineSeparator()
                + "Created At        : " + task.getCreatedAt() + System.lineSeparator()
                + "Completed At      : " + task.getCompletedAt() + System.lineSeparator()
                + "Remarks           : " + task.getRemarks();
    }

    private String formatTaskTable(ListInterface<HousekeepingTask> taskList) {
        if (taskList.isEmpty()) {
            return "No housekeeping task record found.";
        }
        StringBuilder result = new StringBuilder();
        String border = "+----------+--------------+------------------------+---------------------+---------------------+--------------------------------------------------+";
        result.append(border).append(System.lineSeparator());
        result.append(String.format("| %-8s | %-12s | %-22s | %-19s | %-19s | %-48s |%n",
                "Task ID", "Room", "Status", "Created At", "Completed At", "Remarks"));
        result.append(border).append(System.lineSeparator());
        for (HousekeepingTask task : taskList) {
            String remarks = task.getRemarks() == null ? "-" : task.getRemarks();
            result.append(String.format("| %-8s | %-12s | %-22s | %-19s | %-19s | %-48.48s |%n",
                    task.getTaskId(), task.getRoomNumber(), task.getStatus(),
                    task.getCreatedAt(), task.getCompletedAt(), remarks));
        }
        return result.append(border).toString();
    }
}
