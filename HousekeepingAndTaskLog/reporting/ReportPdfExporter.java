package HousekeepingAndTaskLog.reporting;

import common.reporting.pdf.PdfDocumentWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static common.reporting.pdf.PdfDocumentWriter.abbreviate;
import static common.reporting.pdf.PdfDocumentWriter.appendText;

/** Exports Housekeeping reports through the shared PDF document writer. */
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
        List<String[]> tableRows = extractTableRows(report);
        drawChart(page, tableRows);
        drawTable(page, tableRows);
        return PdfDocumentWriter.write(title, "housekeeping-report", List.of(page.toString()));
    }

    public static boolean open(Path pdfPath) throws IOException {
        return PdfDocumentWriter.open(pdfPath);
    }

    private static void drawChart(StringBuilder page, List<String[]> tableRows) {
        List<String> labels = new ArrayList<>();
        List<Integer> values = new ArrayList<>();
        if (!tableRows.isEmpty() && tableRows.get(0).length == 2) {
            for (int index = 1; index < tableRows.size(); index++) {
                try {
                    labels.add(tableRows.get(index)[0]);
                    values.add(Integer.parseInt(tableRows.get(index)[1]));
                } catch (NumberFormatException ignored) {
                    // Ignore an invalid table row.
                }
            }
        } else if (!tableRows.isEmpty()) {
            for (int index = 1; index < tableRows.size(); index++) {
                String status = tableRows.get(index)[2];
                int existing = labels.indexOf(status);
                if (existing < 0) {
                    labels.add(status);
                    values.add(1);
                } else {
                    values.set(existing, values.get(existing) + 1);
                }
            }
        }
        page.append("1 1 1 rg 38 320 766 185 re f\n");
        appendText(page, "F3", 12, 52, 485, 0.07, 0.13, 0.24, "Task Status Chart");
        int maximum = 1;
        for (int value : values) maximum = Math.max(maximum, value);
        double y = 458;
        for (int i = 0; i < labels.size(); i++) {
            appendText(page, "F1", 8, 52, y + 2, 0.16, 0.20, 0.28, abbreviate(labels.get(i), 23));
            page.append("0.20 0.55 0.86 rg 215 ").append(y).append(' ')
                    .append(Math.max(2, 430.0 * values.get(i) / maximum)).append(" 12 re f\n");
            appendText(page, "F1", 8, 660, y + 2, 0.16, 0.20, 0.28, String.valueOf(values.get(i)));
            y -= 24;
        }
    }

    private static List<String[]> extractTableRows(String report) {
        List<String[]> rows = new ArrayList<>();
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
    private static void drawTable(StringBuilder page, List<String[]> rows) {
        if (rows.isEmpty()) {
            appendText(page, "F1", 10, 44, 280, 0.20, 0.24, 0.30, "No task records found.");
            return;
        }
        int rowCount = Math.min(rows.size(), 12);
        int columnCount = rows.get(0).length;
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
                        abbreviate(rows.get(row)[column], Math.max(8, (int) (cellWidth / 5))));
                if (column > 0) {
                    page.append("0.82 0.86 0.91 RG 0.4 w ").append(cellX).append(' ')
                            .append(y - rowHeight).append(" m ").append(cellX).append(' ')
                            .append(y).append(" l S\n");
                }
            }
        }
    }
}
