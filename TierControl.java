import adt.LinkedList;

public class TierControl {
    private LinkedList<Tier> tierLinkedList;

    public TierControl() {
        tierLinkedList = new LinkedList<>();
    }

    public void addTierLevel(Tier tier) {
        tierLinkedList.add(tier);
    }

    public boolean updateTierLevelById(String tierId, String tierLevelName, int minPoint,int maxPoint) {
        Tier tier = getTierById(tierId);

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

    public boolean findTier(String tierId) {
        return getTierById(tierId) != null;
    }

    public void displayAllTierLevel() {
        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier tier = tierLinkedList.getEntry(i);
            System.out.printf("%s\t%s\t%s\t%s\n", "Tier Id", "Tier Level Name", "Min Point", "Max Point");
            System.out.printf("%s\t%s\t%05d\t%05d\n", tier.getTierId(), tier.getTierLevel(), tier.getMinPoint(),
                    tier.getMaxPoint());
        }
    }

    public boolean removeTierLevel(String tierId) {
        for (int i = 1; i < tierLinkedList.size(); i++) {
            if (getTierById(tierId) != null) {
                tierLinkedList.remove(i);
                return true;
            }
        }
        return false;
    }

    // Helper Function
    private Tier getTierById(String tierId) {
        for (int i = 1; i <= tierLinkedList.size(); i++) {
            Tier current = tierLinkedList.getEntry(i);
            if (current.getTierId().equals(tierId))
                return current;
        }

        return null;
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
