package common.src;

/** Shared TARUMT Resort logo used by every portal and service. */
public final class Logo {
    private static final String BORDER = "+--------------------------------------------------------------+";

    private Logo() {
    }

    public static void displayMain() {
        display();
    }

    public static void displayLoyaltyAndRewardsService() {
        display();
    }

    public static void displayHousekeepingAndTaskLog() {
        display();
    }

    public static void displayService(String ignoredServiceName) {
        display();
    }

    private static void display() {
        System.out.println(ConsoleStyle.logo(
                BORDER + "\n"
                + "|                         TARUMT RESORT                        |\n"
                + BORDER));
    }
}
