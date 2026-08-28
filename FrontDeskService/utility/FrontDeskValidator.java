package FrontDeskService.utility;

/** Input checks used by the Front Desk boundary class.
 * @author Yi Ren
 */
public final class FrontDeskValidator {
    private FrontDeskValidator() { }

    public static boolean isConfirmationNumber(String value) {
        return value != null && value.matches("\\d{8}");
    }
}
