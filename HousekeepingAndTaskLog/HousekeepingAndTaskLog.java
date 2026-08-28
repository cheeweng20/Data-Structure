package HousekeepingAndTaskLog;

import java.util.Scanner;

import HousekeepingAndTaskLog.boundary.HousekeepingUI;

/**
 * @author Zhe Sheng
 */

public class HousekeepingAndTaskLog {

    public static void HousekeepingAndTaskLogMain(Scanner input) {
        input.nextLine();
        HousekeepingUI housekeepingUI = new HousekeepingUI(input);
        housekeepingUI.start();
    }
}
