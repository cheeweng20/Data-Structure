package WalkInRegistrationAndReservation;

import java.util.Scanner;
import WalkInRegistrationAndReservation.boundary.ReservationUI;

/**
 * @author Wan Yin
 */
public class WalkInRegistrationAndReservation {

    public static void WalkInRegistrationAndReservationMain(Scanner input) {
        input.nextLine();
        ReservationUI reservationUI = new ReservationUI(input);
        reservationUI.start();
    }
}
