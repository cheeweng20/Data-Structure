package common.src;

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
