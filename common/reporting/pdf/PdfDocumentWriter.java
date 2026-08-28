package common.reporting.pdf;

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

/** Writes dependency-free landscape PDF documents from prepared page streams. */
public final class PdfDocumentWriter {

    public static final double PAGE_WIDTH = 842;
    public static final double PAGE_HEIGHT = 595;
    private static final DateTimeFormatter FILE_TIME =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private PdfDocumentWriter() {
    }

    public static Path write(String title, String fallbackSlug, List<String> pageStreams)
            throws IOException {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("PDF title cannot be blank.");
        }
        if (pageStreams == null || pageStreams.isEmpty()) {
            throw new IllegalArgumentException("PDF must contain at least one page.");
        }

        Path outputDirectory = Path.of("output", "pdf");
        Files.createDirectories(outputDirectory);
        String fileName = slugify(title, fallbackSlug) + "-"
                + LocalDateTime.now().format(FILE_TIME) + ".pdf";
        Path outputPath = outputDirectory.resolve(fileName).toAbsolutePath();
        Files.write(outputPath, buildPdf(pageStreams));
        return outputPath;
    }

    public static boolean open(Path pdfPath) throws IOException {
        if (pdfPath == null || !Desktop.isDesktopSupported()
                || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            return false;
        }
        Desktop.getDesktop().open(pdfPath.toFile());
        return true;
    }

    public static void appendText(StringBuilder stream, String font, double size,
            double x, double y, double red, double green, double blue, String value) {
        stream.append("BT /").append(font).append(' ').append(size).append(" Tf ")
                .append(red).append(' ').append(green).append(' ').append(blue)
                .append(" rg 1 0 0 1 ").append(x).append(' ').append(y)
                .append(" Tm (").append(escapeText(value)).append(") Tj ET\n");
    }

    public static String abbreviate(String value, int maximumLength) {
        if (value == null) {
            return "";
        }
        if (maximumLength < 4 || value.length() <= maximumLength) {
            return value.length() <= maximumLength
                    ? value : value.substring(0, Math.max(0, maximumLength));
        }
        return value.substring(0, maximumLength - 3) + "...";
    }

    private static byte[] buildPdf(List<String> pageStreams) throws IOException {
        int pageCount = pageStreams.size();
        int objectCount = 5 + pageCount * 2;
        List<byte[]> objects = new ArrayList<>();

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

            byte[] streamBytes = bytes(pageStreams.get(index));
            ByteArrayOutputStream content = new ByteArrayOutputStream();
            content.write(bytes("<< /Length " + streamBytes.length + " >>\nstream\n"));
            content.write(streamBytes);
            content.write(bytes("\nendstream"));
            objects.add(content.toByteArray());
        }

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write(bytes("%PDF-1.4\n%1234\n"));
        long[] offsets = new long[objectCount + 1];
        for (int index = 0; index < objects.size(); index++) {
            int objectNumber = index + 1;
            offsets[objectNumber] = pdf.size();
            pdf.write(bytes(objectNumber + " 0 obj\n"));
            pdf.write(objects.get(index));
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

    private static String escapeText(String value) {
        String ascii = (value == null ? "" : value).replaceAll("[^\\x20-\\x7E]", "?");
        return ascii.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private static String slugify(String value, String fallbackSlug) {
        String slug = value.toLowerCase().replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (!slug.isEmpty()) {
            return slug;
        }
        return fallbackSlug == null || fallbackSlug.isBlank()
                ? "report" : fallbackSlug;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.ISO_8859_1);
    }
}
