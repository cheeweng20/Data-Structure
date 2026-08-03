package LoyaltyAndRewardsService.control;

import adt.LinkedList;
import adt.SortedArrayList;

import LoyaltyAndRewardsService.dao.MemberDao;
import LoyaltyAndRewardsService.dao.TierDao;
import LoyaltyAndRewardsService.entity.Tier;
import LoyaltyAndRewardsService.utility.MessageUI;

/**
 * @author Chee Weng
 */
public class TierControl {
    private LinkedList<Tier> tierLinkedList;

    public TierControl() {
        tierLinkedList = new LinkedList<>();
    }

    public void addTierLevel(Tier tier) {
        tierLinkedList.add(tier);
        organizeTierRanges();
    }

    public boolean createTier(String tierLevelName, int minPoint, MemberControl memberControl) {
        if ((isEmpty() && minPoint != 0) || !isMinimumPointAvailable(minPoint, null)) {
            MessageUI.displayError("Tier level could not be added.");
            return false;
        }

        String tierId = generateTierId();
        addTierLevel(new Tier(tierId, tierLevelName, minPoint, 0));
        int updatedMembers = persistTierChanges(memberControl);
        MessageUI.displayTierAdded(tierId);
        MessageUI.displayTierRecalculation(updatedMembers);
        return true;
    }

    public boolean updateTierLevelById(String tierId, String tierLevelName, int minPoint, int maxPoint) {
        Tier tier = getExistTierById(tierId);

        if (tier == null || !isMinimumPointAvailable(minPoint, tierId))
            return false;

        tier.setTierLevel(tierLevelName);
        tier.setMinPoint(minPoint);
        tier.setMaxPoint(maxPoint);
        organizeTierRanges();

        return true;
    }

    public int size() {
        return tierLinkedList.size();
    }

    public boolean isEmpty() {
        return tierLinkedList.isEmpty();
    }

    public boolean findTier(String tierId) {
        return getExistTierById(tierId) != null;
    }

    public Tier getEntry(int position) {
        return tierLinkedList.getEntry(position);
    }

    public String getTierTable() {
        if (tierLinkedList.isEmpty()) {
            return "";
        }

        StringBuilder output = new StringBuilder();
        String border = "+------------+----------------------+------------+------------+";
        output.append(border).append(System.lineSeparator());
        output.append(String.format("| %-10s | %-20s | %10s | %10s |%n",
                "Tier ID", "Tier Level", "Min Points", "Max Points"));
        output.append(border).append(System.lineSeparator());
        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier tier = tierLinkedList.getEntry(i);
            String maxPoints = tier.getMaxPoint() == 0 ? "No limit" : String.valueOf(tier.getMaxPoint());
            output.append(String.format("| %-10.10s | %-20.20s | %10d | %10s |%n",
                    tier.getTierId(), tier.getTierLevel(), tier.getMinPoint(), maxPoints));
        }
        output.append(border);
        return output.toString();
    }

    public boolean removeTierLevel(String tierId) {
        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier tier = tierLinkedList.getEntry(i);
            if (tier.getTierId().equalsIgnoreCase(tierId)) {
                tierLinkedList.remove(i);
                organizeTierRanges();
                return true;
            }
        }
        return false;
    }

    public boolean removeTier(String tierId, MemberControl memberControl) {
        Tier tier = getExistTierById(tierId);
        if (tier == null || (size() > 1 && tier.getMinPoint() == 0)) {
            MessageUI.displayError("Tier level could not be deleted.");
            return false;
        }

        removeTierLevel(tierId);
        int updatedMembers = persistTierChanges(memberControl);
        MessageUI.displayTierDeleted();
        MessageUI.displayTierRecalculation(updatedMembers);
        return true;
    }

    public boolean updateTier(String tierId, String tierLevelName, int minPoint,
            MemberControl memberControl) {
        Tier existing = getExistTierById(tierId);
        if (existing == null
                || (existing.getMinPoint() == 0 && minPoint != 0)
                || !isMinimumPointAvailable(minPoint, tierId)
                || !updateTierLevelById(tierId, tierLevelName, minPoint, 0)) {
            MessageUI.displayError("Tier level could not be updated.");
            return false;
        }

        int updatedMembers = persistTierChanges(memberControl);
        MessageUI.displayTierUpdated();
        MessageUI.displayTierRecalculation(updatedMembers);
        return true;
    }

    // Helper Function
    public Tier getExistTierById(String tierId) {
        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier current = tierLinkedList.getEntry(i);
            if (current.getTierId().equalsIgnoreCase(tierId))
                return current;
        }

        return null;
    }

    public String getTierIdByPoint(int point) {
        Tier matchingTier = null;

        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier current = tierLinkedList.getEntry(i);
            if (point >= current.getMinPoint()) {
                matchingTier = current;
            } else {
                break;
            }
        }

        return matchingTier == null ? null : matchingTier.getTierId();
    }

    public String getTierNameById(String tierId) {
        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier current = tierLinkedList.getEntry(i);
            if (current.getTierId().equalsIgnoreCase(tierId)) {
                return current.getTierLevel();
            }
        }

        return "Unknown";
    }

    public Tier getNextTier(String tierId) {
        for (int i = 1; i <= tierLinkedList.size(); i++) {
            if (tierLinkedList.getEntry(i).getTierId().equalsIgnoreCase(tierId)) {
                return i < tierLinkedList.size() ? tierLinkedList.getEntry(i + 1) : null;
            }
        }
        return null;
    }

    public String generateTierId() {
        int highestNumber = 0;
        for (int i = 1; i <= tierLinkedList.size(); i++) {
            String tierId = tierLinkedList.getEntry(i).getTierId();
            if (tierId.length() > 1 && (tierId.charAt(0) == 'T' || tierId.charAt(0) == 't')) {
                try {
                    highestNumber = Math.max(highestNumber, Integer.parseInt(tierId.substring(1)));
                } catch (NumberFormatException ignored) {
                    // Ignore non-standard IDs while finding the next generated ID.
                }
            }
        }
        return String.format("T%03d", highestNumber + 1);
    }

    public Tier getHighestTier(String tierId) {
        return tierLinkedList.isEmpty() ? null : tierLinkedList.getEntry(tierLinkedList.size());
    }

    public boolean isMinimumPointAvailable(int minPoint, String excludedTierId) {
        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier current = tierLinkedList.getEntry(i);
            boolean isExcludedTier = excludedTierId != null
                    && current.getTierId().equalsIgnoreCase(excludedTierId);
            if (!isExcludedTier && current.getMinPoint() == minPoint) {
                return false;
            }
        }
        return true;
    }

    public int getMinimumPoint(String tierId) {
        Tier tier = getExistTierById(tierId);
        return tier == null ? -1 : tier.getMinPoint();
    }

    public boolean isBaseTier(String tierId) {
        return getMinimumPoint(tierId) == 0;
    }

    private int persistTierChanges(MemberControl memberControl) {
        int updatedMembers = memberControl.recalculateAllMemberTiers();
        TierDao.saveToTierFile(this);
        MemberDao.saveToMemberFile(memberControl);
        return updatedMembers;
    }

    private void organizeTierRanges() {
        sortByMinimumPoint();

        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier current = tierLinkedList.getEntry(i);
            if (i < tierLinkedList.size()) {
                Tier next = tierLinkedList.getEntry(i + 1);
                current.setMaxPoint(next.getMinPoint() - 1);
            } else {
                current.setMaxPoint(0);
            }
        }
    }

    private void sortByMinimumPoint() {
        SortedArrayList<Tier> sortedTiers = new SortedArrayList<>();
        for (Tier tier : tierLinkedList) {
            sortedTiers.add(tier);
        }

        tierLinkedList.clear();
        for (Tier tier : sortedTiers) {
            tierLinkedList.add(tier);
        }
    }

}
