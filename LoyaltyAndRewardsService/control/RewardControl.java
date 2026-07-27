package LoyaltyAndRewardsService.control;

import LoyaltyAndRewardsService.dao.RewardDao;
import LoyaltyAndRewardsService.entity.Reward;
import adt.LinkedList;

/**
 * Manages the rewards kept in memory during the program session.
 *
 * @author Chee Weng
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

    public String createReward(String rewardName, int pointRequired) {
        String rewardId = generateRewardId();
        addReward(new Reward(rewardId, rewardName, pointRequired));
        saveRewards();
        return rewardId;
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

    public boolean removeReward(String rewardId) {
        boolean removed = deleteRewardById(rewardId);
        if (removed) {
            saveRewards();
        }
        return removed;
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

    public boolean updateReward(String rewardId, String rewardName, int pointRequired) {
        boolean updated = updateRewardById(rewardId, rewardName, pointRequired);
        if (updated) {
            saveRewards();
        }
        return updated;
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

    public String getRewardTable() {
        if (rewardList.isEmpty()) {
            return "";
        }

        StringBuilder output = new StringBuilder();
        String border = "+------------+--------------------------------+-----------------+";
        output.append(border).append(System.lineSeparator());
        output.append(String.format("| %-10s | %-30s | %15s |%n",
                "Reward ID", "Reward Name", "Points Required"));
        output.append(border).append(System.lineSeparator());
        for (int i = 1; i <= rewardList.size(); i++) {
            Reward reward = rewardList.getEntry(i);
            output.append(String.format("| %-10.10s | %-30.30s | %15d |%n",
                    reward.getRewardId(), reward.getRewardName(), reward.getPointRequired()));
        }
        output.append(border);
        return output.toString();
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

    public int getRewardPointRequired(String rewardId) {
        Reward reward = getRewardById(rewardId);
        return reward == null ? -1 : reward.getPointRequired();
    }

    public String getRewardName(String rewardId) {
        Reward reward = getRewardById(rewardId);
        return reward == null ? null : reward.getRewardName();
    }

    public void saveRewards() {
        RewardDao.saveToRewardFile(this);
    }
}
