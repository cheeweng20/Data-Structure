package LoyaltyAndRewardsService.utility;

import LoyaltyAndRewardsService.control.MemberControl;
import LoyaltyAndRewardsService.control.TierControl;
import LoyaltyAndRewardsService.entity.Member;
import LoyaltyAndRewardsService.entity.Tier;

public class Verification {
    public static boolean verifyMemberPoint(int point) {
        if (point < 0) {
            MessageUI.displayError("Points cannot be negative. Please enter a valid point value.");
            return false;
        }
        return true;
    }

    public static boolean verifyTierPoints(int minPoint, int maxPoint) {
        if (minPoint < 0 || maxPoint < 0) {
            MessageUI.displayError("Points cannot be negative. Please enter valid point values.");
            return false;
        }
        if (maxPoint != 0 && minPoint >= maxPoint) {
            MessageUI.displayError("Min Point must be less than Max Point. Please enter valid point values.");
            return false;
        }
        return true;
    }

    public static boolean verifyTierName(String tierName, TierControl tierLinkedList) {
        if (tierName == null || tierName.trim().isEmpty()) {
            MessageUI.displayError("Tier name cannot be empty. Please enter a valid tier name.");
            return false;
        }
        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier tier = tierLinkedList.getEntry(i);
            if (tier != null && tierName.equals(tier.getTierLevel())) {
                MessageUI.displayError("Tier name already exists. Please enter a different tier name.");
                return false;
            }
        }

        if (tierName.length() < 3 || tierName.length() > 20) {
            MessageUI.displayError("Tier name must be between 3 and 20 characters. Please enter a valid tier name.");
            return false;
        }
        return true;
    }

    public static boolean verifyMemberName(String memberName, MemberControl memberLinkedList) {
        if (memberName == null || memberName.trim().isEmpty()) {
            MessageUI.displayError("Member name cannot be empty. Please enter a valid member name.");
            return false;
        }
        for (int i = 1; i <= memberLinkedList.size(); i++) {
            Member member = memberLinkedList.getEntry(i);
            if (member != null && memberName.equals(member.getName())) {
                MessageUI.displayError("Member name already exists. Please enter a different member name.");
                return false;
            }
        }

        if (memberName.length() < 3 || memberName.length() > 20) {
            MessageUI
                    .displayError("Member name must be between 3 and 20 characters. Please enter a valid member name.");
            return false;
        }
        return true;
    }

    public static boolean verifyRewardPoints(int points) {
        if (points < 0) {
            MessageUI.displayError("Reward points cannot be negative. Please enter a valid point value.");
            return false;
        }
        return true;
    }

    public static boolean verifyRewardName(String rewardName) {
        if (rewardName == null || rewardName.trim().isEmpty()) {
            MessageUI.displayError("Reward name cannot be empty. Please enter a valid reward name.");
            return false;
        }
        if (rewardName.length() < 3 || rewardName.length() > 20) {
            MessageUI.displayError("Reward name must be between 3 and 20 characters. Please enter a valid reward name.");
            return false;
        }
        return true;
    }
}
