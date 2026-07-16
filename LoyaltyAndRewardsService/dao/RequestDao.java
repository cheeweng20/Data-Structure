package LoyaltyAndRewardsService.dao;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.Iterator;

import LoyaltyAndRewardsService.control.RequestControl;
import LoyaltyAndRewardsService.entity.RedemptionRequest;

/**
 * @author Chee Weng
 */
public class RequestDao {
    private static final String FILE_NAME = "LoyaltyAndRewardsService/src/requests.csv";

    public static void saveToRequestFile(RequestControl requestControl) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            writer.println("RequestId,MemberId,PointsRequested,RequestDate,Status");

            Iterator<RedemptionRequest> it = requestControl.getRequestIterator();
            while (it.hasNext()) {
                RedemptionRequest request = it.next();
                writer.println(request.toCsvLine());
            }
        } catch (IOException e) {
            System.out.println("Error saving request file: " + e.getMessage());
        }
    }

    public static void loadFromRequestFile(RequestControl requestControl) {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            reader.readLine(); // skip header
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    continue;
                }

                String[] fields = line.split(",", -1);
                if (fields.length < 5) {
                    continue;
                }

                String requestId = fields[0];
                String memberId = fields[1];
                int pointsRequested = Integer.parseInt(fields[2]);
                LocalDate requestDate = LocalDate.parse(fields[3]);
                String status = fields[4];

                RedemptionRequest request = new RedemptionRequest(
                        requestId, memberId, pointsRequested, requestDate, status);
                requestControl.addRequest(request);
            }
        } catch (FileNotFoundException e) {
            System.out.println("No existing request data found, creating new file.");
            saveToRequestFile(requestControl);
        } catch (IOException e) {
            System.out.println("Error reading request file: " + e.getMessage());
        }
    }
}
