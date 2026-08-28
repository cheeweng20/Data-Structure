package common.ui;

import java.util.Scanner;

public class InputHelper {

    private static final String CLEAR_SCREEN = "\u001B[H\u001B[2J";

    private InputHelper() {
    }

    /** Clears the current terminal before drawing the next screen. */
    public static void clearScreen() {
        System.out.print(CLEAR_SCREEN);
        System.out.flush();
    }

    /** Keeps an operation result visible until the user is ready to continue. */
    public static void pressEnterToContinue(Scanner input) {
        System.out.print(ConsoleStyle.inputPrompt("\nPress Enter to continue..."));
        if (input.hasNextLine()) {
            input.nextLine();
        }
        System.out.print(ConsoleStyle.endInput());
    }

    public static int inputInt(Scanner input, String prompt) {
        while (true) {
            System.out.print(ConsoleStyle.inputPrompt(prompt));
            String value = readLine(input).trim();
            System.out.print(ConsoleStyle.endInput());
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                System.out.println(ConsoleStyle.error("Invalid Input, Please Try Again !"));
            }
        }
    }

    public static String inputString(Scanner input, String prompt) {
        System.out.print(ConsoleStyle.inputPrompt(prompt));
        String userInput = readLine(input);
        System.out.print(ConsoleStyle.endInput());
        return userInput;
    }

    /** Reads one complete line and distinguishes EOF from a blank input. */
    public static String readLine(Scanner input) {
        if (!input.hasNextLine()) {
            throw new EndOfInputException();
        }
        return input.nextLine();
    }

    /** Control-flow signal used to leave the active console module on EOF. */
    public static final class EndOfInputException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private EndOfInputException() {
            super("Console input has ended.");
        }
    }
}
