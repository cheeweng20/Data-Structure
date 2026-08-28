package common.control;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Small, dependency-free staff credential policy used by the top-level portal.
 *
 * <p>Production credentials can be configured with the
 * {@code RESORT_STAFF_USERNAME} and {@code RESORT_STAFF_PASSWORD} environment
 * variables. Missing credentials fail closed unless {@code RESORT_DEMO_MODE}
 * is explicitly set to {@code true}; demo mode then enables {@code staff} and
 * {@code staff123}. The explicit constructor supports isolated tests.</p>
 */
public final class StaffAuthenticationControl {
    public static final String USERNAME_ENV = "RESORT_STAFF_USERNAME";
    public static final String PASSWORD_ENV = "RESORT_STAFF_PASSWORD";
    public static final String DEMO_MODE_ENV = "RESORT_DEMO_MODE";
    public static final String DEFAULT_USERNAME = "staff";
    public static final String DEFAULT_PASSWORD = "staff123";
    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    private final String expectedUsername;
    private final String expectedPassword;
    private final int maxAttempts;
    private final boolean configured;

    /** Builds a policy from environment configuration and demo fallbacks. */
    public StaffAuthenticationControl() {
        String username = System.getenv(USERNAME_ENV);
        String password = System.getenv(PASSWORD_ENV);
        boolean demoMode = "true".equalsIgnoreCase(System.getenv(DEMO_MODE_ENV));
        if (demoMode && (username == null || username.isBlank())
                && (password == null || password.isBlank())) {
            username = DEFAULT_USERNAME;
            password = DEFAULT_PASSWORD;
        }
        expectedUsername = username;
        expectedPassword = password;
        maxAttempts = DEFAULT_MAX_ATTEMPTS;
        configured = username != null && !username.isBlank()
                && password != null && !password.isBlank();
    }

    /** Builds a three-attempt policy with explicit credentials for tests. */
    public StaffAuthenticationControl(String username, String password) {
        this(username, password, DEFAULT_MAX_ATTEMPTS);
    }

    /** Builds a policy with explicit credentials and an attempt bound. */
    public StaffAuthenticationControl(String username, String password, int maxAttempts) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Staff username must not be blank.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Staff password must not be blank.");
        }
        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("Maximum attempts must be positive.");
        }
        expectedUsername = username;
        expectedPassword = password;
        this.maxAttempts = maxAttempts;
        configured = true;
    }

    /** Returns true only when both supplied credentials match the policy. */
    public boolean authenticate(String username, String password) {
        return configured && secureEquals(expectedUsername, username)
                && secureEquals(expectedPassword, password);
    }

    public boolean isConfigured() {
        return configured;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    private static boolean secureEquals(String expected, String actual) {
        if (actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
