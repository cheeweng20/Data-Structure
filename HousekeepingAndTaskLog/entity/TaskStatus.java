package HousekeepingAndTaskLog.entity;
/**
 * @author Zhe Sheng
 */

public enum TaskStatus {
    DIRTY,
    CLEANING_IN_PROGRESS,
    INSPECTED,
    READY_FOR_CHECK_IN,
    /**
     * Cleaning cannot begin yet, for example because the checked-in guest has
     * been granted a late check-out time.
     */
    BLOCKED
}
