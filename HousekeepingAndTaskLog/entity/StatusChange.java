package HousekeepingAndTaskLog.entity;

import java.time.LocalDateTime;
import VIPPriorityRoomAllocation.entity.Room.RoomStatus;

/**
 * @author Zhe Sheng
 */

public class StatusChange {

    private String taskId;
    private TaskStatus previousStatus;
    private TaskStatus newStatus;
    private LocalDateTime previousCompletedAt;
    private RoomStatus previousRoomStatus;
    private LocalDateTime changedAt;

    public StatusChange(String taskId, TaskStatus previousStatus, TaskStatus newStatus,
            LocalDateTime previousCompletedAt, RoomStatus previousRoomStatus,
            LocalDateTime changedAt) {
        this.taskId = taskId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.previousCompletedAt = previousCompletedAt;
        this.previousRoomStatus = previousRoomStatus;
        this.changedAt = changedAt;
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

    public LocalDateTime getPreviousCompletedAt() {
        return previousCompletedAt;
    }

    public RoomStatus getPreviousRoomStatus() {
        return previousRoomStatus;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
