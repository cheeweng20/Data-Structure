package common.ui;

/** Shared TARUMT Resort logo used by every portal and service. */
public final class Logo {
    private static final String BORDER = "+--------------------------------------------------------------+";

    private Logo() {
    }

    public static void display() {
        System.out.println(ConsoleStyle.logo(
                BORDER + "\n"
                + "|                         TARUMT RESORT                        |\n"
                + BORDER));
    }
}
