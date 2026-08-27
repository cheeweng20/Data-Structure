package common.src;

import java.io.IOException;
import java.util.function.Supplier;

/** Reusable lightweight animations for the console UI. */
public final class ConsoleAnimation {
    private static final String[] SPINNER_FRAMES = {"|", "/", "-", "\\"};
    private static final long FRAME_MILLIS = 140L;
    private static final int ROOM_ASSIGNMENT_FRAMES = 3;

    private ConsoleAnimation() {
    }

    /** Shows a short pulsing status sequence, primarily for startup screens. */
    public static void pulse(String message, int cycles) {
        if (!isEnabled()) {
            return;
        }

        LineRenderer renderer = new LineRenderer();
        int safeCycles = Math.max(1, cycles);
        for (int i = 0; i < safeCycles; i++) {
            renderer.render(message + ".".repeat((i % 3) + 1));
            pause(180L);
        }
        renderer.finish();
    }

    /** Runs a short startup sequence without changing application state. */
    public static void startup() {
        pulse("Initializing TARUMT Resort system", 2);
        pulse("Loading reservation records", 2);
        pulse("Loading loyalty records", 2);
    }

    /** Shows a spinner while a value-returning operation is prepared. */
    public static <T> T runWithSpinner(Supplier<T> operation, String message) {
        requireOperation(operation);
        if (!isEnabled()) {
            return operation.get();
        }

        LineRenderer renderer = spin(message);
        try {
            T result = operation.get();
            renderer.finishWith("Success: " + message + " complete", true);
            return result;
        } catch (RuntimeException | Error exception) {
            renderer.finishWith("Error: " + message + " failed", false);
            throw exception;
        }
    }

    /** Shows a spinner while a void operation is prepared. */
    public static void runWithSpinner(Runnable operation, String message) {
        requireOperation(operation);
        if (!isEnabled()) {
            operation.run();
            return;
        }

        LineRenderer renderer = spin(message);
        try {
            operation.run();
            renderer.finishWith("Success: " + message + " complete", true);
        } catch (RuntimeException | Error exception) {
            renderer.finishWith("Error: " + message + " failed", false);
            throw exception;
        }
    }

    /** Spinner variant that preserves checked I/O failures. */
    public static <T> T runIoWithSpinner(IoSupplier<T> operation, String message)
            throws IOException {
        requireOperation(operation);
        if (!isEnabled()) {
            return operation.get();
        }

        LineRenderer renderer = spin(message);
        try {
            T result = operation.get();
            renderer.finishWith("Success: " + message + " complete", true);
            return result;
        } catch (IOException | RuntimeException | Error exception) {
            renderer.finishWith("Error: " + message + " failed", false);
            throw exception;
        }
    }

    /** Displays a small per-room assignment animation. */
    public static void roomAssignment(String roomNumber, String guestName) {
        if (!isEnabled()) {
            return;
        }

        LineRenderer renderer = new LineRenderer();
        String message = "Assigning room " + roomNumber + " to " + guestName;
        for (int i = 0; i < ROOM_ASSIGNMENT_FRAMES; i++) {
            renderer.render(message + ".".repeat((i % 3) + 1));
            pause(FRAME_MILLIS);
        }
        renderer.finishWith("Success: " + message, true);
    }

    /** Prints a consistent success transition for an operation result. */
    public static void success(String message) {
        System.out.println(ConsoleStyle.success("Success: " + message));
    }

    /** Prints a consistent failure transition for an operation result. */
    public static void error(String message) {
        System.out.println(ConsoleStyle.error("Error: " + message));
    }

    private static LineRenderer spin(String message) {
        LineRenderer renderer = new LineRenderer();
        for (String frame : SPINNER_FRAMES) {
            renderer.render(frame + " " + message + "...");
            pause(FRAME_MILLIS);
        }
        return renderer;
    }

    private static boolean isEnabled() {
        return !"false".equalsIgnoreCase(System.getProperty("cli.progress", "true"));
    }

    private static void requireOperation(Object operation) {
        if (operation == null) {
            throw new IllegalArgumentException("Animation operation is required.");
        }
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    public interface IoSupplier<T> {
        T get() throws IOException;
    }

    private static final class LineRenderer {
        private int lastLineLength;

        private void render(String message) {
            clearLine();
            String line = message == null ? "" : message;
            System.out.print(ConsoleStyle.info("\r" + line));
            System.out.flush();
            lastLineLength = line.length();
        }

        private void finish() {
            clearLine();
            System.out.println();
            lastLineLength = 0;
        }

        private void finishWith(String message, boolean success) {
            clearLine();
            String line = message == null ? "" : message;
            String styled = success ? ConsoleStyle.success("\r" + line)
                    : ConsoleStyle.error("\r" + line);
            System.out.print(styled);
            System.out.println();
            System.out.flush();
            lastLineLength = 0;
        }

        private void clearLine() {
            if (lastLineLength > 0) {
                System.out.print("\r" + " ".repeat(lastLineLength) + "\r");
            }
        }
    }
}
