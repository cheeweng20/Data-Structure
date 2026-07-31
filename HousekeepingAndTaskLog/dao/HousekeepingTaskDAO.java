package HousekeepingAndTaskLog.dao;

import HousekeepingAndTaskLog.entity.HousekeepingTask;
import HousekeepingAndTaskLog.entity.TaskStatus;
import adt.ArrayList;
import adt.ListInterface;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

public class HousekeepingTaskDAO {

    private static final int INITIAL_CAPACITY = 100;
    private static final String HEADER = "TaskId,RoomNumber,AssignedStaff,Status,CreatedAt,ExpectedReadyAt,Remarks";
    private final String fileName;

    public HousekeepingTaskDAO() {
        this("HousekeepingAndTaskLog/src/housekeeping_tasks.csv");
    }

    public HousekeepingTaskDAO(String fileName) {
        this.fileName = fileName;
    }

    public ListInterface<HousekeepingTask> retrieveFromFile() {
        ListInterface<HousekeepingTask> tasks = new ArrayList<>(INITIAL_CAPACITY);

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            reader.readLine();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] fields = line.split(",", -1);
                if (fields.length < 7) {
                    continue;
                }

                tasks.add(new HousekeepingTask(
                        fields[0],
                        fields[1],
                        fields[2],
                        TaskStatus.valueOf(fields[3]),
                        LocalDateTime.parse(fields[4]),
                        LocalDateTime.parse(fields[5]),
                        fields[6]));
            }
        } catch (FileNotFoundException ex) {
            createParentDirectory();
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("Unable to load housekeeping tasks.", ex);
        }

        return tasks;
    }

    public void saveToFile(ListInterface<HousekeepingTask> tasks) {
        createParentDirectory();

        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println(HEADER);

            for (int i = 1; i <= tasks.getNumberOfEntries(); i++) {
                writer.println(tasks.getEntry(i).toCsvLine());
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to save housekeeping tasks.", ex);
        }
    }

    private void createParentDirectory() {
        File file = new File(fileName);
        File parent = file.getParentFile();

        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create housekeeping data directory.");
        }
    }
}
