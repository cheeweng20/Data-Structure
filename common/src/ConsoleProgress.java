package common.src;

import java.io.IOException;
import java.util.function.Supplier;

/**
 * Small, terminal-friendly progress indicator for synchronous console work.
 *
 * <p>The progress animation is intentionally deterministic: it gives the
 * user feedback while the operation is being prepared, runs the operation,
 * and only renders 100% after the operation returns. Set
 * {@code -Dcli.progress=false} to disable the animation without changing the
 * underlying operation.</p>
 */
public final class ConsoleProgress {
    private static final int BAR_WIDTH = 28;
    private static final long ANIMATION_MILLIS = 1200L;
    private static final String DISABLED_PROPERTY = "cli.progress";

    private ConsoleProgress() {
    }

    /** Runs a value-returning operation with staged progress feedback. */
    public static <T> T run(Supplier<T> operation, String... stages) {
        requireOperation(operation);
        if (!isEnabled()) {
            return operation.get();
        }

        ProgressRenderer renderer = new ProgressRenderer(normalizeStages(stages));
        renderer.beforeOperation();
        try {
            T result = operation.get();
            renderer.complete();
            return result;
        } catch (RuntimeException | Error exception) {
            renderer.abort();
            throw exception;
        }
    }

    /** Runs a void operation with staged progress feedback. */
    public static void run(Runnable operation, String... stages) {
        requireOperation(operation);
        if (!isEnabled()) {
            operation.run();
            return;
        }

        ProgressRenderer renderer = new ProgressRenderer(normalizeStages(stages));
        renderer.beforeOperation();
        try {
            operation.run();
            renderer.complete();
        } catch (RuntimeException | Error exception) {
            renderer.abort();
            throw exception;
        }
    }

    /**
     * Runs an operation that can fail with an {@link IOException}, preserving
     * the checked exception for the caller's existing error handling.
     */
    public static <T> T runIo(IoSupplier<T> operation, String... stages)
            throws IOException {
        requireOperation(operation);
        if (!isEnabled()) {
            return operation.get();
        }

        ProgressRenderer renderer = new ProgressRenderer(normalizeStages(stages));
        renderer.beforeOperation();
        try {
            T result = operation.get();
            renderer.complete();
            return result;
        } catch (IOException | RuntimeException | Error exception) {
            renderer.abort();
            throw exception;
        }
    }

    @FunctionalInterface
    public interface IoSupplier<T> {
        T get() throws IOException;
    }

    private static boolean isEnabled() {
        return !"false".equalsIgnoreCase(System.getProperty(DISABLED_PROPERTY, "true"));
    }

    private static String[] normalizeStages(String[] stages) {
        if (stages == null || stages.length == 0) {
            return new String[] {"Processing request..."};
        }

        String[] normalized = new String[stages.length];
        for (int i = 0; i < stages.length; i++) {
            normalized[i] = stages[i] == null || stages[i].trim().isEmpty()
                    ? "Processing request..." : stages[i].trim();
        }
        return normalized;
    }

    private static void requireOperation(Object operation) {
        if (operation == null) {
            throw new IllegalArgumentException("Progress operation is required.");
        }
    }

    private static final class ProgressRenderer {
        private final String[] stages;
        private int lastLineLength;

        private ProgressRenderer(String[] stages) {
            this.stages = stages;
        }

        private void beforeOperation() {
            render(0, stages[0]);
            long delay = ANIMATION_MILLIS / Math.max(1, stages.length - 1);
            for (int i = 1; i < stages.length; i++) {
                pause(delay);
                int percent = (int) Math.round(80.0 * i / (stages.length - 1));
                render(percent, stages[i]);
            }
        }

        private void complete() {
            render(100, "Complete");
            System.out.println();
            lastLineLength = 0;
        }

        private void abort() {
            clearLine();
            System.out.println();
            lastLineLength = 0;
        }

        private void render(int percent, String message) {
            int filled = (int) Math.round(BAR_WIDTH * percent / 100.0);
            String line = "[" + "#".repeat(filled)
                    + "-".repeat(BAR_WIDTH - filled) + "] "
                    + String.format("%3d%% %s", percent, message);
            clearLine();
            System.out.print(ConsoleStyle.info("\r" + line));
            System.out.flush();
            lastLineLength = line.length();
        }

        private void clearLine() {
            if (lastLineLength > 0) {
                System.out.print("\r" + " ".repeat(lastLineLength) + "\r");
            }
        }

        private void pause(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
