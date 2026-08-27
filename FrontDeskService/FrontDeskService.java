package FrontDeskService;

import FrontDeskService.boundary.FrontDeskUI;
import java.util.Scanner;

/** Entry point for the Front Desk Service module.
 * @author Yi Ren
 */
public class FrontDeskService {
    public static void FrontDeskServiceMain(Scanner input) {
        new FrontDeskUI(input).start();
    }
}
