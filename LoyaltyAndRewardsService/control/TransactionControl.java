package LoyaltyAndRewardsService.control;

import java.time.LocalDate;
import java.util.Iterator;

import LoyaltyAndRewardsService.dao.PointTransactionDao;
import LoyaltyAndRewardsService.entity.PointTransaction;
import adt.ArrayList;
import adt.LinkedList;

/**
 * @author Chee Weng
 */
public class TransactionControl {
    private LinkedList<PointTransaction> transactionList;

    public TransactionControl() {
        transactionList = new LinkedList<>();
    }

    public PointTransaction getEntry(int position) {
        return transactionList.getEntry(position);
    }

    public int size() {
        return transactionList.size();
    }

    public Iterator<PointTransaction> getTransactionIterator() {
        return transactionList.iterator();
    }

    public boolean findTransaction(String transactionId) {
        return getTransactionById(transactionId) != null;
    }

    public void addTransaction(PointTransaction transaction) {
        transactionList.add(transaction);
    }

    public void addTransaction(String PointTransactionId, int points) {
        LocalDate earnedDate = LocalDate.now();
        LocalDate expiryDate = earnedDate.plusYears(1);

        String transactionId = generateTransactionId();
        PointTransaction transaction = new PointTransaction(transactionId, PointTransactionId, points, earnedDate, expiryDate);
        transactionList.add(transaction);
    }

    public ArrayList<PointTransaction> generateExpiringReport(int withinDays) {
        ArrayList<PointTransaction> filteredResult = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(withinDays);

        Iterator<PointTransaction> iterator = transactionList.iterator();
        while (iterator.hasNext()) {
            PointTransaction current = iterator.next();

            boolean matchesCriteria = current.getPointsRemaining() > 0
                    && !current.getExpiryDate().isBefore(today)
                    && !current.getExpiryDate().isAfter(cutoff);

            if (matchesCriteria) {
                filteredResult.add(current);
            }
        }

        selectionSortByExpiryDate(filteredResult);
        return filteredResult;
    }

    public int redeemPointsFromOldestTransactions(String memberId, int pointsToRedeem) {
        int remainingToRedeem = pointsToRedeem;

        while (remainingToRedeem > 0) {
            PointTransaction oldest = findOldestAvailableTransaction(memberId);
            if (oldest == null) {
                break;
            }

            int deducted = Math.min(oldest.getPointsRemaining(), remainingToRedeem);
            oldest.setPointsRemaining(oldest.getPointsRemaining() - deducted);
            remainingToRedeem -= deducted;
        }

        return pointsToRedeem - remainingToRedeem;
    }

    public int getExpiringTransactionCount(int withinDays) {
        return generateExpiringReport(withinDays).getNumberOfEntries();
    }

    public int getExpiringPointTotal(int withinDays) {
        int total = 0;
        Iterator<PointTransaction> iterator = generateExpiringReport(withinDays).iterator();
        while (iterator.hasNext()) {
            total += iterator.next().getPointsRemaining();
        }
        return total;
    }

    public void saveTransactions() {
        PointTransactionDao.saveToTransactionFile(this);
    }

    public String generateTransactionId() {
        if (transactionList.isEmpty()) {
            return "TS001";
        }

        PointTransaction lastTransactionId = transactionList.getEntry(transactionList.size());

        String lastId = lastTransactionId.getTransactionId();
        int prefixLength = lastId.startsWith("TS") ? 2 : 1;

        int number = Integer.parseInt(lastId.substring(prefixLength));

        number++;

        return String.format("TS%03d", number);
    }

    // Helper Function

    private PointTransaction getTransactionById(String transactionId) {
        for (int i = 1; i <= transactionList.size(); i++) {
            PointTransaction transaction = transactionList.getEntry(i);
            if (transaction.getTransactionId().equals(transactionId)) {
                return transaction;
            }
        }
        return null;
    }

    private PointTransaction findOldestAvailableTransaction(String memberId) {
        PointTransaction oldest = null;
        Iterator<PointTransaction> iterator = transactionList.iterator();

        while (iterator.hasNext()) {
            PointTransaction current = iterator.next();
            boolean belongsToMember = current.getMemberId().equalsIgnoreCase(memberId);
            if (belongsToMember && current.getPointsRemaining() > 0
                    && (oldest == null || current.compareTo(oldest) < 0)) {
                oldest = current;
            }
        }

        return oldest;
    }

    private void selectionSortByExpiryDate(ArrayList<PointTransaction> list) { 
        for (int i = 1; i <= list.getNumberOfEntries() - 1; i++) {
            int targetPosition = i;
            PointTransaction targetValue = list.getEntry(i);

            for (int j = i + 1; j <= list.getNumberOfEntries(); j++) {
                PointTransaction current = list.getEntry(j);
                if (current.compareTo(targetValue) < 0) {
                    targetValue = current;
                    targetPosition = j;
                }
            }

            if (targetPosition != i) {
                for (int position = targetPosition; position > i; position--) {
                    list.replace(position, list.getEntry(position - 1));
                }
                list.replace(i, targetValue);
            }
        }
    }

}
