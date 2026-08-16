package LoyaltyAndRewardsService.dao;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;

import LoyaltyAndRewardsService.control.LoyaltyServiceControl;
import LoyaltyAndRewardsService.entity.PointTransaction;

/**
 * @author Chee Weng
 */
public class PointTransactionDao {
    private static final String FILE_NAME = "LoyaltyAndRewardsService/src/transaction.csv";
    private static final String HEADER =
            "TransactionId,MemberId,PointsEarned,PointsRemaining,EarnedDate,ExpiryDate";

    public static void loadFromTransactionFile(LoyaltyServiceControl transactionList) {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            reader.readLine();
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isEmpty()) {
                    continue;
                }

                try {
                    String[] fields = line.split(",", -1);
                    if (fields.length < 5) {
                        throw new IllegalArgumentException("expected at least 5 columns");
                    }

                    String transactionId = fields[0].trim();
                    String memberId = fields[1].trim();
                    int pointsEarned = Integer.parseInt(fields[2].trim());
                    boolean hasRemainingPoints = fields.length >= 6;
                    int pointsRemaining = hasRemainingPoints
                            ? Integer.parseInt(fields[3].trim()) : pointsEarned;
                    LocalDate earnedDate = LocalDate.parse(
                            fields[hasRemainingPoints ? 4 : 3].trim());
                    LocalDate expiryDate = LocalDate.parse(
                            fields[hasRemainingPoints ? 5 : 4].trim());

                    if (transactionId.isEmpty() || memberId.isEmpty()
                            || pointsEarned < 0 || pointsRemaining < 0
                            || pointsRemaining > pointsEarned
                            || expiryDate.isBefore(earnedDate)) {
                        throw new IllegalArgumentException("invalid transaction value");
                    }

                    transactionList.addTransaction(
                            new PointTransaction(transactionId, memberId, pointsEarned,
                                    pointsRemaining, earnedDate, expiryDate));
                } catch (RuntimeException exception) {
                    System.out.println("Skipping invalid transaction record at line "
                            + lineNumber + ": " + exception.getMessage());
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("No existing transaction data found, starting fresh.");
            createTransactionCSVFile();
        } catch (IOException e) {
            System.out.println("Error reading transaction file: " + e.getMessage());
        }
    }

    public static void saveToTransactionFile(LoyaltyServiceControl transactionList) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            writer.println(HEADER);
            for (int i = 1; i <= transactionList.getTransactionCount(); i++) {
                PointTransaction transaction = transactionList.getTransactionEntry(i);
                writer.println(transaction.toCsvLine());
            }
        } catch (IOException e) {
            System.out.println("Error saving transaction file: " + e.getMessage());
        }
    }

    public static void loadFromMemberFile(LoyaltyServiceControl transactionList) {
        loadFromTransactionFile(transactionList);
    }

    public static void saveToMemberFile(LoyaltyServiceControl transactionList) {
        saveToTransactionFile(transactionList);
    }

    private static void createTransactionCSVFile() {
        try (PrintWriter writer = new PrintWriter(FILE_NAME)) {
            writer.println(HEADER);
            System.out.println("CSV File created success !");
        } catch (IOException e) {
            System.out.println("Error creating transaction file: " + e.getMessage());
        }
    }
}
