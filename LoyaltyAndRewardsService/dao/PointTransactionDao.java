package LoyaltyAndRewardsService.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;

import LoyaltyAndRewardsService.entity.PointTransaction;
import adt.LinkedList;
import adt.ListInterface;

/**
 * @author Chee Weng
 */
public class PointTransactionDao {
    private static final String DEFAULT_FILE_NAME =
            "LoyaltyAndRewardsService/src/transaction.csv";
    private static final String HEADER =
            "TransactionId,MemberId,PointsEarned,PointsRemaining,EarnedDate,ExpiryDate,SourceReference";
    private final String fileName;

    public PointTransactionDao() {
        this(DEFAULT_FILE_NAME);
    }

    public PointTransactionDao(String fileName) {
        this.fileName = fileName;
    }

    public ListInterface<PointTransaction> retrieveFromFile() {
        ListInterface<PointTransaction> transactions = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
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
                    if (fields.length != 7) {
                        throw new IllegalArgumentException("expected 7 columns");
                    }

                    String transactionId = fields[0].trim();
                    String memberId = fields[1].trim();
                    int pointsEarned = Integer.parseInt(fields[2].trim());
                    int pointsRemaining = Integer.parseInt(fields[3].trim());
                    LocalDate earnedDate = LocalDate.parse(fields[4].trim());
                    LocalDate expiryDate = LocalDate.parse(fields[5].trim());
                    String sourceReference = fields[6].trim();

                    if (transactionId.isEmpty() || memberId.isEmpty()
                            || pointsEarned < 0 || pointsRemaining < 0
                            || pointsRemaining > pointsEarned
                            || expiryDate.isBefore(earnedDate)) {
                        throw new IllegalArgumentException("invalid transaction value");
                    }

                    transactions.add(new PointTransaction(transactionId, memberId, pointsEarned,
                            pointsRemaining, earnedDate, expiryDate, sourceReference));
                } catch (RuntimeException exception) {
                    System.out.println("Skipping invalid transaction record at line "
                            + lineNumber + ": " + exception.getMessage());
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("No existing transaction data found, starting fresh.");
            createTransactionCSVFile();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read transaction file.", e);
        }
        return transactions;
    }

    public void saveToFile(ListInterface<PointTransaction> transactions) {
        createParentDirectory();
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println(HEADER);
            for (PointTransaction transaction : transactions) {
                writer.println(transaction.toCsvLine());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save transaction file.", e);
        }
    }

    private void createTransactionCSVFile() {
        createParentDirectory();
        try (PrintWriter writer = new PrintWriter(fileName)) {
            writer.println(HEADER);
            System.out.println("CSV File created success !");
        } catch (IOException e) {
            throw new IllegalStateException("Unable to create transaction file.", e);
        }
    }

    private void createParentDirectory() {
        File file = new File(fileName);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create transaction data directory.");
        }
    }
}
