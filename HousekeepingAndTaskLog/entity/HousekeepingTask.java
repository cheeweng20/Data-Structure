package HousekeepingAndTaskLog.entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HousekeepingTask implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taskId;
    private String roomNumber;
    private TaskStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime expectedReadyAt;
    private String remarks;

    public HousekeepingTask(String taskId, String roomNumber, TaskStatus status,
            LocalDateTime createdAt, LocalDateTime completedAt, String remarks) {
        this(taskId, roomNumber, status, createdAt, completedAt, null, remarks);
    }

    public HousekeepingTask(String taskId, String roomNumber, TaskStatus status,
            LocalDateTime createdAt, LocalDateTime completedAt,
            LocalDateTime expectedReadyAt, String remarks) {
        this.taskId = taskId;
        this.roomNumber = roomNumber;
        this.status = status;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
        this.expectedReadyAt = expectedReadyAt;
        this.remarks = remarks;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }

    public LocalDateTime getCreatedAtValue() {
        return createdAt;
    }

    public LocalDateTime getCompletedAtValue() {
        return completedAt;
    }

    public String getCompletedAt() {
        return completedAt == null
                ? "-"
                : completedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    /**
     * Gets the estimated time at which this room can be released back to the
     * front desk after its cleaning task is complete.
     *
     * @return the expected ready time, or {@code null} when none is recorded
     */
    public LocalDateTime getExpectedReadyAtValue() {
        return expectedReadyAt;
    }

    public String getExpectedReadyAt() {
        return expectedReadyAt == null
                ? "-"
                : expectedReadyAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }

    public void setExpectedReadyAt(LocalDateTime expectedReadyAt) {
        this.expectedReadyAt = expectedReadyAt;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String toCsvLine() {
        return taskId + ","
                + roomNumber + ","
                + status + ","
                + createdAt + ","
                + (completedAt == null ? "" : completedAt) + ","
                + (expectedReadyAt == null ? "" : expectedReadyAt) + ","
                + remarks.replace(",", ";");
    }
}
