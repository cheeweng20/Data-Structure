package LoyaltyAndRewardsService.entity;

import java.time.LocalDate;

/**
 * @author Chee Weng
 */
public class RedemptionRequest implements Comparable<RedemptionRequest> {
    private String requestId;
    private String memberId;
    private String rewardId;
    private int pointsRequested;
    private LocalDate requestDate;
    private String status;

    public RedemptionRequest(String requestId, String memberId, int pointsRequested, LocalDate requestDate,
            String status) {
        this(requestId, memberId, "", pointsRequested, requestDate, status);
    }

    public RedemptionRequest(String requestId, String memberId, String rewardId, int pointsRequested,
            LocalDate requestDate, String status) {
        this.requestId = requestId;
        this.memberId = memberId;
        this.rewardId = rewardId;
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

    public String getRewardId() {
        return rewardId;
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

    public void setRewardId(String rewardId) {
        this.rewardId = rewardId;
    }

    public void setRequestDate(LocalDate requestDate) {
        this.requestDate = requestDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public int compareTo(RedemptionRequest other) {
        return requestDate.compareTo(other.requestDate);
    }

    public String toCsvLine(){
        return requestId + "," + memberId + "," + rewardId + "," + pointsRequested + ","
                + requestDate + "," + status;
    }
}
