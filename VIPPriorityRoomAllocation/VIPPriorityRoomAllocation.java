package VIPPriorityRoomAllocation;

import java.util.Scanner;
import VIPPriorityRoomAllocation.boundary.ReservationUI;

/**
 * @author Wan Yin
 */
public class VIPPriorityRoomAllocation {

    public static void startModule(Scanner input) {
        ReservationUI reservationUI = new ReservationUI(input);
        reservationUI.start();
    }
}
