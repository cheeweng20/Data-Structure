package LoyaltyAndRewardsService.control;

import java.time.LocalDate;

import LoyaltyAndRewardsService.entity.PointTransaction;
import adt.ArrayList;
import adt.LinkedList;

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

        for (int i = 1; i <= transactionList.size(); i++) {
            PointTransaction current = transactionList.getEntry(i);

            boolean matchesCriteria = !current.getExpiryDate().isBefore(today)
                    && !current.getExpiryDate().isAfter(cutoff);

            if (matchesCriteria) {
                filteredResult.add(current);
            }
        }

        selectionSortByExpiryDate(filteredResult);
        return filteredResult;
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

    private void selectionSortByExpiryDate(ArrayList<PointTransaction> list) { 
        for (int i = 1; i <= list.getNumberOfEntries() - 1; i++) {
            int targetPosition = i;
            PointTransaction targetValue = list.getEntry(i);

            for (int j = i + 1; j <= list.getNumberOfEntries(); j++) {
                PointTransaction current = list.getEntry(j);
                if (current.getExpiryDate().isBefore(targetValue.getExpiryDate())) {
                    targetValue = current;
                    targetPosition = j;
                }
            }

            if (targetPosition != i) {
                PointTransaction temp = list.getEntry(i);
                list.replace(i, targetValue);
                list.replace(targetPosition, temp);
            }
        }
    }

}
