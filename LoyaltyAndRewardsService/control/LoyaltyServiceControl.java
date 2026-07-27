package LoyaltyAndRewardsService.control;

import LoyaltyAndRewardsService.dao.MemberDao;
import LoyaltyAndRewardsService.dao.PointTransactionDao;
import LoyaltyAndRewardsService.dao.RequestDao;
import LoyaltyAndRewardsService.dao.RewardDao;
import LoyaltyAndRewardsService.dao.TierDao;

/**
 * Initializes and coordinates the Loyalty and Rewards subsystem.
 *
 * @author Chee Weng
 */
public class LoyaltyServiceControl {
    private TierControl tierControl;
    private MemberControl memberControl;
    private TransactionControl transactionControl;
    private RequestControl requestControl;
    private RewardControl rewardControl;
    private ReportControl reportControl;

    public LoyaltyServiceControl() {
        tierControl = new TierControl();
        TierDao.loadFromTierFile(tierControl);

        memberControl = new MemberControl(tierControl);
        MemberDao.loadFromMemberFile(memberControl);

        transactionControl = new TransactionControl();
        PointTransactionDao.loadFromTransactionFile(transactionControl);

        requestControl = new RequestControl(memberControl, transactionControl);
        RequestDao.loadFromRequestFile(requestControl);

        rewardControl = new RewardControl();
        RewardDao.loadFromRewardFile(rewardControl);

        reportControl = new ReportControl(
                memberControl, tierControl, transactionControl, requestControl);
    }

    public MemberControl getMemberControl() {
        return memberControl;
    }

    public TierControl getTierControl() {
        return tierControl;
    }

    public TransactionControl getTransactionControl() {
        return transactionControl;
    }

    public RequestControl getRequestControl() {
        return requestControl;
    }

    public RewardControl getRewardControl() {
        return rewardControl;
    }

    public ReportControl getReportControl() {
        return reportControl;
    }

    public void saveAll() {
        memberControl.saveMembers();
        TierDao.saveToTierFile(tierControl);
        transactionControl.saveTransactions();
        requestControl.saveRequests();
        rewardControl.saveRewards();
    }
}
