import adt.LinkedList;
import java.io.*;

public class TierControl {
    private LinkedList<Tier> tierLinkedList;

    // File name
    private static final String FILE_NAME = "src/tier.csv";

    public TierControl() {
        tierLinkedList = new LinkedList<>();
    }

    public void addTierLevel(Tier tier) {
        tierLinkedList.add(tier);
    }

    public boolean updateTierLevelById(String tierId, String tierLevelName, int minPoint, int maxPoint) {
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

    public boolean isEmpty() {
        return tierLinkedList.size() == 0;
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

    // CSV File Reader and Writter
    public void loadFromTierFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            reader.readLine(); // Skip file header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");

                String tierId = fields[0];
                String tierLevel = fields[1];
                int maxPoint = Integer.parseInt(fields[2]);
                int minPoint = Integer.parseInt(fields[3]);

                tierLinkedList.add(new Tier(tierId, tierLevel, maxPoint, minPoint));
            }
        } catch (FileNotFoundException e) {
            System.out.println("No existing tier data found, starting fresh.");
            createTierCSVFile();

        } catch (IOException e) {
            System.out.println("Error reading tier file: " + e.getMessage());
        }
    }

    public void saveToTierFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            writer.println("TierId,TierLevel,MinPoint,MaxPoint");

            for (int i = 1; i <= tierLinkedList.size(); i++) {
                Tier tier = tierLinkedList.getEntry(i);
                writer.println(tier.toCsvLine());
            }
        } catch (IOException e) {
            System.out.println("Error saving tier file: " + e.getMessage());
        }
    }

    public void createTierCSVFile() {
        try (PrintWriter writer = new PrintWriter(FILE_NAME)) {
            writer.println("TierId,TierLevel,MinPoint,MaxPoint");

            System.out.println("CSV File created success !");
        } catch (IOException e) {
            System.out.println("Error creating tier file: " + e.getMessage());
        }
    }
}
