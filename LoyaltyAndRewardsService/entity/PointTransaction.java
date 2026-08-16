package LoyaltyAndRewardsService.entity;

import java.time.LocalDate;

/**
 * @author Chee Weng
 */
public class PointTransaction implements Comparable<PointTransaction> {
    private String transactionId;
    private String memberId;
    private int pointsEarned;
    private int pointsRemaining;
    private LocalDate earnedDate;
    private LocalDate expiryDate;
    private String sourceReference;

    public PointTransaction(String transactionId, String memberId, int pointsEarned, int pointsRemaining,
            LocalDate earnedDate, LocalDate expiryDate, String sourceReference) {
        this.transactionId = transactionId;
        this.memberId = memberId;
        this.pointsEarned = pointsEarned;
        this.pointsRemaining = pointsRemaining;
        this.earnedDate = earnedDate;
        this.expiryDate = expiryDate;
        this.sourceReference = sourceReference == null ? "" : sourceReference;
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

    public int getPointsRemaining() {
        return pointsRemaining;
    }

    public void setPointsRemaining(int pointsRemaining) {
        this.pointsRemaining = Math.max(pointsRemaining, 0);
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

    public String getSourceReference() {
        return sourceReference;
    }

    public void setSourceReference(String sourceReference) {
        this.sourceReference = sourceReference == null ? "" : sourceReference;
    }

    @Override
    public int compareTo(PointTransaction other) {
        return expiryDate.compareTo(other.expiryDate);
    }

    public String toCsvLine() {
        return transactionId + "," + memberId + "," + pointsEarned + "," + pointsRemaining +
                "," + earnedDate + "," + expiryDate + "," + sourceReference;
    }
}
