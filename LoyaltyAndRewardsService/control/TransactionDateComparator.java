package LoyaltyAndRewardsService.control;

import LoyaltyAndRewardsService.entity.PointTransaction;
import java.util.Comparator;

/**
 * Compares point transactions by earned date for Loyalty control operations.
 *
 * @author Chee Weng
 */
final class TransactionDateComparator implements Comparator<PointTransaction> {

    @Override
    public int compare(PointTransaction left, PointTransaction right) {
        return left.getEarnedDate().compareTo(right.getEarnedDate());
    }
}
