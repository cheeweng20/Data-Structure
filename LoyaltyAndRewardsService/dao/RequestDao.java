package LoyaltyAndRewardsService.dao;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Iterator;

import LoyaltyAndRewardsService.control.LoyaltyServiceControl;
import LoyaltyAndRewardsService.entity.RedemptionRequest;

/**
 * @author Chee Weng
 */
public class RequestDao {
    private static final String FILE_NAME = "LoyaltyAndRewardsService/src/requests.csv";

    public static void saveToRequestFile(LoyaltyServiceControl requestControl) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            writer.println("RequestId,MemberId,RewardId,PointsRequested,RequestDate,Status");

            Iterator<RedemptionRequest> it = requestControl.getRequestIterator();
            while (it.hasNext()) {
                RedemptionRequest request = it.next();
                writer.println(request.toCsvLine());
            }
        } catch (IOException e) {
            System.out.println("Error saving request file: " + e.getMessage());
        }
    }

    public static void loadFromRequestFile(LoyaltyServiceControl requestControl) {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            reader.readLine(); // skip header
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

                    boolean hasRewardId = fields.length >= 6;
                    String requestId = fields[0].trim();
                    String memberId = fields[1].trim();
                    String rewardId = hasRewardId ? fields[2].trim() : "";
                    int pointsRequested = Integer.parseInt(
                            fields[hasRewardId ? 3 : 2].trim());
                    LocalDate requestDate = LocalDate.parse(
                            fields[hasRewardId ? 4 : 3].trim());
                    String status = fields[hasRewardId ? 5 : 4].trim();

                    if (requestId.isEmpty() || memberId.isEmpty()
                            || pointsRequested <= 0 || status.isEmpty()) {
                        throw new IllegalArgumentException("invalid required request value");
                    }

                    RedemptionRequest request = new RedemptionRequest(
                            requestId, memberId, rewardId, pointsRequested, requestDate, status);
                    requestControl.addRequest(request);
                } catch (RuntimeException exception) {
                    System.out.println("Skipping invalid request record at line "
                            + lineNumber + ": " + exception.getMessage());
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("No existing request data found, creating new file.");
            saveToRequestFile(requestControl);
        } catch (IOException e) {
            System.out.println("Error reading request file: " + e.getMessage());
        }
    }
}
