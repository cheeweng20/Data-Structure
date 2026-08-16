package LoyaltyAndRewardsService.dao;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import LoyaltyAndRewardsService.control.LoyaltyServiceControl;
import LoyaltyAndRewardsService.entity.Tier;

/**
 * @author Chee Weng
 */
public class TierDao {
    private static final String FILE_NAME = "LoyaltyAndRewardsService/src/tier.csv";

    // CSV File Reader and Writter
    public static void loadFromTierFile(LoyaltyServiceControl tierLinkedList) {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
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

                    tierLinkedList.addTierLevel(new Tier(
                            tierId, tierLevel, minPoint, maxPoint));
                } catch (RuntimeException exception) {
                    System.out.println("Skipping invalid tier record at line "
                            + lineNumber + ": " + exception.getMessage());
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("No existing tier data found, starting fresh.");
            createTierCSVFile();

        } catch (IOException e) {
            System.out.println("Error reading tier file: " + e.getMessage());
        }
    }

    public static void saveToTierFile(LoyaltyServiceControl tierLinkedList) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            writer.println("TierId,TierLevel,MinPoint,MaxPoint");

            for (int i = 1; i <= tierLinkedList.getTierCount(); i++) {
                Tier tier = tierLinkedList.getTierEntry(i);
                writer.println(tier.toCsvLine());
            }
        } catch (IOException e) {
            System.out.println("Error saving tier file: " + e.getMessage());
        }
    }

    private static void createTierCSVFile() {
        try (PrintWriter writer = new PrintWriter(FILE_NAME)) {
            writer.println("TierId,TierLevel,MinPoint,MaxPoint");

            System.out.println("CSV File created success !");
        } catch (IOException e) {
            System.out.println("Error creating tier file: " + e.getMessage());
        }
    }
}
