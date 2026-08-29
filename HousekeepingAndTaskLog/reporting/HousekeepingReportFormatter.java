package HousekeepingAndTaskLog.reporting;

import HousekeepingAndTaskLog.entity.HousekeepingTask;
import HousekeepingAndTaskLog.entity.TaskStatus;
import adt.ListInterface;
import java.util.Iterator;

/** Builds the text reports used by Housekeeping's console and PDF exports. */
/**
 * @author Zhe Sheng
 */
public final class HousekeepingReportFormatter {

    private HousekeepingReportFormatter() {
    }

    public static String buildTaskStatusSummary(int[] totals) {
        StringBuilder report = new StringBuilder();
        report.append("=== Task Status Summary Report ===\n");
        report.append("+----------------------------+----------+\n");
        report.append(String.format("| %-26s | %8s |%n", "Status", "Total"));
        report.append("+----------------------------+----------+\n");
        for (TaskStatus status : TaskStatus.values()) {
            report.append(String.format("| %-26s | %8d |%n", status, totals[status.ordinal()]));
        }
        report.append("+----------------------------+----------+\n");
        return report.toString();
    }

    public static String buildTaskListReport(ListInterface<HousekeepingTask> tasks) {
        if (tasks.isEmpty()) {
            return "=== Filtered Housekeeping Tasks ===\nNo housekeeping task record found.\n";
        }
        StringBuilder report = new StringBuilder("=== Filtered Housekeeping Tasks ===\n");
        report.append("+----------+--------------+------------------------+---------------------+---------------------+\n");
        report.append(String.format("| %-8s | %-12s | %-22s | %-19s | %-19s |%n",
                "Task ID", "Room", "Status", "Created At", "Completed At"));
        report.append("+----------+--------------+------------------------+---------------------+---------------------+\n");
        Iterator<HousekeepingTask> iterator = tasks.iterator();
        while (iterator.hasNext()) {
            HousekeepingTask task = iterator.next();
            report.append(String.format("| %-8s | %-12s | %-22s | %-19s | %-19s |%n",
                    task.getTaskId(), task.getRoomNumber(), task.getStatus(), task.getCreatedAt(),
                    task.getCompletedAt()));
        }
        return report.append("+----------+--------------+------------------------+---------------------+---------------------+\n")
                .toString();
    }
}
