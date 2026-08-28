package VIPPriorityRoomAllocation.reporting;

import adt.ArrayList;
import adt.ListInterface;
import java.io.IOException;
import java.nio.file.Path;

import common.reporting.pdf.PdfDocumentWriter;

import static common.reporting.pdf.PdfDocumentWriter.abbreviate;
import static common.reporting.pdf.PdfDocumentWriter.appendText;

// Creates a PDF copy of a reservation report with one bar chart.
public final class ReportPdfExporter {

    public enum ChartType {
        RESERVATION_STATUS,
        TIER_ALLOCATION
    }

    private static final double PAGE_WIDTH = PdfDocumentWriter.PAGE_WIDTH;
    private static final double PAGE_HEIGHT = PdfDocumentWriter.PAGE_HEIGHT;

    private ReportPdfExporter() {
    }

    public static Path export(String title, String report, ChartType chartType)
            throws IOException {
        ListInterface<ChartItem> chartItems = extractChartItems(report, chartType);
        ListInterface<String> pageStreams = createPageStreams(title, report,
                chartItems, chartType);
        return PdfDocumentWriter.write(title, "reservation-report", pageStreams);
    }

    public static boolean open(Path pdfPath) throws IOException {
        return PdfDocumentWriter.open(pdfPath);
    }

    private static ListInterface<String> createPageStreams(String title, String report,
            ListInterface<ChartItem> chartItems, ChartType chartType) {
        ListInterface<String> streams = new ArrayList<>();
        int pageNumber = 1;
        PageCanvas page = createPage(title, pageNumber, chartItems, chartType);
        int tablePhase = -1;
        int tableRowIndex = 0;
        ListInterface<String> tableHeader = null;

        for (String rawLine : visibleReportLines(report)) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                page.y -= 7;
                continue;
            }

            if (line.matches("^\\+[-+]+\\+$")) {
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

            if (line.startsWith("===") && line.endsWith("===")) {
                String heading = line.replace("=", "").trim();
                if (page.y - 27 < 42) {
                    streams.add(page.stream.toString());
                    page = createPage(title, ++pageNumber, new ArrayList<>(), chartType);
                }
                drawSectionHeading(page.stream, heading, page.y);
                page.y -= 28;
                continue;
            }

            ListInterface<String> columns = parseTableColumns(line);
            if (!columns.isEmpty()) {
                boolean isHeader = tablePhase == 0;
                if (page.y - 19 < 42) {
                    streams.add(page.stream.toString());
                    page = createPage(title, ++pageNumber, new ArrayList<>(), chartType);
                    if (!isHeader && tablePhase == 2 && tableHeader != null) {
                        drawTableRow(page.stream, tableHeader, page.y, true, 0);
                        page.y -= 19;
                    }
                }

                drawTableRow(page.stream, columns, page.y, isHeader, tableRowIndex);
                page.y -= 19;
                if (isHeader) {
                    tableHeader = columns;
                    tablePhase = 1;
                } else {
                    tableRowIndex++;
                }
                continue;
            }

            if (page.y - 21 < 42) {
                streams.add(page.stream.toString());
                page = createPage(title, ++pageNumber, new ArrayList<>(), chartType);
            }
            drawSummaryRow(page.stream, line, page.y);
            page.y -= 21;
        }

        streams.add(page.stream.toString());
        return streams;
    }

    private static PageCanvas createPage(String title, int pageNumber,
            ListInterface<ChartItem> chartItems, ChartType chartType) {
        StringBuilder stream = new StringBuilder();
        drawPageBackground(stream, title, pageNumber);
        if (pageNumber == 1) {
            drawChart(stream, chartTitle(chartType), chartItems);
            return new PageCanvas(stream, 292);
        }
        return new PageCanvas(stream, 507);
    }

    private static void drawPageBackground(StringBuilder stream, String title,
            int pageNumber) {
        stream.append("0.96 0.97 0.99 rg 0 0 ")
                .append(PAGE_WIDTH).append(' ').append(PAGE_HEIGHT).append(" re f\n");
        stream.append("0.07 0.13 0.24 rg 0 535 ")
                .append(PAGE_WIDTH).append(" 60 re f\n");
        appendText(stream, "F1", 19, 42, 557, 1, 1, 1, title);
        appendText(stream, "F1", 8, 735, 558, 0.82, 0.87, 0.95,
                "Page " + pageNumber);
        stream.append("0.78 0.82 0.88 RG 0.6 w 38 31 m 804 31 l S\n");
    }

    private static void drawSectionHeading(StringBuilder stream, String heading, double y) {
        stream.append("0.88 0.93 0.98 rg 44 ").append(y - 5)
                .append(" 756 21 re f\n");
        appendText(stream, "F3", 10.5, 54, y + 1, 0.07, 0.20, 0.36, heading);
    }

    private static void drawSummaryRow(StringBuilder stream, String line, double y) {
        stream.append("1 1 1 rg 44 ").append(y - 5).append(" 756 18 re f\n");
        int separator = line.indexOf(':');
        if (separator > 0 && separator < 38) {
            String label = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            appendText(stream, "F3", 8, 54, y, 0.17, 0.27, 0.40,
                    abbreviate(label, 30));
            appendText(stream, "F1", 8, 245, y, 0.20, 0.24, 0.30,
                    abbreviate(value, 80));
        } else {
            appendText(stream, "F1", 8, 54, y, 0.20, 0.24, 0.30,
                    abbreviate(line, 130));
        }
    }

    private static void drawTableRow(StringBuilder stream, ListInterface<String> columns,
            double y, boolean header, int rowIndex) {
        double tableX = 44;
        double tableWidth = 756;
        double cellWidth = tableWidth / columns.getNumberOfEntries();
        if (header) {
            stream.append("0.09 0.23 0.40 rg ").append(tableX).append(' ')
                    .append(y - 5).append(' ').append(tableWidth).append(" 18 re f\n");
        } else {
            double shade = rowIndex % 2 == 0 ? 1.0 : 0.94;
            stream.append(shade).append(' ').append(shade == 1.0 ? 1.0 : 0.96)
                    .append(' ').append(shade == 1.0 ? 1.0 : 0.98).append(" rg ")
                    .append(tableX).append(' ').append(y - 5).append(' ')
                    .append(tableWidth).append(" 18 re f\n");
        }

        for (int index = 0; index < columns.getNumberOfEntries(); index++) {
            double cellX = tableX + index * cellWidth;
            int maximumCharacters = Math.max(4, (int) ((cellWidth - 14) / 4.0));
            appendText(stream, header ? "F3" : "F1", 7.5, cellX + 7, y,
                    header ? 1 : 0.18, header ? 1 : 0.22, header ? 1 : 0.28,
                    abbreviate(columns.getEntry(index + 1), maximumCharacters));
            if (index > 0) {
                stream.append(header ? "0.35 0.48 0.62 RG " : "0.82 0.85 0.89 RG ")
                        .append("0.4 w ").append(cellX).append(' ').append(y - 5)
                        .append(" m ").append(cellX).append(' ').append(y + 13)
                        .append(" l S\n");
            }
        }
    }

    private static void drawChart(StringBuilder stream, String title,
            ListInterface<ChartItem> items) {
        stream.append("1 1 1 rg 38 325 766 190 re f\n");
        appendText(stream, "F3", 13, 52, 493, 0.07, 0.13, 0.24, title);

        if (items.isEmpty()) {
            appendText(stream, "F1", 10, 52, 455, 0.45, 0.49, 0.55,
                    "No chart data is available.");
            return;
        }

        int maximum = 1;
        for (ChartItem item : items) {
            maximum = Math.max(maximum, item.value);
        }

        double y = 462;
        for (ChartItem item : items) {
            double barWidth = 470.0 * item.value / maximum;
            appendText(stream, "F1", 8.5, 52, y + 2, 0.20, 0.24, 0.30,
                    abbreviate(item.label, 18));
            stream.append("0.20 0.55 0.86 rg 190 ").append(y)
                    .append(' ').append(Math.max(2, barWidth)).append(" 13 re f\n");
            appendText(stream, "F1", 8.5, 690, y + 2, 0.20, 0.24, 0.30,
                    String.valueOf(item.value));
            y -= 25;
        }
    }

    private static ListInterface<String> visibleReportLines(String report) {
        ListInterface<String> lines = new ArrayList<>();
        for (String rawLine : report.split("\\R")) {
            String line = rawLine.trim();
            if (line.contains("Chart Data")) {
                break;
            }
            lines.add(rawLine);
        }
        return lines;
    }

    private static ListInterface<ChartItem> extractChartItems(
            String report, ChartType chartType) {
        ListInterface<ChartItem> items = new ArrayList<>();
        String sectionTitle = chartType == ChartType.RESERVATION_STATUS
                ? "=== Reservation Status Chart Data ==="
                : "=== Loyalty Tier Allocation Chart Data ===";
        boolean inSection = false;

        for (String rawLine : report.split("\\R")) {
            String line = rawLine.trim();
            if (line.equals(sectionTitle)) {
                inSection = true;
                continue;
            }
            if (inSection && line.startsWith("===")) {
                break;
            }
            if (!inSection || !line.startsWith("|")) {
                continue;
            }

            ListInterface<String> columns = parseTableColumns(line);
            if (columns.getNumberOfEntries() != 2
                    || columns.getEntry(1).equalsIgnoreCase("Label")) {
                continue;
            }

            Integer value = parseInteger(columns.getEntry(2));
            if (value != null) {
                items.add(new ChartItem(columns.getEntry(1), value));
            }
        }
        return items;
    }

    private static ListInterface<String> parseTableColumns(String line) {
        ListInterface<String> columns = new ArrayList<>();
        for (String rawColumn : line.split("\\|")) {
            if (!rawColumn.isBlank()) {
                columns.add(rawColumn.trim());
            }
        }
        return columns;
    }

    private static Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value.replace(",", "").trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String chartTitle(ChartType chartType) {
        return chartType == ChartType.RESERVATION_STATUS
                ? "Reservation Status Summary"
                : "Room Allocation by Loyalty Tier";
    }

    private static final class ChartItem {
        private final String label;
        private final int value;

        private ChartItem(String label, int value) {
            this.label = label;
            this.value = value;
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
