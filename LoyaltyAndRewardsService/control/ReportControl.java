package LoyaltyAndRewardsService.control;

import java.time.LocalDate;
import java.util.Iterator;

import LoyaltyAndRewardsService.entity.Member;
import LoyaltyAndRewardsService.entity.PointTransaction;
import LoyaltyAndRewardsService.entity.RedemptionRequest;
import adt.ArrayList;
import adt.SortedArrayList;

/**
 * Performs report searching, filtering, sorting, and formatting.
 *
 * @author Chee Weng
 */
public class ReportControl {
    private MemberControl memberControl;
    private TierControl tierControl;
    private TransactionControl transactionControl;
    private RequestControl requestControl;

    public ReportControl(MemberControl memberControl, TierControl tierControl,
            TransactionControl transactionControl, RequestControl requestControl) {
        this.memberControl = memberControl;
        this.tierControl = tierControl;
        this.transactionControl = transactionControl;
        this.requestControl = requestControl;
    }

    public String generateMemberRankingReport(int minimumPoint, String tierId) {
        ArrayList<Member> members = memberControl.generateRankingReport(minimumPoint, tierId);
        if (members.isEmpty()) {
            return "";
        }

        StringBuilder output = new StringBuilder();
        String border = "+----------------+----------------------+------------+----------+";
        output.append("=== Member Point Ranking Report ===").append("\n");
        output.append(border).append("\n");
        output.append(String.format("| %-14s | %-20s | %-10s | %8s |%n",
                "Tier Name", "Member Name", "Member ID", "Points"));
        output.append(border).append("\n");

        Iterator<Member> iterator = members.iterator();
        while (iterator.hasNext()) {
            Member member = iterator.next();
            output.append(String.format("| %-14.14s | %-20.20s | %-10.10s | %8d |%n",
                    tierControl.getTierNameById(member.getTierId()), member.getName(),
                    member.getMemberId(), member.getPoint()));
        }
        output.append(border);
        return output.toString();
    }

    public String generateExpiringPointsReport(int withinDays) {
        ArrayList<PointTransaction> transactions =
                transactionControl.generateExpiringReport(withinDays);
        if (transactions.isEmpty()) {
            return "";
        }

        StringBuilder output = new StringBuilder();
        String border = "+----------------+------------+-----------------+--------------+";
        output.append("=== Expiring Points Alert ===").append("\n");
        output.append(border).append("\n");
        output.append(String.format("| %-14s | %-10s | %15s | %-12s |%n",
                "Transaction ID", "Member ID", "Points Expiring", "Expiry Date"));
        output.append(border).append("\n");

        Iterator<PointTransaction> iterator = transactions.iterator();
        while (iterator.hasNext()) {
            PointTransaction transaction = iterator.next();
            output.append(String.format("| %-14.14s | %-10.10s | %15d | %-12s |%n",
                    transaction.getTransactionId(), transaction.getMemberId(),
                    transaction.getPointsRemaining(), transaction.getExpiryDate()));
        }
        output.append(border);
        return output.toString();
    }

    public String generateBusinessCycleSummary(LocalDate startDate, LocalDate endDate,
            String tierId, int minimumPoint) {
        ArrayList<Member> rankedMembers =
                memberControl.generateRankingReport(minimumPoint, tierId);
        ArrayList<PointTransaction> transactions =
                filterTransactionsByDateRange(startDate, endDate);
        ArrayList<RedemptionRequest> requests =
                filterRequestsByDateRange(startDate, endDate);

        StringBuilder output = new StringBuilder();
        output.append("=== Business Cycle Summary Report ===").append("\n");
        output.append("Cycle Period: ").append(startDate).append(" to ").append(endDate)
                .append("\n");
        output.append("Applied Filters: tier=")
                .append(tierId == null || tierId.isBlank() ? "All" : tierId)
                .append(", minimum point=").append(minimumPoint)
                .append("\n").append("\n");

        appendMemberSummary(output, rankedMembers);
        appendTransactionSummary(output, transactions);
        appendRequestSummary(output, requests);
        return output.toString();
    }

    private ArrayList<PointTransaction> filterTransactionsByDateRange(
            LocalDate startDate, LocalDate endDate) {
        SortedArrayList<PointTransaction> sortedResult = new SortedArrayList<>(
                (left, right) -> left.getEarnedDate().compareTo(right.getEarnedDate()));
        Iterator<PointTransaction> iterator = transactionControl.getTransactionIterator();
        while (iterator.hasNext()) {
            PointTransaction current = iterator.next();
            if (!current.getEarnedDate().isBefore(startDate)
                    && !current.getEarnedDate().isAfter(endDate)) {
                sortedResult.add(current);
            }
        }
        return copyToArrayList(sortedResult);
    }

    private ArrayList<RedemptionRequest> filterRequestsByDateRange(
            LocalDate startDate, LocalDate endDate) {
        SortedArrayList<RedemptionRequest> sortedResult = new SortedArrayList<>();
        Iterator<RedemptionRequest> iterator = requestControl.getRequestIterator();
        while (iterator.hasNext()) {
            RedemptionRequest current = iterator.next();
            if (!current.getRequestDate().isBefore(startDate)
                    && !current.getRequestDate().isAfter(endDate)) {
                sortedResult.add(current);
            }
        }
        return copyToArrayList(sortedResult);
    }

    private void appendMemberSummary(StringBuilder output, ArrayList<Member> members) {
        String border = "+------------+----------------------+------------+----------+";
        output.append("=== Top Members by Current Points ===").append("\n");
        output.append(border).append("\n");
        output.append(String.format("| %-10s | %-20s | %-10s | %8s |%n",
                "Member ID", "Member Name", "Tier", "Points"));
        output.append(border).append("\n");

        if (members.isEmpty()) {
            output.append(String.format("| %-10s | %-20s | %-10s | %8s |%n",
                    "-", "No matching members", "-", "-"));
        } else {
            Iterator<Member> iterator = members.iterator();
            while (iterator.hasNext()) {
                Member member = iterator.next();
                output.append(String.format("| %-10.10s | %-20.20s | %-10.10s | %8d |%n",
                        member.getMemberId(), member.getName(),
                        tierControl.getTierNameById(member.getTierId()), member.getPoint()));
            }
        }
        output.append(border).append("\n");
    }

    private void appendTransactionSummary(StringBuilder output,
            ArrayList<PointTransaction> transactions) {
        int totalPointsEarned = 0;
        Iterator<PointTransaction> totalIterator = transactions.iterator();
        while (totalIterator.hasNext()) {
            totalPointsEarned += totalIterator.next().getPointsEarned();
        }

        output.append("\n").append("=== Transaction Summary ===")
                .append("\n");
        output.append("Transactions in cycle: ")
                .append(transactions.getNumberOfEntries()).append("\n");
        output.append("Total points earned in cycle: ")
                .append(totalPointsEarned).append("\n");

        String border = "+----------------+------------+----------------+-------------+";
        output.append(border).append("\n");
        output.append(String.format("| %-14s | %-10s | %-14s | %-11s |%n",
                "Transaction ID", "Member ID", "Points Earned", "Earned Date"));
        output.append(border).append("\n");

        Iterator<PointTransaction> iterator = transactions.iterator();
        while (iterator.hasNext()) {
            PointTransaction transaction = iterator.next();
            output.append(String.format("| %-14.14s | %-10.10s | %14d | %-11s |%n",
                    transaction.getTransactionId(), transaction.getMemberId(),
                    transaction.getPointsEarned(), transaction.getEarnedDate()));
        }
        output.append(border).append("\n");
    }

    private void appendRequestSummary(StringBuilder output,
            ArrayList<RedemptionRequest> requests) {
        int pending = 0;
        int approved = 0;
        int rejected = 0;

        Iterator<RedemptionRequest> statusIterator = requests.iterator();
        while (statusIterator.hasNext()) {
            String status = statusIterator.next().getStatus();
            if ("Pending".equalsIgnoreCase(status)) {
                pending++;
            } else if ("Approved".equalsIgnoreCase(status)) {
                approved++;
            } else {
                rejected++;
            }
        }

        output.append("\n").append("=== Redemption Request Summary ===")
                .append("\n");
        output.append("Requests in cycle: ").append(requests.getNumberOfEntries())
                .append("\n");
        output.append("Pending: ").append(pending)
                .append(", Approved: ").append(approved)
                .append(", Rejected: ").append(rejected)
                .append("\n");

        String border =
                "+------------+------------+------------+------------------+--------------------+--------------------------------+";
        output.append(border).append("\n");
        output.append(String.format("| %-10s | %-10s | %-10s | %-16s | %-18s | %-30s |%n",
                "Request ID", "Member ID", "Reward ID", "Points Requested",
                "Request Date", "Status"));
        output.append(border).append("\n");

        Iterator<RedemptionRequest> iterator = requests.iterator();
        while (iterator.hasNext()) {
            RedemptionRequest request = iterator.next();
            String rewardId = request.getRewardId() == null || request.getRewardId().isBlank()
                    ? "Legacy"
                    : request.getRewardId();
            output.append(String.format(
                    "| %-10.10s | %-10.10s | %-10.10s | %16d | %-18s | %-30.30s |%n",
                    request.getRequestId(), request.getMemberId(), rewardId,
                    request.getPointsRequested(), request.getRequestDate(), request.getStatus()));
        }
        output.append(border);
    }

    private <T extends Comparable<T>> ArrayList<T> copyToArrayList(
            SortedArrayList<T> sortedResult) {
        ArrayList<T> result = new ArrayList<>();
        for (T entry : sortedResult) {
            result.add(entry);
        }
        return result;
    }
}
