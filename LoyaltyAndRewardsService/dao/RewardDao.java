package LoyaltyAndRewardsService.dao;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import LoyaltyAndRewardsService.control.LoyaltyServiceControl;
import LoyaltyAndRewardsService.entity.Reward;

/**
 * Loads rewards from and saves rewards to the application's CSV data file.
 *
 * @author Chee Weng
 */
public class RewardDao {
    private static final String FILE_NAME = "LoyaltyAndRewardsService/src/reward.csv";

    public static void loadFromRewardFile(LoyaltyServiceControl rewardList) {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            reader.readLine(); // Skip CSV header.
            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] fields = line.split(",", -1);
                if (fields.length != 3) {
                    System.out.println("Skipping invalid reward record at line "
                            + lineNumber + ": expected 3 columns");
                    continue;
                }

                try {
                    String rewardId = fields[0].trim();
                    String rewardName = fields[1].trim();
                    int pointRequired = Integer.parseInt(fields[2].trim());
                    if (rewardId.isEmpty() || rewardName.isEmpty() || pointRequired <= 0) {
                        throw new IllegalArgumentException("invalid reward value");
                    }
                    rewardList.addReward(new Reward(rewardId, rewardName, pointRequired));
                } catch (RuntimeException exception) {
                    System.out.println("Skipping invalid reward record at line "
                            + lineNumber + ": " + exception.getMessage());
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("No existing reward data found, starting fresh.");
            createRewardCSVFile();
        } catch (IOException e) {
            System.out.println("Error reading reward file: " + e.getMessage());
        }
    }

    public static void saveToRewardFile(LoyaltyServiceControl rewardList) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            writer.println("RewardId,RewardName,PointRequired");
            for (int i = 1; i <= rewardList.getRewardCount(); i++) {
                writer.println(rewardList.getRewardEntry(i).toCsvLine());
            }
        } catch (IOException e) {
            System.out.println("Error saving reward file: " + e.getMessage());
        }
    }

    private static void createRewardCSVFile() {
        try (PrintWriter writer = new PrintWriter(FILE_NAME)) {
            writer.println("RewardId,RewardName,PointRequired");
            System.out.println("Reward CSV file created successfully!");
        } catch (IOException e) {
            System.out.println("Error creating reward file: " + e.getMessage());
        }
    }
}
