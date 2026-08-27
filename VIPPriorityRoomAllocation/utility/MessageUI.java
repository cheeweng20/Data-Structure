package VIPPriorityRoomAllocation.utility;

import common.src.ConsoleStyle;

// Displays messages shared by the reservation boundary classes.
/**
 * @author Wan Yin
 */
public final class MessageUI {

    private MessageUI() {
    }

    public static void displaySuccess(String message) {
        System.out.println(ConsoleStyle.success("Success: " + message));
    }

    public static void displayError(String message) {
        System.out.println(ConsoleStyle.error("Error: " + message));
    }

    public static void displayInfo(String message) {
        System.out.println(ConsoleStyle.infoBadge() + " " + message);
    }
}
