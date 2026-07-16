package LoyaltyAndRewardsService.entity;

import java.time.LocalDate;

/**
 * @author Chee Weng
 */
public class RedemptionRequest {
    private String requestId;
    private String memberId;
    private int pointsRequested;
    private LocalDate requestDate;
    private String status;

    public RedemptionRequest(String requestId, String memberId, int pointsRequested, LocalDate requestDate,
            String status) {
        this.requestId = requestId;
        this.memberId = memberId;
        this.pointsRequested = pointsRequested;
        this.requestDate = requestDate;
        this.status = status;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getMemberId() {
        return memberId;
    }

    public int getPointsRequested() {
        return pointsRequested;
    }

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public String getStatus() {
        return status;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public void setPointsRequested(int pointsRequested) {
        this.pointsRequested = pointsRequested;
    }

    public void setRequestDate(LocalDate requestDate) {
        this.requestDate = requestDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String toCsvLine(){
        return requestId + "," + memberId  + "," + pointsRequested + "," + requestDate + "," + status;
    }
}
