package LoyaltyAndRewardsService.control;


import adt.LinkedQueue;

import java.time.LocalDate;
import java.util.Iterator;

import LoyaltyAndRewardsService.entity.Member;
import LoyaltyAndRewardsService.entity.RedemptionRequest;

/**
 * @author Chee Weng
 */
public class RequestControl {
    private LinkedQueue<RedemptionRequest> requestQueue;
    private MemberControl memberControl;
    private int nextRequestNumber;

    public RequestControl(MemberControl memberControl) {
        requestQueue = new LinkedQueue<>();
        this.memberControl = memberControl;
        nextRequestNumber = 1;
    }

    public boolean submitRequest(String memberId, int pointsRequested) {
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
                requestId, memberId, pointsRequested, LocalDate.now(), "Pending");
        requestQueue.enqueue(request);
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
                request.setStatus("Approved");
            } else {
                request.setStatus("Rejected - insufficient points");
            }
        } else {
            request.setStatus("Rejected");
        }
        return request;
    }

    public boolean isEmpty() {
        return requestQueue.isEmpty();
    }

    public void addRequest(RedemptionRequest request) {
        requestQueue.enqueue(request);
        updateNextRequestNumber(request.getRequestId());
    }

    public Iterator<RedemptionRequest> getRequestIterator() {
        return requestQueue.getIterator();
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
}
