package common.control;

import common.utility.Validation;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Verifies a member using data collected during registration. */
/**
 * @author Chee Weng
 */
public final class MemberIdentityControl {
    private MemberIdentityControl() {
    }

    public static boolean verify(String passport, String phoneNumber,
            String passportOrPhone) {
        if (Validation.isBlank(passport) || Validation.isBlank(passportOrPhone)) {
            return false;
        }
        String storedPhone = Validation.normalizePhoneNumber(phoneNumber);
        String suppliedPhone = Validation.normalizePhoneNumber(passportOrPhone);
        boolean phoneMatches = !storedPhone.isEmpty() && !suppliedPhone.isEmpty()
                && secureEquals(storedPhone, suppliedPhone);
        return secureEquals(passport.toLowerCase(),
                        passportOrPhone.trim().toLowerCase())
                || phoneMatches;
    }

    private static boolean secureEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
