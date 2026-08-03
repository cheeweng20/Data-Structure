package LoyaltyAndRewardsService.control;


import adt.LinkedQueue;
import adt.LinkedList;

import java.time.LocalDate;
import java.util.Iterator;

import LoyaltyAndRewardsService.dao.RequestDao;
import LoyaltyAndRewardsService.entity.Member;
import LoyaltyAndRewardsService.entity.RedemptionRequest;
import LoyaltyAndRewardsService.utility.MessageUI;

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

    public boolean submitRewardRequest(String memberId, String rewardId,
            RewardControl rewardControl) {
        if (!memberControl.findMember(memberId)) {
            MessageUI.displayError("Member not found.");
            return false;
        }

        int pointsRequired = rewardControl.getRewardPointRequired(rewardId);
        String rewardName = rewardControl.getRewardName(rewardId);
        if (pointsRequired <= 0 || rewardName == null) {
            MessageUI.displayError("Reward not found.");
            return false;
        }

        if (!createPendingRequest(memberId, rewardId, pointsRequired)) {
            MessageUI.displayError("Insufficient available points; request not accepted.");
            return false;
        }

        saveRequests();
        MessageUI.displayRequestSubmitted(rewardName);
        return true;
    }

    private boolean createPendingRequest(String memberId, String rewardId, int pointsRequested) {
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

    public RedemptionRequest processNextRequestAndSave(boolean approve) {
        RedemptionRequest next = peekNextRequest();
        if (next == null) {
            MessageUI.displayInfo("No pending requests.");
            return null;
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
        MessageUI.displayRequestProcessed(processed.getStatus());
        if (tierChanged) {
            MessageUI.displayTierChange(
                    memberControl.getTierName(previousTierId),
                    memberControl.getTierName(newTierId));
        }
        return processed;
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

}
