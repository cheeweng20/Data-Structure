package VIPPriorityRoomAllocation.utility;

import java.awt.Desktop;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Creates a PDF copy of a reservation report with one bar chart.
public final class ReportPdfExporter {

    public enum ChartType {
        RESERVATION_STATUS,
        TIER_ALLOCATION
    }

    private static final double PAGE_WIDTH = 842;
    private static final double PAGE_HEIGHT = 595;
    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private ReportPdfExporter() {
    }

    public static Path export(String title, String report, ChartType chartType)
            throws IOException {
        List<ChartItem> chartItems = extractChartItems(report, chartType);
        String pageStream = createPageStream(title, report, chartItems, chartType);

        Path outputDirectory = Path.of("output", "pdf");
        Files.createDirectories(outputDirectory);
        String fileName = slugify(title) + "-" + LocalDateTime.now().format(FILE_TIME) + ".pdf";
        Path outputPath = outputDirectory.resolve(fileName).toAbsolutePath();
        Files.write(outputPath, buildPdf(pageStream));
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

    private static String createPageStream(String title, String report,
            List<ChartItem> chartItems, ChartType chartType) {
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

    private static void drawChart(StringBuilder stream, String title, List<ChartItem> items) {
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

    private static List<ChartItem> extractChartItems(String report, ChartType chartType) {
        List<ChartItem> items = new ArrayList<>();
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

            List<String> columns = parseTableColumns(line);
            if (columns.size() != 2 || columns.get(0).equalsIgnoreCase("Label")) {
                continue;
            }

            Integer value = parseInteger(columns.get(1));
            if (value != null) {
                items.add(new ChartItem(columns.get(0), value));
            }
        }
        return items;
    }

    private static List<String> parseTableColumns(String line) {
        List<String> columns = new ArrayList<>();
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

    private static byte[] buildPdf(String pageStream) throws IOException {
        List<byte[]> objects = new ArrayList<>();
        objects.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        objects.add(bytes("<< /Type /Pages /Kids [6 0 R] /Count 1 >>"));
        objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));
        objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>"));
        objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>"));
        objects.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "
                + PAGE_WIDTH + " " + PAGE_HEIGHT + "] "
                + "/Resources << /Font << /F1 3 0 R /F2 4 0 R /F3 5 0 R >> >> "
                + "/Contents 7 0 R >>"));

        byte[] streamBytes = bytes(pageStream);
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        content.write(bytes("<< /Length " + streamBytes.length + " >>\nstream\n"));
        content.write(streamBytes);
        content.write(bytes("\nendstream"));
        objects.add(content.toByteArray());

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write(bytes("%PDF-1.4\n%1234\n"));
        long[] offsets = new long[objects.size() + 1];
        for (int index = 0; index < objects.size(); index++) {
            int objectNumber = index + 1;
            offsets[objectNumber] = pdf.size();
            pdf.write(bytes(objectNumber + " 0 obj\n"));
            pdf.write(objects.get(index));
            pdf.write(bytes("\nendobj\n"));
        }

        long xrefOffset = pdf.size();
        pdf.write(bytes("xref\n0 " + (objects.size() + 1) + "\n"));
        pdf.write(bytes("0000000000 65535 f \n"));
        for (int objectNumber = 1; objectNumber <= objects.size(); objectNumber++) {
            pdf.write(bytes(String.format("%010d 00000 n \n", offsets[objectNumber])));
        }
        pdf.write(bytes("trailer\n<< /Size " + (objects.size() + 1)
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
        return slug.isEmpty() ? "reservation-report" : slug;
    }

    private static String abbreviate(String value, int maximumLength) {
        if (value.length() <= maximumLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maximumLength - 3)) + "...";
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
}
