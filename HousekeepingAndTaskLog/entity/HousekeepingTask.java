package HousekeepingAndTaskLog.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

public class HousekeepingTask implements Serializable {

    private static final long serialVersionUID = 1L;

    private String taskId;
    private String roomNumber;
    private String assignedStaff;
    private TaskStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expectedReadyAt;
    private String remarks;

    public HousekeepingTask(String taskId, String roomNumber, String assignedStaff,
            TaskStatus status, LocalDateTime createdAt, LocalDateTime expectedReadyAt,
            String remarks) {
        this.taskId = taskId;
        this.roomNumber = roomNumber;
        this.assignedStaff = assignedStaff;
        this.status = status;
        this.createdAt = createdAt;
        this.expectedReadyAt = expectedReadyAt;
        this.remarks = remarks;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public String getAssignedStaff() {
        return assignedStaff;
    }

    public void setAssignedStaff(String assignedStaff) {
        this.assignedStaff = assignedStaff;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpectedReadyAt() {
        return expectedReadyAt;
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
                + assignedStaff + ","
                + status + ","
                + createdAt + ","
                + expectedReadyAt + ","
                + remarks.replace(",", ";");
    }
}
