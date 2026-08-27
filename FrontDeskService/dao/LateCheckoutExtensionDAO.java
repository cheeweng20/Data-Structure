package FrontDeskService.dao;

import FrontDeskService.entity.LateCheckoutExtension;
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
import java.util.List;

/**
 * Persists Front Desk late check-out extensions independently of reservations.
 */
public class LateCheckoutExtensionDAO {

    private static final int INITIAL_CAPACITY = 10;
    private static final String HEADER = "ConfirmationNumber,ExtendedCheckOutAt,"
            + "ExpectedRoomReadyAt,Reason";
    private static final String DEFAULT_FILE_NAME
            = "FrontDeskService/src/late_checkout_extensions.csv";
    private final String fileName;

    public LateCheckoutExtensionDAO() {
        this(DEFAULT_FILE_NAME);
    }

    /** Allows Front Desk tests to use an isolated data file. */
    public LateCheckoutExtensionDAO(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("file name cannot be blank");
        }
        this.fileName = fileName;
    }

    /**
     * Loads all saved late check-out extensions. Missing files are initialised
     * with the current header. Malformed legacy rows are skipped so they do not
     * prevent Front Desk from using valid records.
     */
    public ListInterface<LateCheckoutExtension> retrieveFromFile() {
        ListInterface<LateCheckoutExtension> extensions
                = new ArrayList<>(INITIAL_CAPACITY);

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    List<String> fields = parseCsvLine(line);
                    if (lineNumber == 1 && isHeader(fields)) {
                        continue;
                    }
                    if (fields.size() < 4) {
                        continue;
                    }

                    String reason = joinFields(fields, 3);
                    extensions.add(new LateCheckoutExtension(
                            fields.get(0),
                            LocalDateTime.parse(fields.get(1).trim()),
                            LocalDateTime.parse(fields.get(2).trim()),
                            reason));
                } catch (RuntimeException exception) {
                    // Keep valid historical records available if one row is bad.
                    System.out.println("Skipping invalid late check-out record at line "
                            + lineNumber + ": " + exception.getMessage());
                }
            }
        } catch (FileNotFoundException exception) {
            createFileWithHeader();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to load late check-out extensions.", exception);
        }

        return extensions;
    }

    /** Rewrites the late check-out extension file using the current CSV schema. */
    public void saveToFile(ListInterface<LateCheckoutExtension> extensions) {
        if (extensions == null) {
            throw new IllegalArgumentException("extensions cannot be null");
        }

        createParentDirectory();
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println(HEADER);
            for (int position = 1; position <= extensions.getNumberOfEntries(); position++) {
                LateCheckoutExtension extension = extensions.getEntry(position);
                if (extension != null) {
                    writer.println(toCsvLine(extension));
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to save late check-out extensions.", exception);
        }
    }

    /** Finds the current extension for a reservation, if one has been recorded. */
    public LateCheckoutExtension findByConfirmationNumber(String confirmationNumber) {
        if (confirmationNumber == null || confirmationNumber.trim().isEmpty()) {
            return null;
        }

        ListInterface<LateCheckoutExtension> extensions = retrieveFromFile();
        for (int position = 1; position <= extensions.getNumberOfEntries(); position++) {
            LateCheckoutExtension extension = extensions.getEntry(position);
            if (extension.getConfirmationNumber().equalsIgnoreCase(confirmationNumber.trim())) {
                return extension;
            }
        }
        return null;
    }

    /**
     * Creates an extension or replaces the existing extension for the same
     * confirmation number. A reservation therefore has at most one active
     * Front Desk late check-out record.
     */
    public void saveOrUpdate(LateCheckoutExtension extension) {
        if (extension == null) {
            throw new IllegalArgumentException("extension cannot be null");
        }

        ListInterface<LateCheckoutExtension> extensions = retrieveFromFile();
        for (int position = 1; position <= extensions.getNumberOfEntries(); position++) {
            LateCheckoutExtension current = extensions.getEntry(position);
            if (current.getConfirmationNumber().equalsIgnoreCase(
                    extension.getConfirmationNumber())) {
                extensions.replace(position, extension);
                saveToFile(extensions);
                return;
            }
        }

        extensions.add(extension);
        saveToFile(extensions);
    }

    /** Removes the active extension once the guest has completed check-out. */
    public boolean deleteByConfirmationNumber(String confirmationNumber) {
        if (confirmationNumber == null || confirmationNumber.trim().isEmpty()) {
            return false;
        }

        ListInterface<LateCheckoutExtension> extensions = retrieveFromFile();
        for (int position = 1; position <= extensions.getNumberOfEntries(); position++) {
            LateCheckoutExtension extension = extensions.getEntry(position);
            if (extension.getConfirmationNumber().equalsIgnoreCase(confirmationNumber.trim())) {
                extensions.remove(position);
                saveToFile(extensions);
                return true;
            }
        }
        return false;
    }

    private String toCsvLine(LateCheckoutExtension extension) {
        return escapeCsv(extension.getConfirmationNumber()) + ","
                + escapeCsv(extension.getExtendedCheckOutAt().toString()) + ","
                + escapeCsv(extension.getExpectedRoomReadyAt().toString()) + ","
                + escapeCsv(extension.getReason());
    }

    private boolean isHeader(List<String> fields) {
        return fields.size() >= 4
                && "ConfirmationNumber".equalsIgnoreCase(fields.get(0).trim())
                && "ExtendedCheckOutAt".equalsIgnoreCase(fields.get(1).trim());
    }

    private String joinFields(List<String> fields, int firstPosition) {
        StringBuilder value = new StringBuilder();
        for (int position = firstPosition; position < fields.size(); position++) {
            if (position > firstPosition) {
                value.append(',');
            }
            value.append(fields.get(position));
        }
        return value.toString().trim();
    }

    private List<String> parseCsvLine(String line) {
        List<String> fields = new java.util.ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length()
                        && line.charAt(index + 1) == '"') {
                    field.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(character);
            }
        }

        if (quoted) {
            throw new IllegalArgumentException("unclosed quoted value");
        }
        fields.add(field.toString());
        return fields;
    }

    private String escapeCsv(String value) {
        if (value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    private void createFileWithHeader() {
        saveToFile(new ArrayList<LateCheckoutExtension>(INITIAL_CAPACITY));
    }

    private void createParentDirectory() {
        File file = new File(fileName);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException(
                    "Unable to create Front Desk data directory.");
        }
    }
}
