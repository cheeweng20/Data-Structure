package LoyaltyAndRewardsService.utility;

import LoyaltyAndRewardsService.control.LoyaltyServiceControl;
import LoyaltyAndRewardsService.entity.Member;
import LoyaltyAndRewardsService.entity.Tier;

/**
 * Provides reusable validation for loyalty and rewards input.
 *
 * @author Chee Weng
 */
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

    public static boolean verifyTierName(String tierName, LoyaltyServiceControl tierLinkedList) {
        return verifyTierName(tierName, null, tierLinkedList);
    }

    public static boolean verifyTierName(String tierName, String excludedTierId,
            LoyaltyServiceControl tierLinkedList) {
        if (tierName == null || tierName.trim().isEmpty()) {
            MessageUI.displayError("Tier name cannot be empty. Please enter a valid tier name.");
            return false;
        }
        for (int i = 1; i <= tierLinkedList.getTierCount(); i++) {
            Tier tier = tierLinkedList.getTierEntry(i);
            boolean isExcludedTier = excludedTierId != null
                    && tier.getTierId().equalsIgnoreCase(excludedTierId);
            if (tier != null && !isExcludedTier && tierName.equalsIgnoreCase(tier.getTierLevel())) {
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

    public static boolean verifyMemberName(String memberName, LoyaltyServiceControl memberLinkedList) {
        return verifyMemberName(memberName, null, memberLinkedList);
    }

    public static boolean verifyMemberName(String memberName, String excludedMemberId,
            LoyaltyServiceControl memberLinkedList) {
        if (memberName == null || memberName.trim().isEmpty()) {
            MessageUI.displayError("Member name cannot be empty. Please enter a valid member name.");
            return false;
        }
        for (int i = 1; i <= memberLinkedList.getMemberCount(); i++) {
            Member member = memberLinkedList.getMemberEntry(i);
            boolean isExcludedMember = excludedMemberId != null
                    && member.getMemberId().equalsIgnoreCase(excludedMemberId);
            if (member != null && !isExcludedMember && memberName.equalsIgnoreCase(member.getName())) {
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
        if (points <= 0) {
            MessageUI.displayError("Reward points must be greater than zero.");
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
