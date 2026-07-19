package LoyaltyAndRewardsService.control;

import adt.LinkedList;

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
        if (tier.getMaxPoint() == 0) {
            Tier currentHighestTier = getCurrentHighestTier();

            if (currentHighestTier != null) {
                currentHighestTier.setMaxPoint(tier.getMinPoint() - 1);
                updateTierLevelById(currentHighestTier.getTierId(), currentHighestTier.getTierLevel(),
                        currentHighestTier.getMinPoint(), currentHighestTier.getMaxPoint());
            }
        }
        tierLinkedList.add(tier);
    }

    public boolean updateTierLevelById(String tierId, String tierLevelName, int minPoint, int maxPoint) {
        Tier tier = getExistTierById(tierId);

        if (tier == null)
            return false;

        tier.setTierLevel(tierLevelName);
        tier.setMinPoint(minPoint);
        tier.setMaxPoint(maxPoint);

        return true;
    }

    public int size() {
        return tierLinkedList.size();
    }

    public boolean isEmpty() {
        return tierLinkedList.size() == 0;
    }

    public boolean findTier(String tierId) {
        return getExistTierById(tierId) != null;
    }

    public Tier getEntry(int position) {
        return tierLinkedList.getEntry(position);
    }

    public void displayAllTierLevel() {
        if (tierLinkedList.isEmpty()) {
            MessageUI.displayInfo("No tier records found.");
            return;
        }

        String border = "+------------+----------------------+------------+------------+";
        System.out.println(border);
        System.out.printf("| %-10s | %-20s | %10s | %10s |%n", "Tier ID", "Tier Level", "Min Points", "Max Points");
        System.out.println(border);
        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier tier = tierLinkedList.getEntry(i);
            String maxPoints = tier.getMaxPoint() == 0 ? "No limit" : String.valueOf(tier.getMaxPoint());
            System.out.printf("| %-10.10s | %-20.20s | %10d | %10s |%n", tier.getTierId(), tier.getTierLevel(),
                    tier.getMinPoint(), maxPoints);

        }
        System.out.println(border);
    }

    public boolean removeTierLevel(String tierId) {
        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier tier = tierLinkedList.getEntry(i);
            if (tier.getTierId().equalsIgnoreCase(tierId)) {
                tierLinkedList.remove(i);
                return true;
            }
        }
        return false;
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
        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier current = tierLinkedList.getEntry(i);
            if (point >= current.getMinPoint() && (current.getMaxPoint() == 0 || point <= current.getMaxPoint()))
                return current.getTierId();
        }

        return null;
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
        Tier currentTier = getExistTierById(tierId);

        if (currentTier == null || currentTier.getMaxPoint() == 0) // Top Tier
            return null;

        Tier nextTier = null;

        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier candidate = tierLinkedList.getEntry(i);

            if (candidate.getTierId().equals(currentTier.getTierId()))
                continue;

            if (candidate.getMinPoint() >= currentTier.getMaxPoint()) {

                if (nextTier == null || candidate.getMinPoint() < nextTier.getMinPoint()) {
                    nextTier = candidate;
                }
            }
        }

        return nextTier;
    }

    public String generateTierId() {
        if (tierLinkedList.isEmpty()) {
            return "T001";
        }

        Tier lastTierId = tierLinkedList.getEntry(tierLinkedList.size());

        String lastId = lastTierId.getTierId();

        int number = Integer.parseInt(lastId.substring(1));

        number++;

        return String.format("T%03d", number);
    }

    public Tier getHighestTier(String tierId) {
        Tier currentTier = getExistTierById(tierId);

        Tier nextTier = null;

        if (currentTier == null || currentTier.getMaxPoint() == 0) // Top Tier
            return currentTier;

        else {
            for (int i = 1; i <= tierLinkedList.size(); i++) {
                Tier candidate = tierLinkedList.getEntry(i);

                if (candidate.getTierId().equals(currentTier.getTierId()))
                    continue;

                if (candidate.getMinPoint() >= currentTier.getMaxPoint()) {

                    if (nextTier == null || candidate.getMinPoint() < nextTier.getMinPoint()) {
                        nextTier = candidate;
                    }
                }
            }
        }

        return nextTier;
    }

    private Tier getCurrentHighestTier() {
        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier current = tierLinkedList.getEntry(i);

            if (current.getMaxPoint() == 0) {
                return current;
            }
        }

        if (tierLinkedList.isEmpty()) {
            return null;
        }

        return tierLinkedList.getEntry(tierLinkedList.size());
    }
}
