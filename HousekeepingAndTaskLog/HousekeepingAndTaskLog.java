package HousekeepingAndTaskLog;

import java.util.Scanner;

import HousekeepingAndTaskLog.boundary.HousekeepingUI;

public class HousekeepingAndTaskLog {

    public static void HousekeepingAndTaskLogMain(Scanner input) {
        HousekeepingUI housekeepingUI = new HousekeepingUI(input);
        housekeepingUI.start();
    }
}
