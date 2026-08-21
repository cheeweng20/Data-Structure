package HousekeepingAndTaskLog.control;

import HousekeepingAndTaskLog.dao.HousekeepingTaskDAO;
import HousekeepingAndTaskLog.entity.HousekeepingTask;
import HousekeepingAndTaskLog.entity.StatusChange;
import HousekeepingAndTaskLog.entity.TaskStatus;
import VIPPriorityRoomAllocation.dao.RoomDAO;
import VIPPriorityRoomAllocation.entity.Room;
import adt.ArrayList;
import adt.ArrayStack;
import adt.ListInterface;
import adt.StackInterface;
import java.time.LocalDateTime;
import java.util.Iterator;

public class HousekeepingControl {

    private static final String LATE_CHECKOUT_REMARK_PREFIX = "Late Checkout | Confirmation: ";
    private final HousekeepingTaskDAO housekeepingTaskDAO;
    private final RoomDAO roomDAO;
    private final ListInterface<HousekeepingTask> tasks;
    private final ListInterface<Room> rooms;
    private final StackInterface<StatusChange> rollbackLog;

    public HousekeepingControl() {
        this(new HousekeepingTaskDAO(), new RoomDAO());
    }

    public HousekeepingControl(HousekeepingTaskDAO housekeepingTaskDAO, RoomDAO roomDAO) {
        if (housekeepingTaskDAO == null || roomDAO == null) {
            throw new IllegalArgumentException("Housekeeping task and room data access objects are required.");
        }

        this.housekeepingTaskDAO = housekeepingTaskDAO;
        this.roomDAO = roomDAO;
        tasks = housekeepingTaskDAO.retrieveFromFile();
        rooms = roomDAO.retrieveFromFile();
        rollbackLog = new ArrayStack<>();
    }

    public HousekeepingTask addTask(String roomNumber, String assignedStaff,
            LocalDateTime expectedReadyAt, String remarks) {
        if (!roomExists(roomNumber)) {
            return null;
        }

        HousekeepingTask task = new HousekeepingTask(generateTaskId(), roomNumber,
                assignedStaff, TaskStatus.DIRTY, LocalDateTime.now(), expectedReadyAt,
                remarks);
        tasks.add(task);
        saveData();
        return task;
    }

    public boolean updateTaskStatus(String taskId, TaskStatus newStatus, String reason) {
        HousekeepingTask task = findTaskById(taskId);

        if (task == null || task.getStatus() == newStatus) {
            return false;
        }

        rollbackLog.push(new StatusChange(taskId, task.getStatus(), newStatus,
                task.getExpectedReadyAt(), LocalDateTime.now(), reason));
        task.setStatus(newStatus);
        saveData();
        return true;
    }

    public boolean recordLateCheckout(String taskId, LocalDateTime newExpectedReadyAt, String reason) {
        HousekeepingTask task = findTaskById(taskId);

        if (task == null || newExpectedReadyAt.isBefore(task.getExpectedReadyAt())) {
            return false;
        }

        rollbackLog.push(new StatusChange(taskId, task.getStatus(), TaskStatus.BLOCKED,
                task.getExpectedReadyAt(), LocalDateTime.now(), reason));
        task.setStatus(TaskStatus.BLOCKED);
        task.setExpectedReadyAt(newExpectedReadyAt);
        task.setRemarks(reason);
        saveData();
        return true;
    }

    /**
     * Creates or refreshes the blocked housekeeping task for a guest who has
     * been granted a late checkout. The confirmation number in the remarks
     * makes repeated notifications for the same stay idempotent.
     *
     * @return the persisted task, or {@code null} when the notification data
     *         is invalid or the room does not exist
     */
    public HousekeepingTask notifyLateCheckout(String roomNumber, String confirmationNumber,
            String guestName, LocalDateTime extendedCheckOutAt,
            LocalDateTime expectedReadyAt, String reason) {
        String normalizedRoomNumber = normalizeRequiredText(roomNumber);
        String normalizedConfirmationNumber = normalizeRequiredText(confirmationNumber);
        String normalizedGuestName = normalizeRequiredText(guestName);
        String normalizedReason = normalizeRequiredText(reason);

        if (normalizedRoomNumber == null || normalizedConfirmationNumber == null
                || normalizedGuestName == null || normalizedReason == null
                || extendedCheckOutAt == null || expectedReadyAt == null
                || expectedReadyAt.isBefore(extendedCheckOutAt)
                || !roomExists(normalizedRoomNumber)) {
            return null;
        }

        HousekeepingTask task = findLateCheckoutTask(normalizedRoomNumber,
                normalizedConfirmationNumber);
        String remarks = buildLateCheckoutRemarks(normalizedConfirmationNumber,
                normalizedGuestName, extendedCheckOutAt, normalizedReason);

        if (task == null) {
            task = new HousekeepingTask(generateTaskId(), normalizedRoomNumber,
                    "Unassigned", TaskStatus.BLOCKED, LocalDateTime.now(), expectedReadyAt,
                    remarks);
            tasks.add(task);
        } else {
            task.setStatus(TaskStatus.BLOCKED);
            task.setExpectedReadyAt(expectedReadyAt);
            task.setRemarks(remarks);
        }

        saveData();
        return task;
    }

    public StatusChange rollbackLastChange() {
        if (rollbackLog.isEmpty()) {
            return null;
        }

        StatusChange lastChange = rollbackLog.pop();
        HousekeepingTask task = findTaskById(lastChange.getTaskId());

        if (task != null) {
            task.setStatus(lastChange.getPreviousStatus());
            task.setExpectedReadyAt(lastChange.getPreviousExpectedReadyAt());
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
        Iterator<Room> iterator = rooms.iterator();

        while (iterator.hasNext()) {
            if (iterator.next().getRoomNumber().equalsIgnoreCase(roomNumber)) {
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

    public int countByStatus(TaskStatus status) {
        return filterByStatus(status).getNumberOfEntries();
    }

    public ListInterface<HousekeepingTask> getOverdueTasks() {
        ListInterface<HousekeepingTask> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        Iterator<HousekeepingTask> iterator = tasks.iterator();

        while (iterator.hasNext()) {
            HousekeepingTask task = iterator.next();

            if (task.getExpectedReadyAt().isBefore(now)
                    && task.getStatus() != TaskStatus.READY_FOR_CHECK_IN) {
                result.add(task);
            }
        }

        return result;
    }

    public ListInterface<HousekeepingTask> getTasks() {
        return tasks;
    }

    public void saveData() {
        housekeepingTaskDAO.saveToFile(tasks);
    }

    private HousekeepingTask findLateCheckoutTask(String roomNumber,
            String confirmationNumber) {
        Iterator<HousekeepingTask> iterator = tasks.iterator();
        String confirmationMarker = LATE_CHECKOUT_REMARK_PREFIX
                + sanitizeRemarkValue(confirmationNumber) + " |";

        while (iterator.hasNext()) {
            HousekeepingTask task = iterator.next();

            if (task.getRoomNumber().equalsIgnoreCase(roomNumber)
                    && task.getRemarks() != null
                    && task.getRemarks().startsWith(confirmationMarker)) {
                return task;
            }
        }

        return null;
    }

    private String buildLateCheckoutRemarks(String confirmationNumber, String guestName,
            LocalDateTime extendedCheckOutAt, String reason) {
        return LATE_CHECKOUT_REMARK_PREFIX + sanitizeRemarkValue(confirmationNumber)
                + " | Guest: " + sanitizeRemarkValue(guestName)
                + " | Extended Checkout: " + extendedCheckOutAt
                + " | Reason: " + sanitizeRemarkValue(reason);
    }

    private String normalizeRequiredText(String value) {
        if (value == null) {
            return null;
        }

        String normalizedValue = value.trim();
        return normalizedValue.isEmpty() ? null : normalizedValue;
    }

    private String sanitizeRemarkValue(String value) {
        return value.replace("|", "/")
                .replace(",", ";")
                .replace('\r', ' ')
                .replace('\n', ' ');
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
