package FrontDeskService.reporting;

import adt.ArrayList;
import adt.ListInterface;
import common.reporting.pdf.PdfDocumentWriter;
import java.io.IOException;
import java.nio.file.Path;

import static common.reporting.pdf.PdfDocumentWriter.abbreviate;
import static common.reporting.pdf.PdfDocumentWriter.appendText;

/** Exports Front Desk table reports through the shared PDF document writer. */
/**
 * @author Yi Ren
 */
public final class ReportPdfExporter {

    /** The chart shown for each Front Desk report PDF. */
    public enum ChartType {
        OUTSTANDING_BALANCE,
        PAYMENT_METHOD
    }

    private static final int MAXIMUM_CHART_ITEMS = 8;

    private ReportPdfExporter() {
    }

    /**
     * Creates a paginated PDF copy of a Front Desk console report.
     *
     * <p>This overload remains available for existing callers. Front Desk's
     * two known report titles automatically receive their matching chart.</p>
     */
    public static Path export(String title, String report) throws IOException {
        return export(title, report, inferChartType(title));
    }

    /** Creates a paginated Front Desk report PDF with its selected chart. */
    public static Path export(String title, String report, ChartType chartType)
            throws IOException {
        ListInterface<String> pageStreams = createPageStreams(title, report, chartType);
        return PdfDocumentWriter.write(title, "front-desk-report", pageStreams);
    }

    /** Opens an exported report when the current desktop supports it. */
    public static boolean open(Path pdfPath) throws IOException {
        return PdfDocumentWriter.open(pdfPath);
    }

    private static ListInterface<String> createPageStreams(String title, String report,
            ChartType chartType) {
        ListInterface<String> streams = new ArrayList<>();
        int pageNumber = 1;
        ListInterface<ChartItem> chartItems = extractChartItems(report, chartType);
        PageCanvas page = createPage(title, pageNumber, chartType, chartItems);
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
                    page = createPage(title, ++pageNumber, chartType, chartItems);
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
                page = createPage(title, ++pageNumber, chartType, chartItems);
            }
            drawSummaryRow(page.stream, line, page.y);
            page.y -= 21;
        }

        streams.add(page.stream.toString());
        return streams;
    }

    private static PageCanvas createPage(String title, int pageNumber,
            ChartType chartType, ListInterface<ChartItem> chartItems) {
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

        if (pageNumber == 1 && chartType != null) {
            drawChart(stream, chartTitle(chartType), chartItems);
            return new PageCanvas(stream, 292);
        }
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

    /** Draws a first-page horizontal bar chart from the report's table rows. */
    private static void drawChart(StringBuilder stream, String title,
            ListInterface<ChartItem> items) {
        stream.append("1 1 1 rg 38 316 766 194 re f\n")
                .append("0.80 0.85 0.91 RG 0.6 w 38 316 766 194 re S\n");
        appendText(stream, "F3", 12, 52, 492, 0.07, 0.13, 0.24, title);
        appendText(stream, "F1", 8, 52, 477, 0.38, 0.43, 0.50,
                "Bars show the total bill amount for each report category.");

        if (items.isEmpty()) {
            appendText(stream, "F1", 10, 52, 447, 0.45, 0.49, 0.55,
                    "No chart data is available for this report.");
            return;
        }

        int displayedItems = Math.min(items.getNumberOfEntries(), MAXIMUM_CHART_ITEMS);
        double maximum = 1.0;
        for (int position = 1; position <= displayedItems; position++) {
            maximum = Math.max(maximum, items.getEntry(position).value);
        }

        if (items.getNumberOfEntries() > displayedItems) {
            appendText(stream, "F1", 7.5, 610, 477, 0.38, 0.43, 0.50,
                    "Showing top " + displayedItems + " of "
                            + items.getNumberOfEntries() + " categories");
        }

        double y = 451;
        for (int position = 1; position <= displayedItems; position++) {
            ChartItem item = items.getEntry(position);
            double barWidth = 455.0 * item.value / maximum;
            appendText(stream, "F1", 8, 52, y + 2, 0.20, 0.24, 0.30,
                    abbreviate(item.label, 19));
            stream.append("0.20 0.55 0.86 rg 205 ").append(y).append(' ')
                    .append(Math.max(2.0, barWidth)).append(" 12 re f\n");
            appendText(stream, "F1", 8, 675, y + 2, 0.20, 0.24, 0.30,
                    formatAmount(item.value));
            y -= 17;
        }
    }

    /** Extracts and groups chart values from the existing five-column report table. */
    private static ListInterface<ChartItem> extractChartItems(String report,
            ChartType chartType) {
        ListInterface<ChartItem> items = new ArrayList<>();
        if (report == null || chartType == null) {
            return items;
        }

        for (String rawLine : report.split("\\R")) {
            ListInterface<String> columns = parseTableColumns(rawLine.trim());
            if (columns.getNumberOfEntries() != 5) {
                continue;
            }

            Double amount = parseAmount(columns.getEntry(5));
            if (amount == null) {
                continue;
            }

            String label = chartType == ChartType.OUTSTANDING_BALANCE
                    ? columns.getEntry(1) : columns.getEntry(4);
            addChartAmount(items, label, amount);
        }

        sortChartItemsByAmountDescending(items);
        return items;
    }

    private static void addChartAmount(ListInterface<ChartItem> items, String label,
            double amount) {
        String normalizedLabel = label == null || label.trim().isEmpty()
                ? "Unspecified" : label.trim();
        for (ChartItem item : items) {
            if (item.label.equalsIgnoreCase(normalizedLabel)) {
                item.value += amount;
                return;
            }
        }
        items.add(new ChartItem(normalizedLabel, amount));
    }

    private static void sortChartItemsByAmountDescending(ListInterface<ChartItem> items) {
        for (int end = items.getNumberOfEntries(); end > 1; end--) {
            for (int position = 1; position < end; position++) {
                if (items.getEntry(position).value
                        < items.getEntry(position + 1).value) {
                    ChartItem temporary = items.getEntry(position);
                    items.replace(position, items.getEntry(position + 1));
                    items.replace(position + 1, temporary);
                }
            }
        }
    }

    private static Double parseAmount(String value) {
        if (value == null) {
            return null;
        }
        try {
            String normalized = value.replace("RM", "").trim();
            int lastPeriod = normalized.lastIndexOf('.');
            int lastComma = normalized.lastIndexOf(',');
            if (lastComma > lastPeriod) {
                normalized = normalized.replace(".", "").replace(',', '.');
            } else {
                normalized = normalized.replace(",", "");
            }
            double amount = Double.parseDouble(normalized);
            return Double.isNaN(amount) || Double.isInfinite(amount) || amount < 0.0
                    ? null : amount;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String formatAmount(double amount) {
        return String.format("RM %.2f", amount);
    }

    private static ChartType inferChartType(String title) {
        if ("Outstanding Balance Report".equalsIgnoreCase(title)) {
            return ChartType.OUTSTANDING_BALANCE;
        }
        if ("Payment Method Room Report".equalsIgnoreCase(title)) {
            return ChartType.PAYMENT_METHOD;
        }
        return null;
    }

    private static String chartTitle(ChartType chartType) {
        return chartType == ChartType.OUTSTANDING_BALANCE
                ? "Outstanding Balance Chart (RM by Room)"
                : "Payment Method Chart (RM Billed)";
    }

    private static final class ChartItem {
        private final String label;
        private double value;

        private ChartItem(String label, double value) {
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
