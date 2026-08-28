package HousekeepingAndTaskLog.control;

import HousekeepingAndTaskLog.dao.HousekeepingTaskDAO;
import HousekeepingAndTaskLog.entity.HousekeepingTask;
import HousekeepingAndTaskLog.entity.StatusChange;
import HousekeepingAndTaskLog.entity.TaskStatus;
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
import java.util.Iterator;

/**
 * @author Zhe Sheng
 */

public class HousekeepingControl {

    private static final String CHECKED_OUT_REMARK_PREFIX = "Checked-out reservation: ";
    private static final String LATE_CHECKOUT_REMARK_PREFIX = "Late check-out | Confirmation: ";
    private final HousekeepingTaskDAO housekeepingTaskDAO;
    private final RoomDAO roomDAO;
    private final ReservationDAO reservationDAO;
    private final ListInterface<HousekeepingTask> tasks;
    private final ListInterface<Room> rooms;
    private final ListInterface<Reservation> reservations;
    private final StackInterface<StatusChange> rollbackLog;

    public HousekeepingControl() {
        this(true);
    }

    /**
     * Creates a housekeeping control instance using the normal data files.
     * Front Desk should pass {@code false} when it only needs to send a late
     * check-out notification; that prevents the notification path from also
     * generating catch-up tasks for historical checked-out reservations.
     */
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

    /**
     * Creates a control instance with explicit data access objects.
     *
     * @param createCheckedOutReservationTasks whether construction should
     * create missing cleaning tasks for already checked-out reservations
     */
    public HousekeepingControl(HousekeepingTaskDAO housekeepingTaskDAO, RoomDAO roomDAO,
            ReservationDAO reservationDAO, boolean createCheckedOutReservationTasks) {
        if (housekeepingTaskDAO == null || roomDAO == null || reservationDAO == null) {
            throw new IllegalArgumentException("Housekeeping, room, and reservation data access objects are required.");
        }

        this.housekeepingTaskDAO = housekeepingTaskDAO;
        this.roomDAO = roomDAO;
        this.reservationDAO = reservationDAO;
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

    /**
     * Notifies Housekeeping that an occupied room has been granted a late
     * check-out. The room's cleaning task is blocked until the guest leaves.
     * Repeated notifications for the same room and reservation update the
     * existing task instead of creating duplicates.
     *
     * @return the blocked task, or {@code null} when the supplied data is
     * invalid or the room already has a task for another active reservation
     */
    public HousekeepingTask notifyLateCheckout(String roomNumber, String confirmationNumber,
            String guestName, LocalDateTime extendedCheckOutAt,
            LocalDateTime expectedRoomReadyAt, String reason) {
        if (isBlank(roomNumber) || isBlank(confirmationNumber)
                || extendedCheckOutAt == null || expectedRoomReadyAt == null
                || expectedRoomReadyAt.isBefore(extendedCheckOutAt)) {
            return null;
        }

        String normalizedRoomNumber = roomNumber.trim();
        String normalizedConfirmationNumber = confirmationNumber.trim();

        if (!roomExists(normalizedRoomNumber)) {
            return null;
        }

        HousekeepingTask task = findTaskForReservation(normalizedRoomNumber,
                normalizedConfirmationNumber);

        if (task == null && findActiveTaskForRoom(normalizedRoomNumber) != null) {
            // A room cannot have two unfinished cleaning tasks. Do not replace
            // another reservation's active task with this notification.
            return null;
        }

        String lateCheckoutRemarks = buildLateCheckoutRemarks(normalizedConfirmationNumber,
                guestName, extendedCheckOutAt, expectedRoomReadyAt, reason);

        if (task == null) {
            task = new HousekeepingTask(generateTaskId(), normalizedRoomNumber,
                    TaskStatus.BLOCKED, LocalDateTime.now(), null,
                    expectedRoomReadyAt, lateCheckoutRemarks);
            tasks.add(task);
        } else {
            task.setStatus(TaskStatus.BLOCKED);
            task.setCompletedAt(null);
            task.setExpectedReadyAt(expectedRoomReadyAt);
            task.setRemarks(lateCheckoutRemarks);
        }

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
        return reservation.getCheckOutDate().equals(LocalDate.now())
                && reservation.getAssignedRoom() != null;
                //&& reservation.getAssignedRoom().getStatus() == RoomStatus.NEEDS_CLEANING;
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

    private HousekeepingTask findTaskForReservation(String roomNumber,
            String confirmationNumber) {
        Iterator<HousekeepingTask> iterator = tasks.iterator();

        while (iterator.hasNext()) {
            HousekeepingTask task = iterator.next();

            if (task.getRoomNumber().equalsIgnoreCase(roomNumber)
                    && isTaskForReservation(task, confirmationNumber)) {
                return task;
            }
        }

        return null;
    }

    private HousekeepingTask findActiveTaskForRoom(String roomNumber) {
        Iterator<HousekeepingTask> iterator = tasks.iterator();

        while (iterator.hasNext()) {
            HousekeepingTask task = iterator.next();

            if (task.getRoomNumber().equalsIgnoreCase(roomNumber)
                    && task.getStatus() != TaskStatus.READY_FOR_CHECK_IN) {
                return task;
            }
        }

        return null;
    }

    private boolean isTaskForReservation(HousekeepingTask task, String confirmationNumber) {
        if (task == null || task.getRemarks() == null || isBlank(confirmationNumber)) {
            return false;
        }

        String normalizedConfirmationNumber = confirmationNumber.trim();
        return task.getRemarks().equals(CHECKED_OUT_REMARK_PREFIX + normalizedConfirmationNumber)
                || task.getRemarks().startsWith(LATE_CHECKOUT_REMARK_PREFIX
                        + normalizedConfirmationNumber + " | ");
    }

    private String buildLateCheckoutRemarks(String confirmationNumber, String guestName,
            LocalDateTime extendedCheckOutAt, LocalDateTime expectedRoomReadyAt,
            String reason) {
        return LATE_CHECKOUT_REMARK_PREFIX + confirmationNumber
                + " | Guest: " + displayValue(guestName)
                + " | Extended check-out: " + extendedCheckOutAt
                + " | Expected room-ready: " + expectedRoomReadyAt
                + " | Reason: " + displayValue(reason);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String displayValue(String value) {
        return isBlank(value) ? "-" : value.trim().replace("\r", " ").replace("\n", " ");
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
}
