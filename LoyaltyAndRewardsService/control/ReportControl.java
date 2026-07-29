package LoyaltyAndRewardsService.control;

import java.time.LocalDate;
import java.util.Iterator;

import LoyaltyAndRewardsService.entity.Member;
import LoyaltyAndRewardsService.entity.PointTransaction;
import LoyaltyAndRewardsService.entity.RedemptionRequest;
import adt.ArrayList;

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
        output.append("=== Member Point Ranking Report ===").append(System.lineSeparator());
        output.append(border).append(System.lineSeparator());
        output.append(String.format("| %-14s | %-20s | %-10s | %8s |%n",
                "Tier Name", "Member Name", "Member ID", "Points"));
        output.append(border).append(System.lineSeparator());

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
        String border = "+--------------+------------+----------------+--------------+";
        output.append("=== Expiring Points Alert ===").append(System.lineSeparator());
        output.append(border).append(System.lineSeparator());
        output.append(String.format("| %-12s | %-10s | %14s | %-12s |%n",
                "Transaction ID", "Member ID", "Points Expiring", "Expiry Date"));
        output.append(border).append(System.lineSeparator());

        Iterator<PointTransaction> iterator = transactions.iterator();
        while (iterator.hasNext()) {
            PointTransaction transaction = iterator.next();
            output.append(String.format("| %-12.12s | %-10.10s | %14d | %-12s |%n",
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
        output.append("=== Business Cycle Summary Report ===").append(System.lineSeparator());
        output.append("Cycle Period: ").append(startDate).append(" to ").append(endDate)
                .append(System.lineSeparator());
        output.append("Applied Filters: tier=")
                .append(tierId == null || tierId.isBlank() ? "All" : tierId)
                .append(", minimum point=").append(minimumPoint)
                .append(System.lineSeparator()).append(System.lineSeparator());

        appendMemberSummary(output, rankedMembers);
        appendTransactionSummary(output, transactions);
        appendRequestSummary(output, requests);
        return output.toString();
    }

    private ArrayList<PointTransaction> filterTransactionsByDateRange(
            LocalDate startDate, LocalDate endDate) {
        ArrayList<PointTransaction> result = new ArrayList<>();
        Iterator<PointTransaction> iterator = transactionControl.getTransactionIterator();
        while (iterator.hasNext()) {
            PointTransaction current = iterator.next();
            if (!current.getEarnedDate().isBefore(startDate)
                    && !current.getEarnedDate().isAfter(endDate)) {
                result.add(current);
            }
        }
        selectionSortTransactionsByEarnedDate(result);
        return result;
    }

    private ArrayList<RedemptionRequest> filterRequestsByDateRange(
            LocalDate startDate, LocalDate endDate) {
        ArrayList<RedemptionRequest> result = new ArrayList<>();
        Iterator<RedemptionRequest> iterator = requestControl.getRequestIterator();
        while (iterator.hasNext()) {
            RedemptionRequest current = iterator.next();
            if (!current.getRequestDate().isBefore(startDate)
                    && !current.getRequestDate().isAfter(endDate)) {
                result.add(current);
            }
        }
        selectionSortRequestsByDate(result);
        return result;
    }

    private void appendMemberSummary(StringBuilder output, ArrayList<Member> members) {
        String border = "+------------+----------------------+------------+----------+";
        output.append("=== Top Members by Current Points ===").append(System.lineSeparator());
        output.append(border).append(System.lineSeparator());
        output.append(String.format("| %-10s | %-20s | %-10s | %8s |%n",
                "Member ID", "Member Name", "Tier", "Points"));
        output.append(border).append(System.lineSeparator());

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
        output.append(border).append(System.lineSeparator());
    }

    private void appendTransactionSummary(StringBuilder output,
            ArrayList<PointTransaction> transactions) {
        int totalPointsEarned = 0;
        Iterator<PointTransaction> totalIterator = transactions.iterator();
        while (totalIterator.hasNext()) {
            totalPointsEarned += totalIterator.next().getPointsEarned();
        }

        output.append(System.lineSeparator()).append("=== Transaction Summary ===")
                .append(System.lineSeparator());
        output.append("Transactions in cycle: ")
                .append(transactions.getNumberOfEntries()).append(System.lineSeparator());
        output.append("Total points earned in cycle: ")
                .append(totalPointsEarned).append(System.lineSeparator());

        String border = "+----------------+------------+----------------+-------------+";
        output.append(border).append(System.lineSeparator());
        output.append(String.format("| %-14s | %-10s | %-14s | %-11s |%n",
                "Transaction ID", "Member ID", "Points Earned", "Earned Date"));
        output.append(border).append(System.lineSeparator());

        Iterator<PointTransaction> iterator = transactions.iterator();
        while (iterator.hasNext()) {
            PointTransaction transaction = iterator.next();
            output.append(String.format("| %-14.14s | %-10.10s | %14d | %-11s |%n",
                    transaction.getTransactionId(), transaction.getMemberId(),
                    transaction.getPointsEarned(), transaction.getEarnedDate()));
        }
        output.append(border).append(System.lineSeparator());
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

        output.append(System.lineSeparator()).append("=== Redemption Request Summary ===")
                .append(System.lineSeparator());
        output.append("Requests in cycle: ").append(requests.getNumberOfEntries())
                .append(System.lineSeparator());
        output.append("Pending: ").append(pending)
                .append(", Approved: ").append(approved)
                .append(", Rejected: ").append(rejected)
                .append(System.lineSeparator());

        String border =
                "+------------+------------+------------+------------------+--------------------+--------------------------------+";
        output.append(border).append(System.lineSeparator());
        output.append(String.format("| %-10s | %-10s | %-10s | %-16s | %-18s | %-30s |%n",
                "Request ID", "Member ID", "Reward ID", "Points Requested",
                "Request Date", "Status"));
        output.append(border).append(System.lineSeparator());

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

    private void selectionSortTransactionsByEarnedDate(ArrayList<PointTransaction> list) {
        for (int i = 1; i < list.getNumberOfEntries(); i++) {
            int targetPosition = i;
            PointTransaction targetValue = list.getEntry(i);

            for (int j = i + 1; j <= list.getNumberOfEntries(); j++) {
                PointTransaction current = list.getEntry(j);
                if (current.getEarnedDate().compareTo(targetValue.getEarnedDate()) < 0) {
                    targetValue = current;
                    targetPosition = j;
                }
            }
            insertSelectedEntry(list, i, targetPosition, targetValue);
        }
    }

    private void selectionSortRequestsByDate(ArrayList<RedemptionRequest> list) {
        for (int i = 1; i < list.getNumberOfEntries(); i++) {
            int targetPosition = i;
            RedemptionRequest targetValue = list.getEntry(i);

            for (int j = i + 1; j <= list.getNumberOfEntries(); j++) {
                RedemptionRequest current = list.getEntry(j);
                if (current.compareTo(targetValue) < 0) {
                    targetValue = current;
                    targetPosition = j;
                }
            }
            insertSelectedEntry(list, i, targetPosition, targetValue);
        }
    }

    private <T> void insertSelectedEntry(ArrayList<T> list, int position,
            int targetPosition, T targetValue) {
        if (targetPosition == position) {
            return;
        }
        for (int current = targetPosition; current > position; current--) {
            list.replace(current, list.getEntry(current - 1));
        }
        list.replace(position, targetValue);
    }
}
