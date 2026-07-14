package LoyaltyAndRewardsService.control;


import adt.LinkedQueue;

import java.time.LocalDate;
import java.util.Iterator;

import LoyaltyAndRewardsService.entity.Member;
import LoyaltyAndRewardsService.entity.RedemptionRequest;

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
        boolean hasEnoughPoints =  currentMember.getPoint() >= pointsRequested;

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
            memberControl.redeemPoint(request.getMemberId(), request.getPointsRequested());
            request.setStatus("Approved");
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
