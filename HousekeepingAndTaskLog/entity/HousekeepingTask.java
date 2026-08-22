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
    private String remarks;

    public HousekeepingTask(String taskId, String roomNumber, TaskStatus status,
            LocalDateTime createdAt, String remarks) {
        this.taskId = taskId;
        this.roomNumber = roomNumber;
        this.status = status;
        this.createdAt = createdAt;
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
                + remarks.replace(",", ";");
    }
}
