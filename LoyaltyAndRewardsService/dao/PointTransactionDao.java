package LoyaltyAndRewardsService.dao;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;

import LoyaltyAndRewardsService.control.TransactionControl;
import LoyaltyAndRewardsService.entity.PointTransaction;

public class PointTransactionDao {
    private static final String FILE_NAME = "LoyaltyAndRewardsService/src/transaction.csv";
    private static final String HEADER = "TransactionId,MemberId,PointsEarned,EarnedDate,ExpiryDate";

    public static void loadFromTransactionFile(TransactionControl transactionList) {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            reader.readLine();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }

                String[] fields = line.split(",", -1);
                if (fields.length < 5) {
                    continue;
                }

                String transactionId = fields[0];
                String memberId = fields[1];
                int pointsEarned = Integer.parseInt(fields[2]);
                LocalDate earnedDate = LocalDate.parse(fields[3]);
                LocalDate expiryDate = LocalDate.parse(fields[4]);

                transactionList.addTransaction(
                        new PointTransaction(transactionId, memberId, pointsEarned, earnedDate, expiryDate));
            }
        } catch (FileNotFoundException e) {
            System.out.println("No existing transaction data found, starting fresh.");
            createTransactionCSVFile();
        } catch (IOException e) {
            System.out.println("Error reading transaction file: " + e.getMessage());
        }
    }

    public static void saveToTransactionFile(TransactionControl transactionList) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            writer.println(HEADER);
            for (int i = 1; i <= transactionList.size(); i++) {
                PointTransaction transaction = transactionList.getEntry(i);
                writer.println(transaction.toCsvLine());
            }
        } catch (IOException e) {
            System.out.println("Error saving transaction file: " + e.getMessage());
        }
    }

    public static void loadFromMemberFile(TransactionControl transactionList) {
        loadFromTransactionFile(transactionList);
    }

    public static void saveToMemberFile(TransactionControl transactionList) {
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
