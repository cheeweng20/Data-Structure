package HousekeepingAndTaskLog.utility;

import adt.ListInterface;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Generates a PDF report and a bar chart without external libraries. */
public final class HousekeepingReportPdfExporter {

    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private HousekeepingReportPdfExporter() {
    }

    public static Path export(String title, String report, ListInterface<String> labels,
            ListInterface<Integer> values) throws IOException {
        Path directory = Path.of("output", "pdf");
        Files.createDirectories(directory);
        Path path = directory.resolve(slugify(title) + "-"
                + LocalDateTime.now().format(FILE_TIME) + ".pdf").toAbsolutePath();
        Files.write(path, pdf(title, report, labels, values));
        return path;
    }

    public static boolean open(Path path) throws IOException {
        if (!Desktop.isDesktopSupported()
                || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            return false;
        }
        Desktop.getDesktop().open(path.toFile());
        return true;
    }

    private static byte[] pdf(String title, String report, ListInterface<String> labels,
            ListInterface<Integer> values) {
        StringBuilder content = new StringBuilder();
        drawText(content, 18, 36, 560, title);
        drawTable(content, report);
        drawText(content, 12, 510, 560, "Task Status Chart");
        int maximum = 1;
        for (int i = 1; i <= values.getNumberOfEntries(); i++) {
            maximum = Math.max(maximum, values.getEntry(i));
        }
        for (int i = 1; i <= labels.getNumberOfEntries() && i <= values.getNumberOfEntries(); i++) {
            int barY = 525 - (i - 1) * 55;
            int width = values.getEntry(i) * 230 / maximum;
            content.append("0.12 0.35 0.62 rg\n500 ").append(barY).append(" ")
                    .append(width).append(" 18 re f\n");
            drawText(content, 8, 500, barY - 12,
                    labels.getEntry(i) + " (" + values.getEntry(i) + ")");
        }

        byte[] stream = content.toString().getBytes(StandardCharsets.ISO_8859_1);
        String[] objects = {
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 842 595] "
                    + "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
            "<< /Length " + stream.length + " >>\nstream\n" + content + "endstream"
        };
        StringBuilder document = new StringBuilder("%PDF-1.4\n");
        int[] offsets = new int[objects.length + 1];
        for (int i = 0; i < objects.length; i++) {
            offsets[i + 1] = document.toString().getBytes(StandardCharsets.ISO_8859_1).length;
            document.append(i + 1).append(" 0 obj\n").append(objects[i]).append("\nendobj\n");
        }
        int xref = document.toString().getBytes(StandardCharsets.ISO_8859_1).length;
        document.append("xref\n0 ").append(objects.length + 1).append("\n0000000000 65535 f \n");
        for (int i = 1; i <= objects.length; i++) {
            document.append(String.format("%010d 00000 n \n", offsets[i]));
        }
        document.append("trailer\n<< /Size ").append(objects.length + 1)
                .append(" /Root 1 0 R >>\nstartxref\n").append(xref).append("\n%%EOF");
        return document.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private static void drawText(StringBuilder content, int size, int x, int y, String text) {
        content.append("BT /F1 ").append(size).append(" Tf ").append(x).append(" ").append(y)
                .append(" Td (").append(escape(text)).append(") Tj ET\n");
    }

    private static void drawTable(StringBuilder content, String report) {
        List<String[]> rows = new ArrayList<>();

        for (String line : report.split("\\R")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("|") || trimmed.contains("REPORT")) {
                continue;
            }
            String[] rawColumns = trimmed.substring(1, trimmed.length() - 1).split("\\|", -1);
            String[] columns = new String[rawColumns.length];
            for (int index = 0; index < rawColumns.length; index++) {
                columns[index] = rawColumns[index].trim();
            }
            rows.add(columns);
        }

        if (rows.isEmpty()) {
            drawText(content, 10, 36, 525, "No task records found.");
            return;
        }

        int columnCount = rows.get(0).length;
        int rowHeight = columnCount == 2 ? 24 : 18;
        int rowCount = Math.min(rows.size(), 22);
        int x = 36;
        int top = 530;
        int width = 430;
        int columnWidth = width / columnCount;

        content.append("0.90 g\n").append(x).append(" ").append(top - rowHeight)
                .append(" ").append(width).append(" ").append(rowHeight).append(" re f\n")
                .append("0 g 0 0 0 RG 0.6 w\n");
        for (int row = 0; row <= rowCount; row++) {
            int y = top - row * rowHeight;
            content.append(x).append(" ").append(y).append(" m ")
                    .append(x + width).append(" ").append(y).append(" l S\n");
        }
        for (int column = 0; column <= columnCount; column++) {
            int columnX = x + column * columnWidth;
            content.append(columnX).append(" ").append(top).append(" m ")
                    .append(columnX).append(" ").append(top - rowCount * rowHeight)
                    .append(" l S\n");
        }
        for (int row = 0; row < rowCount; row++) {
            String[] columns = rows.get(row);
            for (int column = 0; column < columns.length; column++) {
                int size = columnCount == 2 ? 9 : 6;
                drawText(content, size, x + column * columnWidth + 4,
                        top - row * rowHeight - rowHeight + 7,
                        abbreviate(columns[column], columnCount == 2 ? 25 : 16));
            }
        }
    }

    private static String abbreviate(String value, int maximumLength) {
        return value.length() <= maximumLength
                ? value : value.substring(0, maximumLength - 3) + "...";
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private static String slugify(String value) {
        return value.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
