package LoyaltyAndRewardsService.utility;

import LoyaltyAndRewardsService.entity.PointTransaction;
import java.util.Comparator;

public class TransactionDateComparator implements Comparator<PointTransaction> {

    @Override
    public int compare(PointTransaction left, PointTransaction right) {
        return left.getEarnedDate().compareTo(right.getEarnedDate());
    }
}
