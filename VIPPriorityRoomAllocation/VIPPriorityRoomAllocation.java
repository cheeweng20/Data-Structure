package VIPPriorityRoomAllocation;

import java.util.Scanner;
import VIPPriorityRoomAllocation.boundary.ReservationUI;

/**
 * @author Wan Yin
 */
public class VIPPriorityRoomAllocation {

    public static void VIPPriorityRoomAllocationMain(Scanner input) {
        input.nextLine();
        ReservationUI reservationUI = new ReservationUI(input);
        reservationUI.start();
    }
}
