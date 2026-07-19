package LoyaltyAndRewardsService.entity;

import java.time.LocalDate;

/**
 * @author Chee Weng
 */
public class PointTransaction {
    private String transactionId;
    private String memberId;
    private int pointsEarned;
    private LocalDate earnedDate;
    private LocalDate expiryDate;

    public PointTransaction(String transactionId, String memberId, int pointsEarned, LocalDate earnedDate,
            LocalDate expiryDate) {
        this.transactionId = transactionId;
        this.memberId = memberId;
        this.pointsEarned = pointsEarned;
        this.earnedDate = earnedDate;
        this.expiryDate = expiryDate;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(int pointsEarned) {
        this.pointsEarned = pointsEarned;
    }

    public LocalDate getEarnedDate() {
        return earnedDate;
    }

    public void setEarnedDate(LocalDate earnedDate) {
        this.earnedDate = earnedDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String toCsvLine() {
        return transactionId + "," + memberId + "," + pointsEarned +
                "," + earnedDate + "," + expiryDate;
    }
}
