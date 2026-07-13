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

        tier.setTierId(tierLevelName);
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
        System.out.println("+------------+--------------+--------------+--------------+\r\n" + //
                "|  Tier Id   |  Tier Level  |  Min Point   |  Max Point   |\r\n" + //
                "+------------+--------------+--------------+--------------+");

        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier tier = tierLinkedList.getEntry(i);
            String str1 = "|    " + tier.getTierId() + "   |";
            String str2 = tier.getTierLevel();
            String str3 = " | " + tier.getMaxPoint();
            String str4 = "|     " + tier.getMinPoint() + "     |";
            String str5 = "+------------+";
            String str6 = "--------------+";
            String str7 = "--------------+";
            String str8 = "--------------+";
            System.out.printf("%-10s %-12s %-12s %-12s\n", str1, str2, str3, str4);
            System.out.printf("%-10s %-12s %-12s %-12s\n",str5,str6,str7,str8);

        }
    }

    public boolean removeTierLevel(String tierId) {
        for (int i = 1; i < tierLinkedList.size(); i++) {
            if (getExistTierById(tierId) != null) {
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
            if (current.getTierId().equals(tierId))
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
            if (current.getTierId().equals(tierId)) {
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
