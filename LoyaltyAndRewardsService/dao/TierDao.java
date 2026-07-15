package LoyaltyAndRewardsService.dao;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import LoyaltyAndRewardsService.control.TierControl;
import LoyaltyAndRewardsService.entity.Tier;

public class TierDao {
    private static final String FILE_NAME = "LoyaltyAndRewardsService/src/tier.csv";

    // CSV File Reader and Writter
    public static void loadFromTierFile(TierControl tierLinkedList) {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            reader.readLine(); // Skip file header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");

                String tierId = fields[0];
                String tierLevel = fields[1];
                int minPoint = Integer.parseInt(fields[2]);
                int maxPoint = Integer.parseInt(fields[3]);

                tierLinkedList.addTierLevel(new Tier(tierId, tierLevel, minPoint, maxPoint));
            }
        } catch (FileNotFoundException e) {
            System.out.println("No existing tier data found, starting fresh.");
            createTierCSVFile();

        } catch (IOException e) {
            System.out.println("Error reading tier file: " + e.getMessage());
        }
    }

    public static void saveToTierFile(TierControl tierLinkedList) {
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

    private static void createTierCSVFile() {
        try (PrintWriter writer = new PrintWriter(FILE_NAME)) {
            writer.println("TierId,TierLevel,MinPoint,MaxPoint");

            System.out.println("CSV File created success !");
        } catch (IOException e) {
            System.out.println("Error creating tier file: " + e.getMessage());
        }
    }
}
