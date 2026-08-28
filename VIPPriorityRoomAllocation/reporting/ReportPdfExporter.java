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
        String pageStream = createPageStream(title, report, chartItems, chartType);

        ListInterface<String> pages = new ArrayList<>();
        pages.add(pageStream);
        return PdfDocumentWriter.write(title, "reservation-report", pages);
    }

    public static boolean open(Path pdfPath) throws IOException {
        return PdfDocumentWriter.open(pdfPath);
    }

    private static String createPageStream(String title, String report,
            ListInterface<ChartItem> chartItems, ChartType chartType) {
        StringBuilder stream = new StringBuilder();
        drawPageBackground(stream, title);
        drawChart(stream, chartTitle(chartType), chartItems);
        drawReportText(stream, report);
        return stream.toString();
    }

    private static void drawPageBackground(StringBuilder stream, String title) {
        stream.append("0.96 0.97 0.99 rg 0 0 ")
                .append(PAGE_WIDTH).append(' ').append(PAGE_HEIGHT).append(" re f\n");
        stream.append("0.07 0.13 0.24 rg 0 535 ")
                .append(PAGE_WIDTH).append(" 60 re f\n");
        appendText(stream, "F1", 19, 42, 557, 1, 1, 1, title);
        appendText(stream, "F1", 7, 42, 18, 0.38, 0.43, 0.50,
                "VIP Priority Room Allocation");
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

    private static void drawReportText(StringBuilder stream, String report) {
        double y = 292;
        for (String rawLine : report.split("\\R")) {
            if (y < 42) {
                break;
            }
            String line = rawLine.trim();
            if (line.contains("Chart Data")) {
                break;
            }
            if (line.isEmpty()) {
                y -= 8;
                continue;
            }
            appendText(stream, "F2", 7.2, 44, y, 0.20, 0.24, 0.30,
                    abbreviate(line, 145));
            y -= 13;
        }
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
                ? "Reservation status count"
                : "Allocated rooms by loyalty tier";
    }

    private static final class ChartItem {
        private final String label;
        private final int value;

        private ChartItem(String label, int value) {
            this.label = label;
            this.value = value;
        }
    }
}
