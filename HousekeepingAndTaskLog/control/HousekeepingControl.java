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

public class HousekeepingControl {

    private static final String CHECKED_OUT_REMARK_PREFIX = "Checked-out reservation: ";
    private final HousekeepingTaskDAO housekeepingTaskDAO;
    private final RoomDAO roomDAO;
    private final ReservationDAO reservationDAO;
    private final ListInterface<HousekeepingTask> tasks;
    private final ListInterface<Room> rooms;
    private final ListInterface<Reservation> reservations;
    private final StackInterface<StatusChange> rollbackLog;

    public HousekeepingControl() {
        this(new HousekeepingTaskDAO(), new RoomDAO(), new ReservationDAO());
    }

    public HousekeepingControl(HousekeepingTaskDAO housekeepingTaskDAO, RoomDAO roomDAO) {
        this(housekeepingTaskDAO, roomDAO, new ReservationDAO());
    }

    public HousekeepingControl(HousekeepingTaskDAO housekeepingTaskDAO, RoomDAO roomDAO,
            ReservationDAO reservationDAO) {
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
        createTasksForCheckedOutReservations();
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
        return reservation.getStatus() == ReservationStatus.CHECKED_OUT
                && reservation.getAssignedRoom() != null
                && reservation.getAssignedRoom().getStatus() == RoomStatus.NEEDS_CLEANING;
    }

    private boolean hasTaskForReservation(String confirmationNumber) {
        Iterator<HousekeepingTask> iterator = tasks.iterator();
        String confirmationMarker = CHECKED_OUT_REMARK_PREFIX + confirmationNumber;

        while (iterator.hasNext()) {
            HousekeepingTask task = iterator.next();

            if (task.getRemarks() != null && task.getRemarks().equals(confirmationMarker)) {
                return true;
            }
        }

        return false;
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
