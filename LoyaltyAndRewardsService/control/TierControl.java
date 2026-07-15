package LoyaltyAndRewardsService.control;

import adt.LinkedList;

import LoyaltyAndRewardsService.entity.Tier;

public class TierControl {
    private LinkedList<Tier> tierLinkedList;

    public TierControl() {
        tierLinkedList = new LinkedList<>();
    }

    public void addTierLevel(Tier tier) {
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
        System.out.printf("%-10s %-12s %-12s %-12s\n", "Tier Id", "Tier Level", "Min Point", "Max Point");

        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier tier = tierLinkedList.getEntry(i);
            System.out.printf("%-10s %-12s %-12d %-12d\n", tier.getTierId(), tier.getTierLevel(), tier.getMinPoint(),
                    tier.getMaxPoint());

        }
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
                return current.getTierId();
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
}
