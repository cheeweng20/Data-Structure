package common.control;

import LoyaltyAndRewardsService.entity.Member;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Verifies a member using data collected during registration. */
public final class MemberIdentityControl {
    private MemberIdentityControl() {
    }

    public static boolean verify(Member member, String passportOrPhone) {
        if (member == null || passportOrPhone == null || passportOrPhone.isBlank()) {
            return false;
        }
        String storedPhone = normalizePhone(member.getPhoneNumber());
        String suppliedPhone = normalizePhone(passportOrPhone);
        boolean phoneMatches = !storedPhone.isEmpty() && !suppliedPhone.isEmpty()
                && secureEquals(storedPhone, suppliedPhone);
        return secureEquals(member.getPassport().toLowerCase(),
                        passportOrPhone.trim().toLowerCase())
                || phoneMatches;
    }

    private static String normalizePhone(String value) {
        return value.replaceAll("[^0-9]", "");
    }

    private static boolean secureEquals(String expected, String actual) {
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
