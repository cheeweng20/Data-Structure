package LoyaltyAndRewardsService.utility;

/**
 * Provides reusable validation for loyalty and rewards input.
 *
 * @author Chee Weng
 */
public final class Verification {
    private Verification() {
    }

    public static boolean isValidMemberName(String memberName) {
        return memberName != null
                && memberName.trim().matches("^(?=.*[A-Za-z])[A-Za-z .'-]{3,20}$");
    }

    public static boolean isValidPassport(String passport) {
        return passport != null && passport.trim().matches("^[A-Za-z0-9]{5,20}$");
    }

    public static boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && phoneNumber.trim().matches("^[0-9+ -]{7,20}$");
    }
}
