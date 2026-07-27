package LoyaltyAndRewardsService.control;


import adt.LinkedQueue;
import adt.LinkedList;

import java.time.LocalDate;
import java.util.Iterator;

import LoyaltyAndRewardsService.dao.RequestDao;
import LoyaltyAndRewardsService.entity.Member;
import LoyaltyAndRewardsService.entity.RedemptionRequest;
import LoyaltyAndRewardsService.entity.Reward;

/**
 * @author Chee Weng
 */
public class RequestControl {
    private LinkedQueue<RedemptionRequest> requestQueue;
    private LinkedList<RedemptionRequest> requestHistory;
    private MemberControl memberControl;
    private TransactionControl transactionControl;
    private int nextRequestNumber;

    public RequestControl(MemberControl memberControl) {
        this(memberControl, null);
    }

    public RequestControl(MemberControl memberControl, TransactionControl transactionControl) {
        requestQueue = new LinkedQueue<>();
        requestHistory = new LinkedList<>();
        this.memberControl = memberControl;
        this.transactionControl = transactionControl;
        nextRequestNumber = 1;
    }

    public boolean submitRequest(String memberId, int pointsRequested) {
        return submitRequest(memberId, "", pointsRequested);
    }

    public boolean submitRequest(String memberId, Reward reward) {
        if (reward == null) {
            return false;
        }
        return submitRequest(memberId, reward.getRewardId(), reward.getPointRequired());
    }

    public RequestOperationResult submitRewardRequest(String memberId, String rewardId,
            RewardControl rewardControl) {
        if (!memberControl.findMember(memberId)) {
            return RequestOperationResult.failure("Member not found.");
        }

        int pointsRequired = rewardControl.getRewardPointRequired(rewardId);
        String rewardName = rewardControl.getRewardName(rewardId);
        if (pointsRequired <= 0 || rewardName == null) {
            return RequestOperationResult.failure("Reward not found.");
        }

        if (!submitRequest(memberId, rewardId, pointsRequired)) {
            return RequestOperationResult.failure(
                    "Insufficient available points; request not accepted.");
        }

        saveRequests();
        return RequestOperationResult.success(
                "Request for " + rewardName + " submitted and is waiting to be processed.");
    }

    private boolean submitRequest(String memberId, String rewardId, int pointsRequested) {
        Member currentMember = memberControl.getMemberById(memberId);
        if (currentMember == null || pointsRequested <= 0) {
            return false;
        }

        int availablePoints = currentMember.getPoint() - getPendingPointsForMember(memberId);
        boolean hasEnoughPoints = availablePoints >= pointsRequested;

        if (!hasEnoughPoints) {
            return false;
        }

        String requestId = generateRequestId();
        RedemptionRequest request = new RedemptionRequest(
                requestId, memberId, rewardId, pointsRequested, LocalDate.now(), "Pending");
        requestQueue.enqueue(request);
        requestHistory.add(request);
        return true;
    }

    public RedemptionRequest peekNextRequest() {
        return requestQueue.getFront();
    }

    public RedemptionRequest processNextRequest(boolean approve) {
        RedemptionRequest request = requestQueue.dequeue();
        if (request == null) return null;

        if (approve) {
            int newPoint = memberControl.redeemPoint(request.getMemberId(), request.getPointsRequested());
            if (newPoint >= 0) {
                if (transactionControl != null) {
                    transactionControl.redeemPointsFromOldestTransactions(
                            request.getMemberId(), request.getPointsRequested());
                }
                request.setStatus("Approved");
            } else {
                request.setStatus("Rejected - insufficient points");
            }
        } else {
            request.setStatus("Rejected");
        }
        return request;
    }

    public RequestOperationResult processNextRequestAndSave(boolean approve) {
        RedemptionRequest next = peekNextRequest();
        if (next == null) {
            return RequestOperationResult.failure("No pending requests.");
        }

        String previousTierId = memberControl.getMemberTierId(next.getMemberId());
        RedemptionRequest processed = processNextRequest(approve);
        saveRequests();

        if ("Approved".equalsIgnoreCase(processed.getStatus())) {
            memberControl.saveMembers();
            if (transactionControl != null) {
                transactionControl.saveTransactions();
            }
        }

        String newTierId = memberControl.getMemberTierId(processed.getMemberId());
        boolean tierChanged = previousTierId == null
                ? newTierId != null
                : !previousTierId.equalsIgnoreCase(newTierId);
        String tierChangeMessage = tierChanged
                ? "Tier changed: " + memberControl.getTierName(previousTierId)
                        + " -> " + memberControl.getTierName(newTierId)
                : "";

        if ("Approved".equalsIgnoreCase(processed.getStatus())) {
            return RequestOperationResult.processed(
                    true, "Request approved.", tierChangeMessage);
        }
        return RequestOperationResult.processed(
                false, "Request " + processed.getStatus() + ".", tierChangeMessage);
    }

    public boolean isEmpty() {
        return requestQueue.isEmpty();
    }

    public void addRequest(RedemptionRequest request) {
        requestHistory.add(request);
        if ("Pending".equalsIgnoreCase(request.getStatus())) {
            requestQueue.enqueue(request);
        }
        updateNextRequestNumber(request.getRequestId());
    }

    public Iterator<RedemptionRequest> getRequestIterator() {
        return requestHistory.iterator();
    }

    public Iterator<RedemptionRequest> getPendingRequestIterator() {
        return requestQueue.getIterator();
    }

    public int getPendingRequestCount() {
        int count = 0;
        Iterator<RedemptionRequest> iterator = requestQueue.getIterator();
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }

    public String getNextRequestTable() {
        RedemptionRequest request = peekNextRequest();
        if (request == null) {
            return "";
        }
        return buildRequestTable("Next Redemption Request", singleRequestIterator(request));
    }

    public String getPendingRequestTable() {
        return buildRequestTable("Pending Redemption Requests", getPendingRequestIterator());
    }

    public String getRequestHistoryTable() {
        return buildRequestTable("Redemption Request History", getRequestIterator());
    }

    public void saveRequests() {
        RequestDao.saveToRequestFile(this);
    }

    private int getPendingPointsForMember(String memberId) {
        int pendingPoints = 0;
        Iterator<RedemptionRequest> iterator = requestQueue.getIterator();
        while (iterator.hasNext()) {
            RedemptionRequest request = iterator.next();
            if (request.getMemberId().equalsIgnoreCase(memberId)
                    && "Pending".equalsIgnoreCase(request.getStatus())) {
                pendingPoints += request.getPointsRequested();
            }
        }
        return pendingPoints;
    }

    private String generateRequestId() {
        return String.format("R%03d", nextRequestNumber++);
    }

    private void updateNextRequestNumber(String requestId) {
        int number = Integer.parseInt(requestId.substring(1));
        if (number >= nextRequestNumber) {
            nextRequestNumber = number + 1;
        }
    }

    private Iterator<RedemptionRequest> singleRequestIterator(RedemptionRequest request) {
        LinkedList<RedemptionRequest> singleRequest = new LinkedList<>();
        singleRequest.add(request);
        return singleRequest.iterator();
    }

    private String buildRequestTable(String title, Iterator<RedemptionRequest> iterator) {
        if (!iterator.hasNext()) {
            return "";
        }

        StringBuilder output = new StringBuilder();
        String border = "+------------+------------+------------+------------------+------------+--------------------------------+";
        output.append("=== ").append(title).append(" ===").append(System.lineSeparator());
        output.append(border).append(System.lineSeparator());
        output.append(String.format("| %-10s | %-10s | %-10s | %16s | %-10s | %-30s |%n",
                "Request ID", "Member ID", "Reward ID", "Points", "Date", "Status"));
        output.append(border).append(System.lineSeparator());

        while (iterator.hasNext()) {
            RedemptionRequest request = iterator.next();
            String rewardId = request.getRewardId() == null || request.getRewardId().isBlank()
                    ? "Legacy"
                    : request.getRewardId();
            output.append(String.format("| %-10.10s | %-10.10s | %-10.10s | %16d | %-10s | %-30.30s |%n",
                    request.getRequestId(), request.getMemberId(), rewardId,
                    request.getPointsRequested(), request.getRequestDate(), request.getStatus()));
        }

        output.append(border);
        return output.toString();
    }

    public static final class RequestOperationResult {
        private final boolean successful;
        private final boolean approved;
        private final String message;
        private final String tierChangeMessage;

        private RequestOperationResult(boolean successful, boolean approved, String message,
                String tierChangeMessage) {
            this.successful = successful;
            this.approved = approved;
            this.message = message;
            this.tierChangeMessage = tierChangeMessage;
        }

        public static RequestOperationResult failure(String message) {
            return new RequestOperationResult(false, false, message, "");
        }

        public static RequestOperationResult success(String message) {
            return new RequestOperationResult(true, false, message, "");
        }

        public static RequestOperationResult processed(boolean approved, String message,
                String tierChangeMessage) {
            return new RequestOperationResult(true, approved, message, tierChangeMessage);
        }

        public boolean isSuccessful() {
            return successful;
        }

        public boolean isApproved() {
            return approved;
        }

        public String getMessage() {
            return message;
        }

        public boolean hasTierChange() {
            return !tierChangeMessage.isEmpty();
        }

        public String getTierChangeMessage() {
            return tierChangeMessage;
        }
    }
}
