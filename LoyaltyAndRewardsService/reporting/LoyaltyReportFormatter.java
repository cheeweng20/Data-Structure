package LoyaltyAndRewardsService.reporting;

import LoyaltyAndRewardsService.entity.PointTransaction;
import adt.SortedArrayList;
import java.time.LocalDate;

/**
 * Builds the textual loyalty reports displayed by the UI and exported to PDF.
 *
 * @author Chee Weng
 */
public final class LoyaltyReportFormatter {
    private LoyaltyReportFormatter() {
    }

    public static String buildExpiringPointsReport(
            SortedArrayList<PointTransaction> transactions) {
        if (transactions.isEmpty()) {
            return "";
        }

        StringBuilder output = new StringBuilder();
        String border = "+----------------+------------+-----------------+--------------+";
        output.append("=== Expiring Points Alert ===\n");
        output.append(border).append('\n');
        output.append(String.format("| %-14s | %-10s | %15s | %-12s |%n",
                "Transaction ID", "Member ID", "Points Expiring", "Expiry Date"));
        output.append(border).append('\n');

        for (PointTransaction transaction : transactions) {
            output.append(String.format("| %-14.14s | %-10.10s | %15d | %-12s |%n",
                    transaction.getTransactionId(), transaction.getMemberId(),
                    transaction.getPointsRemaining(), transaction.getExpiryDate()));
        }
        output.append(border);
        return output.toString();
    }

    public static String buildPointsTransactionReport(LocalDate startDate,
            LocalDate endDate, SortedArrayList<PointTransaction> transactions) {
        if (transactions.isEmpty()) {
            return "";
        }

        int totalPointsEarned = 0;
        for (PointTransaction transaction : transactions) {
            totalPointsEarned += transaction.getPointsEarned();
        }

        StringBuilder output = new StringBuilder();
        output.append("=== Points Transaction Report ===\n");
        output.append("Report Period: ").append(startDate).append(" to ")
                .append(endDate).append('\n');
        output.append("Total Transactions: ")
                .append(transactions.getNumberOfEntries()).append('\n');
        output.append("Total Points Earned: ").append(totalPointsEarned).append("\n\n");
        String border = "+----------------+------------+----------------+-------------+";
        output.append(border).append('\n');
        output.append(String.format("| %-14s | %-10s | %-14s | %-11s |%n",
                "Transaction ID", "Member ID", "Points Earned", "Earned Date"));
        output.append(border).append('\n');

        for (PointTransaction transaction : transactions) {
            output.append(String.format("| %-14.14s | %-10.10s | %14d | %-11s |%n",
                    transaction.getTransactionId(), transaction.getMemberId(),
                    transaction.getPointsEarned(), transaction.getEarnedDate()));
        }
        output.append(border);
        return output.toString();
    }
}
