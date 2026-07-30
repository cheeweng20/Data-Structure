package HousekeepingAndTaskLog.control;

import HousekeepingAndTaskLog.dao.HousekeepingTaskDAO;
import HousekeepingAndTaskLog.entity.HousekeepingTask;
import HousekeepingAndTaskLog.entity.StatusChange;
import HousekeepingAndTaskLog.entity.TaskStatus;
import adt.ArrayList;
import adt.ListInterface;
import java.time.LocalDateTime;
import java.util.Iterator;

/**
 * @author Your Name
 */
public class HousekeepingControl {

    private final HousekeepingTaskDAO housekeepingTaskDAO;
    private final ListInterface<HousekeepingTask> tasks;
    private final ListInterface<StatusChange> rollbackLog;

    public HousekeepingControl() {
        housekeepingTaskDAO = new HousekeepingTaskDAO();
        tasks = housekeepingTaskDAO.retrieveFromFile();
        rollbackLog = new ArrayList<>();
    }

    public HousekeepingTask addTask(String roomNumber, String assignedStaff,
            LocalDateTime expectedReadyAt, int priority, String remarks) {
        HousekeepingTask task = new HousekeepingTask(generateTaskId(), roomNumber,
                assignedStaff, TaskStatus.DIRTY, LocalDateTime.now(), expectedReadyAt,
                priority, remarks);
        tasks.add(task);
        saveData();
        return task;
    }

    public boolean updateTaskStatus(String taskId, TaskStatus newStatus, String reason) {
        HousekeepingTask task = findTaskById(taskId);

        if (task == null || task.getStatus() == newStatus) {
            return false;
        }

        rollbackLog.add(new StatusChange(taskId, task.getStatus(), newStatus,
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

        rollbackLog.add(new StatusChange(taskId, task.getStatus(), TaskStatus.BLOCKED,
                task.getExpectedReadyAt(), LocalDateTime.now(), reason));
        task.setStatus(TaskStatus.BLOCKED);
        task.setExpectedReadyAt(newExpectedReadyAt);
        task.setRemarks(reason);
        saveData();
        return true;
    }

    public StatusChange rollbackLastChange() {
        if (rollbackLog.isEmpty()) {
            return null;
        }

        StatusChange lastChange = rollbackLog.remove(rollbackLog.getNumberOfEntries());
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

    public ListInterface<HousekeepingTask> getTasksSortedByPriority() {
        ListInterface<HousekeepingTask> sortedTasks = copyTasks();

        for (int i = 1; i < sortedTasks.getNumberOfEntries(); i++) {
            for (int j = 1; j <= sortedTasks.getNumberOfEntries() - i; j++) {
                HousekeepingTask current = sortedTasks.getEntry(j);
                HousekeepingTask next = sortedTasks.getEntry(j + 1);

                if (current.getPriority() > next.getPriority()) {
                    sortedTasks.replace(j, next);
                    sortedTasks.replace(j + 1, current);
                }
            }
        }

        return sortedTasks;
    }

    public ListInterface<HousekeepingTask> getTasksSortedByExpectedReadyTime() {
        ListInterface<HousekeepingTask> sortedTasks = copyTasks();

        for (int i = 1; i < sortedTasks.getNumberOfEntries(); i++) {
            for (int j = 1; j <= sortedTasks.getNumberOfEntries() - i; j++) {
                HousekeepingTask current = sortedTasks.getEntry(j);
                HousekeepingTask next = sortedTasks.getEntry(j + 1);

                if (current.getExpectedReadyAt().isAfter(next.getExpectedReadyAt())) {
                    sortedTasks.replace(j, next);
                    sortedTasks.replace(j + 1, current);
                }
            }
        }

        return sortedTasks;
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

    private ListInterface<HousekeepingTask> copyTasks() {
        ListInterface<HousekeepingTask> copiedTasks = new ArrayList<>();
        Iterator<HousekeepingTask> iterator = tasks.iterator();

        while (iterator.hasNext()) {
            copiedTasks.add(iterator.next());
        }

        return copiedTasks;
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
