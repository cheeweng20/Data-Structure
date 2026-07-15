package LoyaltyAndRewardsService.control;

import LoyaltyAndRewardsService.entity.Reward;
import adt.LinkedList;

/**
 * Manages the rewards kept in memory during the program session.
 */
public class RewardControl {
    private LinkedList<Reward> rewardList;

    public RewardControl() {
        rewardList = new LinkedList<>();
    }

    public int size() {
        return rewardList.size();
    }

    public boolean isEmpty() {
        return rewardList.isEmpty();
    }

    public void addReward(Reward reward) {
        rewardList.add(reward);
    }

    public Reward getEntry(int position) {
        return rewardList.getEntry(position);
    }

    public boolean findReward(String rewardId) {
        return getRewardById(rewardId) != null;
    }

    public boolean deleteRewardById(String rewardId) {
        for (int i = 1; i <= rewardList.size(); i++) {
            if (rewardList.getEntry(i).getRewardId().equalsIgnoreCase(rewardId)) {
                rewardList.remove(i);
                return true;
            }
        }
        return false;
    }

    public boolean updateRewardById(String rewardId, String rewardName, int pointRequired) {
        Reward reward = getRewardById(rewardId);
        if (reward == null) {
            return false;
        }

        reward.setRewardName(rewardName);
        reward.setPointRequired(pointRequired);
        return true;
    }

    public Reward getRewardById(String rewardId) {
        for (int i = 1; i <= rewardList.size(); i++) {
            Reward reward = rewardList.getEntry(i);
            if (reward.getRewardId().equalsIgnoreCase(rewardId)) {
                return reward;
            }
        }
        return null;
    }

    public void displayAllRewards() {
        System.out.printf("%-12s %-30s %-15s%n", "Reward Id", "Reward Name", "Points Required");
        for (int i = 1; i <= rewardList.size(); i++) {
            Reward reward = rewardList.getEntry(i);
            System.out.printf("%-12s %-30s %-15d%n", reward.getRewardId(), reward.getRewardName(),
                    reward.getPointRequired());
        }
    }

    public String generateRewardId() {
        int highestNumber = 0;

        for (int i = 1; i <= rewardList.size(); i++) {
            String rewardId = rewardList.getEntry(i).getRewardId();
            if (rewardId.length() > 1 && (rewardId.charAt(0) == 'R' || rewardId.charAt(0) == 'r')) {
                try {
                    highestNumber = Math.max(highestNumber, Integer.parseInt(rewardId.substring(1)));
                } catch (NumberFormatException ignored) {
                    // Ignore non-standard IDs while finding the next generated ID.
                }
            }
        }

        return String.format("R%03d", highestNumber + 1);
    }
}
