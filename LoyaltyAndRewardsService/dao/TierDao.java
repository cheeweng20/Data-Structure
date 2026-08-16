package LoyaltyAndRewardsService.dao;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import LoyaltyAndRewardsService.entity.Tier;
import adt.LinkedList;
import adt.ListInterface;

/**
 * @author Chee Weng
 */
public class TierDao {
    private static final String DEFAULT_FILE_NAME = "LoyaltyAndRewardsService/src/tier.csv";
    private final String fileName;

    public TierDao() {
        this(DEFAULT_FILE_NAME);
    }

    public TierDao(String fileName) {
        this.fileName = fileName;
    }

    // Loads fixed tier definitions used for automatic tier progression.
    public ListInterface<Tier> retrieveFromFile() {
        ListInterface<Tier> tiers = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            reader.readLine(); // Skip file header
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    String[] fields = line.split(",", -1);
                    if (fields.length != 4) {
                        throw new IllegalArgumentException("expected 4 columns");
                    }

                    String tierId = fields[0].trim();
                    String tierLevel = fields[1].trim();
                    int minPoint = Integer.parseInt(fields[2].trim());
                    int maxPoint = Integer.parseInt(fields[3].trim());
                    if (tierId.isEmpty() || tierLevel.isEmpty() || minPoint < 0
                            || maxPoint < 0 || (maxPoint != 0 && maxPoint < minPoint)) {
                        throw new IllegalArgumentException("invalid tier value");
                    }

                    tiers.add(new Tier(tierId, tierLevel, minPoint, maxPoint));
                } catch (RuntimeException exception) {
                    System.out.println("Skipping invalid tier record at line "
                            + lineNumber + ": " + exception.getMessage());
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("No existing tier data found, creating default tiers.");
            createDefaultTierFile();
            return retrieveFromFile();

        } catch (IOException e) {
            System.out.println("Error reading tier file: " + e.getMessage());
        }
        return tiers;
    }

    private void createDefaultTierFile() {
        try (PrintWriter writer = new PrintWriter(fileName)) {
            writer.println("TierId,TierLevel,MinPoint,MaxPoint");
            writer.println("T003,Classic,0,199");
            writer.println("T002,Silver,200,499");
            writer.println("T001,Gold,500,700");
            writer.println("T004,Platinum,701,0");

            System.out.println("CSV File created success !");
        } catch (IOException e) {
            System.out.println("Error creating tier file: " + e.getMessage());
        }
    }
}
