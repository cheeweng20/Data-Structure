package LoyaltyAndRewardsService.utility;

import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import adt.ArrayList;
import adt.ListInterface;

/**
 * Creates a dependency-free PDF copy of a console report and adds a simple chart.
 *
 * @author Chee Weng
 */
public final class ReportPdfExporter {
    public enum ChartType {
        EXPIRING_POINTS,
        POINTS_TRANSACTION
    }

    private static final double PAGE_WIDTH = 842;
    private static final double PAGE_HEIGHT = 595;
    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private ReportPdfExporter() {
    }

    public static Path export(String title, String report, ChartType chartType)
            throws IOException {
        ListInterface<ReportChart> charts = extractCharts(report, chartType);
        ListInterface<String> pageStreams = createPageStreams(title, report, charts);

        Path outputDirectory = Path.of("output", "pdf");
        Files.createDirectories(outputDirectory);
        String fileName = slugify(title) + "-" + LocalDateTime.now().format(FILE_TIME) + ".pdf";
        Path outputPath = outputDirectory.resolve(fileName).toAbsolutePath();
        Files.write(outputPath, buildPdf(pageStreams));
        return outputPath;
    }

    public static boolean open(Path pdfPath) throws IOException {
        if (!Desktop.isDesktopSupported()
                || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            return false;
        }
        Desktop.getDesktop().open(pdfPath.toFile());
        return true;
    }

    private static ListInterface<String> createPageStreams(String title, String report,
            ListInterface<ReportChart> charts) {
        ListInterface<String> streams = new ArrayList<>();
        int pageNumber = 1;
        PageCanvas page = createPage(title, pageNumber, charts);
        int tablePhase = -1;
        int tableRowIndex = 0;
        ListInterface<String> tableHeader = null;

        for (String rawLine : report.split("\\R", -1)) {
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
                if (heading.equalsIgnoreCase(title)) {
                    continue;
                }
                if (page.y - 27 < 42) {
                    streams.add(page.stream.toString());
                    page = createPage(title, ++pageNumber, new ArrayList<>());
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
                    page = createPage(title, ++pageNumber, new ArrayList<>());
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
                page = createPage(title, ++pageNumber, new ArrayList<>());
            }
            drawSummaryRow(page.stream, line, page.y);
            page.y -= 21;
        }

        streams.add(page.stream.toString());

        return streams;
    }

    private static PageCanvas createPage(String title, int pageNumber,
            ListInterface<ReportChart> charts) {
        StringBuilder stream = new StringBuilder();
        drawPageBackground(stream, title, pageNumber);
        if (pageNumber == 1) {
            drawCharts(stream, charts);
            return new PageCanvas(stream, 277);
        }
        return new PageCanvas(stream, 507);
    }

    private static void drawSectionHeading(StringBuilder stream, String heading, double y) {
        stream.append("0.88 0.93 0.98 rg 44 ").append(y - 5)
                .append(" 756 21 re f\n");
        appendText(stream, "F3", 10.5, 54, y + 1, 0.07, 0.20, 0.36, heading);
    }

    private static void drawSummaryRow(StringBuilder stream, String line, double y) {
        Matcher statusMatcher = Pattern.compile(
                "Pending:\\s*(\\d+),\\s*Approved:\\s*(\\d+),\\s*Rejected:\\s*(\\d+)",
                Pattern.CASE_INSENSITIVE).matcher(line);
        if (statusMatcher.matches()) {
            String[] labels = {"Pending", "Approved", "Rejected"};
            double cardWidth = 252;
            for (int index = 0; index < labels.length; index++) {
                double cardX = 44 + index * cardWidth;
                stream.append("1 1 1 rg ").append(cardX).append(' ')
                        .append(y - 5).append(' ').append(cardWidth - 4)
                        .append(" 18 re f\n");
                appendText(stream, "F3", 8, cardX + 10, y, 0.17, 0.27, 0.40,
                        labels[index]);
                appendText(stream, "F1", 8, cardX + 105, y, 0.20, 0.24, 0.30,
                        statusMatcher.group(index + 1));
            }
            return;
        }

        stream.append("1 1 1 rg 44 ").append(y - 5).append(" 756 18 re f\n");
        int separator = line.indexOf(':');
        if (separator > 0 && separator < 32) {
            String label = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            appendText(stream, "F3", 8, 54, y, 0.17, 0.27, 0.40, label);
            appendText(stream, "F1", 8, 205, y, 0.20, 0.24, 0.30, value);
        } else {
            appendText(stream, "F1", 8, 54, y, 0.20, 0.24, 0.30, line);
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

    private static void drawPageBackground(StringBuilder stream, String title, int pageNumber) {
        stream.append("0.96 0.97 0.99 rg 0 0 ")
                .append(PAGE_WIDTH).append(' ').append(PAGE_HEIGHT).append(" re f\n");
        stream.append("0.07 0.13 0.24 rg 0 535 ")
                .append(PAGE_WIDTH).append(" 60 re f\n");
        appendText(stream, "F1", 19, 42, 557, 1, 1, 1, title);
        appendText(stream, "F1", 8, 735, 558, 0.82, 0.87, 0.95,
                "Page " + pageNumber);
        stream.append("0.78 0.82 0.88 RG 0.6 w 38 31 m 804 31 l S\n");
        appendText(stream, "F1", 7, 42, 18, 0.38, 0.43, 0.50,
                "Loyalty and Rewards Service");
    }

    private static void drawCharts(StringBuilder stream, ListInterface<ReportChart> charts) {
        drawFullWidthChart(stream, charts.getEntry(1));
    }

    private static void drawFullWidthChart(StringBuilder stream, ReportChart chart) {
        stream.append("1 1 1 rg 38 300 766 215 re f\n");
        appendText(stream, "F1", 12, 52, 491, 0.07, 0.13, 0.24, chart.title);

        if (chart.items.isEmpty()) {
            appendText(stream, "F1", 10, 52, 450, 0.45, 0.49, 0.55,
                    "No chart data is available for this report.");
            return;
        }

        int maximum = 1;
        for (ChartItem item : chart.items) {
            maximum = Math.max(maximum, item.value);
        }

        int displayedItems = Math.min(chart.items.getNumberOfEntries(), 8);
        double y = 463;
        for (int index = 0; index < displayedItems; index++) {
            ChartItem item = chart.items.getEntry(index + 1);
            double barWidth = 480.0 * item.value / maximum;
            appendText(stream, "F1", 8, 52, y + 2, 0.20, 0.24, 0.30,
                    abbreviate(item.label, 22));
            stream.append("0.20 0.55 0.86 rg 205 ").append(y)
                    .append(' ').append(Math.max(2, barWidth)).append(" 12 re f\n");
            appendText(stream, "F1", 8, 694, y + 2, 0.20, 0.24, 0.30,
                    String.valueOf(item.value));
            y -= 21;
        }
    }

    private static ListInterface<ReportChart> extractCharts(String report, ChartType chartType) {
        ListInterface<ReportChart> charts = new ArrayList<>();
        charts.add(new ReportChart(chartTitle(chartType),
                chartType == ChartType.POINTS_TRANSACTION
                        ? extractTransactionChartItems(report)
                        : extractSimpleChartItems(report)));
        return charts;
    }

    private static ListInterface<ChartItem> extractSimpleChartItems(String report) {
        ListInterface<ChartItem> items = new ArrayList<>();
        String[] lines = report.split("\\R");
        for (String line : lines) {
            ListInterface<String> columns = parseTableColumns(line);
            if (columns.getNumberOfEntries() != 4) {
                continue;
            }

            Integer value = parseInteger(columns.getEntry(3));
            if (value == null) {
                continue;
            }
            items.add(new ChartItem(columns.getEntry(1), value));
        }
        return items;
    }

    private static ListInterface<ChartItem> extractTransactionChartItems(String report) {
        ListInterface<DatedPointTotal> transactionPointsByDate = new ArrayList<>();

        for (String line : report.split("\\R")) {
            ListInterface<String> columns = parseTableColumns(line);
            if (columns.getNumberOfEntries() != 4) {
                continue;
            }

            Integer points = parseInteger(columns.getEntry(3));
            if (points != null) {
                addPointsForDate(transactionPointsByDate, columns.getEntry(4), points);
            }
        }

        ListInterface<ChartItem> transactionPoints = new ArrayList<>();
        for (DatedPointTotal entry : transactionPointsByDate) {
            transactionPoints.add(new ChartItem(entry.date, entry.points));
        }
        return transactionPoints;
    }

    private static void addPointsForDate(ListInterface<DatedPointTotal> totals,
            String date, int points) {
        for (DatedPointTotal total : totals) {
            if (total.date.equals(date)) {
                total.points += points;
                return;
            }
        }
        totals.add(new DatedPointTotal(date, points));
    }

    private static ListInterface<String> parseTableColumns(String line) {
        ListInterface<String> columns = new ArrayList<>();
        if (!line.trim().startsWith("|")) {
            return columns;
        }
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
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String chartTitle(ChartType chartType) {
        return chartType == ChartType.EXPIRING_POINTS
                ? "Points due to expire" : "Points earned by date";
    }

    private static byte[] buildPdf(ListInterface<String> pageStreams) throws IOException {
        int pageCount = pageStreams.getNumberOfEntries();
        int objectCount = 5 + pageCount * 2;
        ListInterface<byte[]> objects = new ArrayList<>();

        objects.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        StringBuilder kids = new StringBuilder();
        for (int index = 0; index < pageCount; index++) {
            kids.append(6 + index * 2).append(" 0 R ");
        }
        objects.add(bytes("<< /Type /Pages /Kids [" + kids + "] /Count "
                + pageCount + " >>"));
        objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));
        objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>"));
        objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"));

        for (int index = 0; index < pageCount; index++) {
            int contentObject = 7 + index * 2;
            String pageObject = "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "
                    + PAGE_WIDTH + " " + PAGE_HEIGHT + "] "
                    + "/Resources << /Font << /F1 3 0 R /F2 4 0 R /F3 5 0 R >> >> "
                    + "/Contents " + contentObject + " 0 R >>";
            objects.add(bytes(pageObject));

            byte[] streamBytes = bytes(pageStreams.getEntry(index + 1));
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            content.write(bytes("<< /Length " + streamBytes.length + " >>\nstream\n"));
            content.write(streamBytes);
            content.write(bytes("\nendstream"));
            objects.add(content.toByteArray());
        }

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write(bytes("%PDF-1.4\n%1234\n"));
        long[] offsets = new long[objectCount + 1];
        for (int index = 0; index < objects.getNumberOfEntries(); index++) {
            int objectNumber = index + 1;
            offsets[objectNumber] = pdf.size();
            pdf.write(bytes(objectNumber + " 0 obj\n"));
            pdf.write(objects.getEntry(index + 1));
            pdf.write(bytes("\nendobj\n"));
        }

        long xrefOffset = pdf.size();
        pdf.write(bytes("xref\n0 " + (objectCount + 1) + "\n"));
        pdf.write(bytes("0000000000 65535 f \n"));
        for (int objectNumber = 1; objectNumber <= objectCount; objectNumber++) {
            pdf.write(bytes(String.format("%010d 00000 n \n", offsets[objectNumber])));
        }
        pdf.write(bytes("trailer\n<< /Size " + (objectCount + 1)
                + " /Root 1 0 R >>\nstartxref\n" + xrefOffset + "\n%%EOF\n"));
        return pdf.toByteArray();
    }

    private static void appendText(StringBuilder stream, String font, double size,
            double x, double y, double red, double green, double blue, String value) {
        stream.append("BT /").append(font).append(' ').append(size).append(" Tf ")
                .append(red).append(' ').append(green).append(' ').append(blue)
                .append(" rg 1 0 0 1 ").append(x).append(' ').append(y)
                .append(" Tm (").append(escapePdfText(value)).append(") Tj ET\n");
    }

    private static String escapePdfText(String value) {
        String ascii = value.replaceAll("[^\\x20-\\x7E]", "?");
        return ascii.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private static String slugify(String value) {
        String slug = value.toLowerCase().replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return slug.isEmpty() ? "loyalty-report" : slug;
    }

    private static String abbreviate(String value, int maximumLength) {
        if (value.length() <= maximumLength) {
            return value;
        }
        return value.substring(0, maximumLength - 3) + "...";
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static final class ChartItem {
        private final String label;
        private final int value;

        private ChartItem(String label, int value) {
            this.label = label;
            this.value = value;
        }
    }

    private static final class ReportChart {
        private final String title;
        private final ListInterface<ChartItem> items;

        private ReportChart(String title, ListInterface<ChartItem> items) {
            this.title = title;
            this.items = items;
        }
    }

    private static final class DatedPointTotal {
        private final String date;
        private int points;

        private DatedPointTotal(String date, int points) {
            this.date = date;
            this.points = points;
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
