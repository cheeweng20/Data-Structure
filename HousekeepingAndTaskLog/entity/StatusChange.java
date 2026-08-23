package HousekeepingAndTaskLog.entity;

import java.time.LocalDateTime;

public class StatusChange {

    private String taskId;
    private TaskStatus previousStatus;
    private TaskStatus newStatus;
    private LocalDateTime changedAt;
    private String reason;

    public StatusChange(String taskId, TaskStatus previousStatus, TaskStatus newStatus,
            LocalDateTime changedAt, String reason) {
        this.taskId = taskId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.changedAt = changedAt;
        this.reason = reason;
    }

    public String getTaskId() {
        return taskId;
    }

    public TaskStatus getPreviousStatus() {
        return previousStatus;
    }

    public TaskStatus getNewStatus() {
        return newStatus;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public String getReason() {
        return reason;
    }
}
