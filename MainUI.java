import java.util.Scanner;

import FrontDeskService.boundary.FrontDeskUI;
import HousekeepingAndTaskLog.boundary.HousekeepingUI;
import LoyaltyAndRewardsService.boundary.LoyaltyUI;
import VIPPriorityRoomAllocation.boundary.ReservationUI;
import common.ui.ConsoleAnimation;
import common.ui.ConsoleStyle;
import common.ui.InputHelper;
import common.ui.InputHelper.EndOfInputException;
import common.ui.Logo;
import common.ui.MessageUI;

/** Routes users to functional hotel modules. */
public final class MainUI {
    private final Scanner scanner;

    public MainUI(Scanner scanner) {
        this.scanner = scanner;
    }

    public void start() {
        try {
            ConsoleAnimation.startup();
            boolean exit = false;
            while (!exit) {
                InputHelper.clearScreen();
                displayMainMenu();
                String choice = InputHelper.inputString(scanner, "Select an option: ").trim();

                switch (choice) {
                    case "1":
                        new ReservationUI(scanner).start();
                        break;
                    case "2":
                        new LoyaltyUI(scanner).start();
                        break;
                    case "3":
                        new FrontDeskUI(scanner).start();
                        break;
                    case "4":
                        new HousekeepingUI(scanner).start();
                        break;
                    case "0":
                        exit = true;
                        break;
                    default:
                        MessageUI.displayError("Invalid option. Please try again.");
                        InputHelper.pressEnterToContinue(scanner);
                        break;
                }
            }
        } catch (EndOfInputException exception) {
            // EOF behaves like selecting Exit.
        }
        System.out.println(ConsoleStyle.success("Thank you for using TARUMT Resort."));
    }

    private void displayMainMenu() {
        Logo.display();
        System.out.println(ConsoleStyle.menu(ConsoleStyle.menuBox("HOTEL MANAGEMENT SYSTEM",
                "1|VIP Priority Room Allocation",
                "2|Loyalty & Rewards",
                "3|Front Desk Service",
                "4|Housekeeping & Task Log",
                "0|Exit")));
    }
}
