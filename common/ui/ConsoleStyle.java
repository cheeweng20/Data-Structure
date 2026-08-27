package common.ui;

/**
 * Shared ANSI styling for the command-line user interface.
 *
 * <p>Colours can be disabled with the {@code NO_COLOR} environment variable
 * or by running Java with {@code -Dcli.color=false}.</p>
 */
public final class ConsoleStyle {
    private static final String ESC = "\u001B[";
    private static final String RESET = ESC + "0m";
    private static final String BOLD = ESC + "1m";
    private static final String DIM = ESC + "2m";
    private static final String RED = ESC + "31m";
    private static final String GREEN = ESC + "32m";
    private static final String BLUE = ESC + "34m";
    private static final String MAGENTA = ESC + "35m";
    private static final String CYAN = ESC + "36m";
    private static final String WHITE = ESC + "37m";
    private static final String BLACK_BG = ESC + "40m";
    private static final String RED_BG = ESC + "41m";
    private static final String GREEN_BG = ESC + "42m";

    private static final boolean ENABLED = coloursEnabled();

    private ConsoleStyle() {
    }

    public static String logo(String text) {
        return style(text, BOLD + CYAN);
    }

    public static String title(String text) {
        return style(text, BOLD + CYAN);
    }

    public static String menu(String text) {
        return style(text, WHITE);
    }

    public static String tableHeader(String text) {
        return style(text, BOLD + BLUE);
    }

    public static String tableBorder(String text) {
        return style(text, DIM + CYAN);
    }

    public static String prompt(String text) {
        return style(text, BOLD + WHITE);
    }

    /** Styles an input prompt without applying a background colour. */
    public static String inputPrompt(String text) {
        return style(text, BOLD + WHITE);
    }

    /** Ends the input-box style after Scanner has finished reading input. */
    public static String endInput() {
        return ENABLED ? RESET : "";
    }

    public static String success(String text) {
        return style(text, BOLD + GREEN);
    }

    public static String error(String text) {
        return style(text, BOLD + RED);
    }

    public static String info(String text) {
        return style(text, MAGENTA);
    }

    public static String muted(String text) {
        return style(text, DIM);
    }

    /** Builds the shared numbered menu used by every resort screen. */
    public static String menuBox(String title, String... items) {
        int labelWidth = Math.max(40, title == null ? 0 : title.length());
        for (String item : items) {
            int separator = item.indexOf('|');
            String label = separator < 0 ? item : item.substring(separator + 1);
            labelWidth = Math.max(labelWidth, label.length());
        }

        // The number cell is formatted as "| %-4s|", so its content is five
        // characters wide (one leading space plus the four-character field).
        int totalWidth = 1 + 5 + 1 + 1 + labelWidth + 1 + 1;
        String outerBorder = "+" + "-".repeat(totalWidth - 2) + "+";
        String divider = "+-----+" + "-".repeat(labelWidth + 2) + "+";
        StringBuilder menu = new StringBuilder();
        menu.append(outerBorder).append('\n');
        menu.append(String.format("| %s |%n", centre(title == null ? "" : title,
                totalWidth - 4)));
        menu.append(divider).append('\n');
        for (String item : items) {
            int separator = item.indexOf('|');
            String number = separator < 0 ? "" : item.substring(0, separator);
            String label = separator < 0 ? item : item.substring(separator + 1);
            menu.append(String.format("| %-4s| %-" + labelWidth + "s |%n",
                    number, label));
        }
        menu.append(divider);
        return menu.toString();
    }

    private static String centre(String text, int width) {
        if (text.length() >= width) {
            return text.substring(0, width);
        }
        int leftPadding = (width - text.length()) / 2;
        return " ".repeat(leftPadding) + text
                + " ".repeat(width - leftPadding - text.length());
    }

    public static String infoBadge() {
        return badge("Info", BLACK_BG);
    }

    public static String successBadge() {
        return badge("Success", GREEN_BG);
    }

    public static String failedBadge() {
        return badge("Failed", RED_BG);
    }

    private static String style(String text, String ansiStyle) {
        if (!ENABLED || text == null || text.isEmpty()) {
            return text;
        }
        return ansiStyle + text + RESET;
    }

    private static String badge(String label, String background) {
        return style("[" + label + "]", BOLD + WHITE + background);
    }

    private static boolean coloursEnabled() {
        String property = System.getProperty("cli.color");
        if (property != null) {
            return Boolean.parseBoolean(property);
        }
        return System.getenv("NO_COLOR") == null;
    }
}
