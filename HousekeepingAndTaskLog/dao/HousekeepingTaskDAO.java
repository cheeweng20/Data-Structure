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

/**
 * @author Your Name
 */
public class HousekeepingTaskDAO {

    private static final int INITIAL_CAPACITY = 100;
    private static final String HEADER = "TaskId,RoomNumber,AssignedStaff,Status,CreatedAt,ExpectedReadyAt,Priority,Remarks";
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
                if (fields.length < 8) {
                    continue;
                }

                tasks.add(new HousekeepingTask(
                        fields[0],
                        fields[1],
                        fields[2],
                        TaskStatus.valueOf(fields[3]),
                        LocalDateTime.parse(fields[4]),
                        LocalDateTime.parse(fields[5]),
                        Integer.parseInt(fields[6]),
                        fields[7]));
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

    // private void createDefaultFile() {
    //     createParentDirectory();

    //     try (PrintWriter writer = new PrintWriter(fileName)) {
    //         writer.println(HEADER);
    //         writer.println("HK1001,101,Alicia Tan,DIRTY,2026-07-30T08:00,2026-07-30T11:00,1,Guest checked out");
    //         writer.println("HK1002,102,Bryan Lim,CLEANING_IN_PROGRESS,2026-07-30T08:20,2026-07-30T11:30,2,Early arrival expected");
    //         writer.println("HK1003,201,Chong Mei,INSPECTED,2026-07-30T09:00,2026-07-30T12:00,3,Pending supervisor approval");
    //         writer.println("HK1004,205,Deepa Nair,READY_FOR_CHECK_IN,2026-07-30T09:30,2026-07-30T10:30,4,Ready for front desk");
    //     } catch (IOException ex) {
    //         throw new IllegalStateException("Unable to create housekeeping task file.", ex);
    //     }
    // }

    private void createParentDirectory() {
        File file = new File(fileName);
        File parent = file.getParentFile();

        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create housekeeping data directory.");
        }
    }
}
