package LoyaltyAndRewardsService;

import java.util.Scanner;

import LoyaltyAndRewardsService.boundary.LoyaltyUI;

/**
 * @author Chee Weng
 */
public class LoyaltyAndRewardsService {

    public static void startModule(Scanner input) {
        LoyaltyUI loyaltyUI = new LoyaltyUI(input);
        loyaltyUI.start();
    }
}
