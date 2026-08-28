package LoyaltyAndRewardsService.dao;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import LoyaltyAndRewardsService.entity.RedemptionRequest;
import adt.LinkedList;
import adt.ListInterface;

/**
 * @author Chee Weng
 */
public class RequestDao {
    private static final String DEFAULT_FILE_NAME =
            "LoyaltyAndRewardsService/src/requests.csv";
    private static final String HEADER =
            "RequestId,MemberId,ConfirmationNumber,PointsRequested,RequestDate,Status";
    private final String fileName;

    public RequestDao() {
        this(DEFAULT_FILE_NAME);
    }

    public RequestDao(String fileName) {
        this.fileName = fileName;
    }

    public void saveToFile(ListInterface<RedemptionRequest> requests) {
        createParentDirectory();
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println(HEADER);
            for (RedemptionRequest request : requests) {
                writer.println(request.toCsvLine());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to save request file.", e);
        }
    }

    public ListInterface<RedemptionRequest> retrieveFromFile() {
        ListInterface<RedemptionRequest> requests = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
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
                    if (fields.length != 6) {
                        throw new IllegalArgumentException("expected 6 columns");
                    }

                    String requestId = fields[0].trim();
                    String memberId = fields[1].trim();
                    String confirmationNumber = fields[2].trim();
                    int pointsRequested = Integer.parseInt(fields[3].trim());
                    LocalDate requestDate = LocalDate.parse(fields[4].trim());
                    String status = fields[5].trim();

                    if (!requestId.matches("R\\d+") || memberId.isEmpty()
                            || confirmationNumber.isEmpty()
                            || pointsRequested <= 0 || status.isEmpty()) {
                        throw new IllegalArgumentException("invalid required request value");
                    }

                    RedemptionRequest request = new RedemptionRequest(
                            requestId, memberId, confirmationNumber,
                            pointsRequested, requestDate, status);
                    requests.add(request);
                } catch (RuntimeException exception) {
                    System.out.println("Skipping invalid request record at line "
                            + lineNumber + ": " + exception.getMessage());
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("No existing request data found, creating new file.");
            saveToFile(requests);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read request file.", e);
        }
        return requests;
    }

    private void createParentDirectory() {
        File file = new File(fileName);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create request data directory.");
        }
    }
}
