package FrontDeskService.reporting;

import adt.ArrayList;
import adt.ListInterface;
import common.reporting.pdf.PdfDocumentWriter;
import java.io.IOException;
import java.nio.file.Path;

import static common.reporting.pdf.PdfDocumentWriter.abbreviate;
import static common.reporting.pdf.PdfDocumentWriter.appendText;

/** Exports Front Desk table reports through the shared PDF document writer. */
public final class ReportPdfExporter {

    private ReportPdfExporter() {
    }

    /** Creates a paginated PDF copy of a Front Desk console report. */
    public static Path export(String title, String report) throws IOException {
        ListInterface<String> pageStreams = createPageStreams(title, report);
        return PdfDocumentWriter.write(title, "front-desk-report", pageStreams);
    }

    /** Opens an exported report when the current desktop supports it. */
    public static boolean open(Path pdfPath) throws IOException {
        return PdfDocumentWriter.open(pdfPath);
    }

    private static ListInterface<String> createPageStreams(String title, String report) {
        ListInterface<String> streams = new ArrayList<>();
        int pageNumber = 1;
        PageCanvas page = createPage(title, pageNumber);
        int tablePhase = -1;
        int tableRowIndex = 0;
        ListInterface<String> tableHeader = null;

        for (String rawLine : report.split("\\R", -1)) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                page.y -= 7;
                continue;
            }

            if (isTableBorder(line)) {
                if (tablePhase == -1) {
                    tablePhase = 0;
                    tableRowIndex = 0;
                } else if (tablePhase == 1) {
                    tablePhase = 2;
                } else {
                    tablePhase = -1;
                    tableHeader = null;
                }
                continue;
            }

            ListInterface<String> columns = parseTableColumns(line);
            if (!columns.isEmpty()) {
                boolean header = tablePhase == 0;
                if (page.y - 19 < 42) {
                    streams.add(page.stream.toString());
                    page = createPage(title, ++pageNumber);
                    if (!header && tablePhase == 2 && tableHeader != null) {
                        drawTableRow(page.stream, tableHeader, page.y, true, 0);
                        page.y -= 19;
                    }
                }

                drawTableRow(page.stream, columns, page.y, header, tableRowIndex);
                page.y -= 19;
                if (header) {
                    tableHeader = columns;
                    tablePhase = 1;
                } else {
                    tableRowIndex++;
                }
                continue;
            }

            if (isReportTitle(line, title)) {
                continue;
            }
            if (page.y - 21 < 42) {
                streams.add(page.stream.toString());
                page = createPage(title, ++pageNumber);
            }
            drawSummaryRow(page.stream, line, page.y);
            page.y -= 21;
        }

        streams.add(page.stream.toString());
        return streams;
    }

    private static PageCanvas createPage(String title, int pageNumber) {
        StringBuilder stream = new StringBuilder();
        stream.append("0.96 0.97 0.99 rg 0 0 ")
                .append(PdfDocumentWriter.PAGE_WIDTH).append(' ')
                .append(PdfDocumentWriter.PAGE_HEIGHT).append(" re f\n")
                .append("0.07 0.13 0.24 rg 0 535 ")
                .append(PdfDocumentWriter.PAGE_WIDTH).append(" 60 re f\n");
        appendText(stream, "F1", 19, 42, 557, 1, 1, 1, title);
        appendText(stream, "F1", 8, 735, 558, 0.82, 0.87, 0.95,
                "Page " + pageNumber);
        stream.append("0.78 0.82 0.88 RG 0.6 w 38 31 m 804 31 l S\n");
        appendText(stream, "F1", 7, 42, 18, 0.38, 0.43, 0.50,
                "Front Desk Service");
        return new PageCanvas(stream, 507);
    }

    private static boolean isTableBorder(String line) {
        return line.matches("^\\+[-+]+\\+$");
    }

    private static boolean isReportTitle(String line, String title) {
        return line.replace("-", "").trim().equalsIgnoreCase(title);
    }

    private static ListInterface<String> parseTableColumns(String line) {
        ListInterface<String> columns = new ArrayList<>();
        if (!line.startsWith("|") || !line.endsWith("|")) {
            return columns;
        }

        String[] rawColumns = line.substring(1, line.length() - 1).split("\\|", -1);
        for (String rawColumn : rawColumns) {
            columns.add(rawColumn.trim());
        }
        return columns;
    }

    private static void drawSummaryRow(StringBuilder stream, String line, double y) {
        stream.append("1 1 1 rg 44 ").append(y - 5).append(" 756 18 re f\n");
        int separator = line.indexOf(':');
        if (separator > 0 && separator < 32) {
            String label = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            appendText(stream, "F3", 8, 54, y, 0.17, 0.27, 0.40, label);
            appendText(stream, "F1", 8, 245, y, 0.20, 0.24, 0.30, value);
        } else {
            appendText(stream, "F1", 8, 54, y, 0.20, 0.24, 0.30,
                    abbreviate(line, 132));
        }
    }

    private static void drawTableRow(StringBuilder stream, ListInterface<String> columns,
            double y, boolean header, int rowIndex) {
        double tableX = 44;
        double tableWidth = 756;
        double cellWidth = tableWidth / columns.getNumberOfEntries();
        if (header) {
            stream.append("0.09 0.23 0.40 rg ");
        } else {
            double shade = rowIndex % 2 == 0 ? 1.0 : 0.94;
            stream.append(shade).append(' ')
                    .append(shade == 1.0 ? 1.0 : 0.96).append(' ')
                    .append(shade == 1.0 ? 1.0 : 0.98).append(" rg ");
        }
        stream.append(tableX).append(' ').append(y - 5).append(' ')
                .append(tableWidth).append(" 18 re f\n");

        for (int index = 0; index < columns.getNumberOfEntries(); index++) {
            double cellX = tableX + index * cellWidth;
            int maximumCharacters = Math.max(4, (int) ((cellWidth - 14) / 4.0));
            appendText(stream, header ? "F3" : "F1", 7.5, cellX + 7, y,
                    header ? 1 : 0.18, header ? 1 : 0.22, header ? 1 : 0.28,
                    abbreviate(columns.getEntry(index + 1), maximumCharacters));
            if (index > 0) {
                stream.append(header ? "0.35 0.48 0.62 RG " : "0.82 0.85 0.89 RG ")
                        .append("0.4 w ").append(cellX).append(' ')
                        .append(y - 5).append(" m ").append(cellX).append(' ')
                        .append(y + 13).append(" l S\n");
            }
        }
    }

    private static final class PageCanvas {
        private final StringBuilder stream;
        private double y;

        private PageCanvas(StringBuilder stream, double y) {
            this.stream = stream;
            this.y = y;
        }
    }
}
