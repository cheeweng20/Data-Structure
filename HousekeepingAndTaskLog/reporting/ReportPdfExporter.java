package HousekeepingAndTaskLog.reporting;

import adt.ArrayList;
import adt.ListInterface;
import common.reporting.pdf.PdfDocumentWriter;
import java.io.IOException;
import java.nio.file.Path;

import static common.reporting.pdf.PdfDocumentWriter.abbreviate;
import static common.reporting.pdf.PdfDocumentWriter.appendText;

/** Exports Housekeeping reports through the shared PDF document writer. */
/**
 * @author Zhe Sheng
 */
public final class ReportPdfExporter {

    private ReportPdfExporter() {
    }

    public static Path export(String title, String report) throws IOException {
        StringBuilder page = new StringBuilder();
        page.append("0.96 0.97 0.99 rg 0 0 ").append(PdfDocumentWriter.PAGE_WIDTH)
                .append(' ').append(PdfDocumentWriter.PAGE_HEIGHT).append(" re f\n")
                .append("0.07 0.13 0.24 rg 0 535 ").append(PdfDocumentWriter.PAGE_WIDTH)
                .append(" 60 re f\n");
        appendText(page, "F1", 19, 42, 557, 1, 1, 1, title);
        ListInterface<String[]> tableRows = extractTableRows(report);
        drawChart(page, tableRows);
        drawTable(page, tableRows);
        ListInterface<String> pages = new ArrayList<>();
        pages.add(page.toString());
        return PdfDocumentWriter.write(title, "housekeeping-report", pages);
    }

    public static boolean open(Path pdfPath) throws IOException {
        return PdfDocumentWriter.open(pdfPath);
    }

    private static void drawChart(StringBuilder page,
            ListInterface<String[]> tableRows) {
        ListInterface<String> labels = new ArrayList<>();
        ListInterface<Integer> values = new ArrayList<>();
        if (!tableRows.isEmpty() && tableRows.getEntry(1).length == 2) {
            for (int position = 2;
                    position <= tableRows.getNumberOfEntries(); position++) {
                try {
                    labels.add(tableRows.getEntry(position)[0]);
                    values.add(Integer.parseInt(tableRows.getEntry(position)[1]));
                } catch (NumberFormatException ignored) {
                    // Ignore an invalid table row.
                }
            }
        } else if (!tableRows.isEmpty()) {
            for (int position = 2;
                    position <= tableRows.getNumberOfEntries(); position++) {
                String status = tableRows.getEntry(position)[2];
                int existingPosition = findPosition(labels, status);
                if (existingPosition == 0) {
                    labels.add(status);
                    values.add(1);
                } else {
                    values.replace(existingPosition,
                            values.getEntry(existingPosition) + 1);
                }
            }
        }
        page.append("1 1 1 rg 38 320 766 185 re f\n");
        appendText(page, "F3", 12, 52, 485, 0.07, 0.13, 0.24, "Task Status Chart");
        int maximum = 1;
        for (int value : values) maximum = Math.max(maximum, value);
        double y = 458;
        for (int position = 1;
                position <= labels.getNumberOfEntries(); position++) {
            appendText(page, "F1", 8, 52, y + 2, 0.16, 0.20, 0.28,
                    abbreviate(labels.getEntry(position), 23));
            page.append("0.20 0.55 0.86 rg 215 ").append(y).append(' ')
                    .append(Math.max(2, 430.0 * values.getEntry(position) / maximum))
                    .append(" 12 re f\n");
            appendText(page, "F1", 8, 660, y + 2, 0.16, 0.20, 0.28,
                    String.valueOf(values.getEntry(position)));
            y -= 24;
        }
    }

    private static int findPosition(ListInterface<String> entries, String value) {
        for (int position = 1;
                position <= entries.getNumberOfEntries(); position++) {
            if (entries.getEntry(position).equals(value)) {
                return position;
            }
        }
        return 0;
    }

    private static ListInterface<String[]> extractTableRows(String report) {
        ListInterface<String[]> rows = new ArrayList<>();
        for (String line : report.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("|")) {
                continue;
            }
            String[] raw = trimmed.substring(1, trimmed.length() - 1).split("\\|", -1);
            String[] cells = new String[raw.length];
            for (int index = 0; index < raw.length; index++) {
                cells[index] = raw[index].trim();
            }
            rows.add(cells);
        }
        return rows;
    }

    /** Draws a navy-header, alternating-row table for the PDF report. */
    private static void drawTable(StringBuilder page, ListInterface<String[]> rows) {
        if (rows.isEmpty()) {
            appendText(page, "F1", 10, 44, 280, 0.20, 0.24, 0.30, "No task records found.");
            return;
        }
        int rowCount = Math.min(rows.getNumberOfEntries(), 12);
        int columnCount = rows.getEntry(1).length;
        double x = 38;
        double width = 766;
        double cellWidth = width / columnCount;
        double rowHeight = 20;
        double top = 285;
        for (int row = 0; row < rowCount; row++) {
            double y = top - row * rowHeight;
            if (row == 0) {
                page.append("0.06 0.22 0.42 rg ");
            } else if (row % 2 == 0) {
                page.append("0.94 0.96 0.99 rg ");
            } else {
                page.append("1 1 1 rg ");
            }
            page.append(x).append(' ').append(y - rowHeight).append(' ').append(width)
                    .append(' ').append(rowHeight).append(" re f\n");
            for (int column = 0; column < columnCount; column++) {
                double cellX = x + column * cellWidth;
                appendText(page, row == 0 ? "F3" : "F1", 7.5, cellX + 8, y - 13,
                        row == 0 ? 1 : 0.10, row == 0 ? 1 : 0.14, row == 0 ? 1 : 0.20,
                        abbreviate(rows.getEntry(row + 1)[column],
                                Math.max(8, (int) (cellWidth / 5))));
                if (column > 0) {
                    page.append("0.82 0.86 0.91 RG 0.4 w ").append(cellX).append(' ')
                            .append(y - rowHeight).append(" m ").append(cellX).append(' ')
                            .append(y).append(" l S\n");
                }
            }
        }
    }
}
