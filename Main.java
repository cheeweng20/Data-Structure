import java.util.Scanner;

import LoyaltyAndRewardsService.LoyaltyAndRewardsService;
import common.src.Logo;

public class Main {
    private static void display() {
        Logo.displayMain();

        System.out.println("\r\n" + //
                ".------.----------------------------------------------------.\r\n" + //
                "| No.  |                      Function                      |\r\n" + //
                ":------+----------------------------------------------------:\r\n" + //
                "|   1. | Walk-In Registrations & Standard Booking Procedure |\r\n" + //
                ":------+----------------------------------------------------:\r\n" + //
                "|   2. | Housekeeping and Task Log                          |\r\n" + //
                ":------+----------------------------------------------------:\r\n" + //
                "|   3. | Front Desk Service                                 |\r\n" + //
                ":------+----------------------------------------------------:\r\n" + //
                "|   4. | Loyalty and Rewards service                        |\r\n" + //
                "'------'----------------------------------------------------'\r\n" + //
                "\r\n" + //
                "");

        System.out.print("Enter a number to select function(0 to exit): ");
    }

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        boolean exit = false;

        // 把你们的main function换成static的然后直接放在这里就可以了
        while (!exit) {
            display();
            int userSelection = input.nextInt();

            switch (userSelection) {
                case 1:
                    System.out.println("Comming Soon");
                    break;
                case 2:
                    System.out.println("Comming Soon");
                    break;
                case 3:
                    System.out.println("Comming Soon");
                    break;
                case 4:
                    LoyaltyAndRewardsService.LoyaltyAndRewardsServiceMain(input);
                    break;
                case 0:
                    System.out.println("Thank You for your using");
                    exit = true;
                    break;
                default:
                    break;
            }
        }

    }
}
