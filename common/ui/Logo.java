package common.ui;

/** Shared TARUMT Resort logo used by every portal and service. */
public final class Logo {
    private Logo() {
    }

    public static void display() {
        System.out.println(ConsoleStyle.logo(
                "  _____  _    ____  _   _ __  __ _____   ____  _____ ____   ___  ____ _____ \r\n" + //
                                        " |_   _|/ \\  |  _ \\| | | |  \\/  |_   _| |  _ \\| ____/ ___| / _ \\|  _ \\_   _|\r\n" + //
                                        "   | | / _ \\ | |_) | | | | |\\/| | | |   | |_) |  _| \\___ \\| | | | |_) || |  \r\n" + //
                                        "   | |/ ___ \\|  _ <| |_| | |  | | | |   |  _ <| |___ ___) | |_| |  _ < | |  \r\n" + //
                                        "   |_/_/   \\_\\_| \\_\\\\___/|_|  |_| |_|   |_| \\_\\_____|____/ \\___/|_| \\_\\|_|  \r\n" + //
                                        "                                                                            "));
    }
}
