package VIPPriorityRoomAllocation.boundary;

import VIPPriorityRoomAllocation.control.ReservationManager;
import VIPPriorityRoomAllocation.control.ReservationManager.AllocationResult;
import VIPPriorityRoomAllocation.dao.LoyaltyLookupDAO.LoyaltyProfile;
import VIPPriorityRoomAllocation.entity.Guest;
import VIPPriorityRoomAllocation.entity.Reservation;
import VIPPriorityRoomAllocation.entity.ReservationStatus;
import VIPPriorityRoomAllocation.entity.Room;
import LoyaltyAndRewardsService.control.LoyaltyServiceControl;
import LoyaltyAndRewardsService.dao.RequestDao;
import LoyaltyAndRewardsService.entity.RedemptionRequest;
import adt.ArrayList;
import adt.ListInterface;
import adt.SortedArrayList;
import adt.SortedListInterface;
import common.ui.MessageUI;
import common.ui.InputHelper;
import common.ui.InputHelper.EndOfInputException;
import common.ui.ConsoleStyle;
import common.ui.ConsoleProgress;
import common.ui.ConsoleAnimation;
import common.ui.Logo;
import common.utility.Validation;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Scanner;

/**
 * @author Wan Yin
 */
public class ReservationUI {

    private final ReservationManager reservationManager;
    private final Scanner scanner;

    public ReservationUI(Scanner scanner) {
        this.scanner = scanner;
        reservationManager = new ReservationManager();
    }

    public void start() {
        try {
            boolean exit = false;

            while (!exit) {
                InputHelper.clearScreen();
                displayStaffMenu();
                String choice = InputHelper.inputString(scanner, "Select an option: ").trim();

                switch (choice) {
                    case "1":
                        displayPendingPriorityReservations();
                        break;
                    case "2":
                        allocateAvailableRooms();
                        break;
                    case "3":
                        searchReservation();
                        break;
                    case "4":
                        displayReservations(reservationManager.getReservations());
                        break;
                    case "5":
                        displayReportMenu();
                        break;
                    case "0":
                        exit = true;
                        break;
                    default:
                        MessageUI.displayError("Invalid option. Please try again.");
                }
                if (!exit && !choice.equals("5")) {
                    InputHelper.pressEnterToContinue(scanner);
                }
            }
        } catch (EndOfInputException exception) {
            // EOF behaves like selecting Back.
        }
    }

    /**
     * Opens the customer-facing reservation functions for one member whose ID
     * was already verified by the member portal.
     */
    public void startMember(String memberId) {
        try {
            LoyaltyProfile profile = loadMemberProfile(memberId);
            if (profile == null || !profile.getMemberId().equalsIgnoreCase(memberId.trim())) {
                MessageUI.displayError("Member record is no longer available. Please sign in again.");
                return;
            }
            InputHelper.clearScreen();
            submitReservationRequest(profile);
        } catch (EndOfInputException exception) {
            // EOF behaves like selecting Back.
        }
    }

    /** Displays the signed-in member's reservations from Member Home. */
    public void viewMemberReservations(String memberId) {
        try {
            InputHelper.clearScreen();
            LoyaltyProfile profile = reservationManager.findLoyaltyProfile(memberId);
            if (profile == null || !profile.getMemberId().equalsIgnoreCase(memberId.trim())) {
                MessageUI.displayError("Member record is no longer available. Please sign in again.");
                return;
            }
            displayReservations(ConsoleAnimation.runWithSpinner(
                    () -> reservationManager.findReservationsByGuestId(profile.getMemberId()),
                    "Fetching member reservations"));
        } catch (EndOfInputException exception) {
            // EOF behaves like returning to Member Home.
        }
    }

    private void displayStaffMenu() {
        Logo.display();
        System.out.println(ConsoleStyle.menu(ConsoleStyle.menuBox("PRIORITY ROOM ALLOCATION",
                "1|View Priority Waiting Queue",
                "2|Allocate Rooms by Priority",
                "3|Search Reservation",
                "4|View All Reservations",
                "5|View Reports",
                "0|Back")));
    }

    private LoyaltyProfile loadMemberProfile(String memberId) {
        return ConsoleProgress.run(
                () -> reservationManager.findLoyaltyProfile(memberId),
                "Fetching member information...",
                "Checking member records...",
                "Preparing member reservation page...");
    }

    private void payForReservation(LoyaltyProfile profile) {
        System.out.println("\n--- Reservation Payment ---");
        ListInterface<Reservation> payableReservations = findPayableReservations(
                profile.getMemberId());
        if (payableReservations.isEmpty()) {
            MessageUI.displayInfo("You have no confirmed, unpaid reservations with an assigned room.");
            return;
        }

        System.out.println("Select a confirmed reservation to pay with points:");
        displayPayableReservations(payableReservations);
        int selection = promptReservationSelection(payableReservations.getNumberOfEntries());
        if (selection == 0) {
            MessageUI.displayInfo("Points payment cancelled.");
            return;
        }
        Reservation reservation = payableReservations.getEntry(selection);

        LoyaltyServiceControl loyalty = new LoyaltyServiceControl();
        double amount = calculateReservationAmount(reservation);
        int requiredPoints = loyalty.calculatePointsForPaymentAmount(amount);
        int availablePoints = loyalty.getAvailablePointsForPayment(profile.getMemberId());
        System.out.printf("Amount due: RM%.2f%n", amount);
        System.out.println("Points required: " + requiredPoints);
        System.out.println("Available points: " + availablePoints);
        if (availablePoints < requiredPoints) {
            MessageUI.displayInfo("You do not have enough points for this reservation.");
            return;
        }

        System.out.println("1. Member Points");
        String choice = InputHelper.inputString(scanner, "Select payment method: ").trim();

        if ("1".equals(choice)) {
            if (!confirmYes("Submit this points-payment request? (Y/N): ")) {
                MessageUI.displayInfo("Points payment cancelled.");
                return;
            }

            boolean submitted = ConsoleProgress.run(
                    () -> loyalty.submitPointPaymentRequest(
                            profile.getMemberId(), reservation.getConfirmationNumber(), amount),
                    "Processing points payment...",
                    "Updating payment request...",
                    "Saving payment status...");
            if (submitted) {
                MessageUI.displaySuccess(
                        "Points-payment request submitted for staff approval.");
            } else {
                MessageUI.displayError("Unable to submit points payment. Check available "
                        + "points or an existing request.");
            }
        } else {
            MessageUI.displayError("Invalid payment method.");
        }
    }

    /** Opens the signed-in member's point-payment flow directly. */
    public void startMemberPayment(String memberId) {
        try {
            LoyaltyProfile profile = loadMemberProfile(memberId);
            if (profile == null || !profile.getMemberId().equalsIgnoreCase(memberId.trim())) {
                MessageUI.displayError("Member record is no longer available. Please sign in again.");
                return;
            }
            InputHelper.clearScreen();
            payForReservation(profile);
        } catch (EndOfInputException exception) {
            // EOF behaves like returning to Member Home.
        }
    }

    private ListInterface<Reservation> findPayableReservations(String memberId) {
        ListInterface<Reservation> payableReservations = new ArrayList<>();
        ListInterface<Reservation> memberReservations =
                reservationManager.findReservationsByGuestId(memberId);
        ListInterface<RedemptionRequest> paymentRequests =
                new RequestDao().retrieveFromFile();

        for (Reservation reservation : memberReservations) {
            if (reservation.getStatus() == ReservationStatus.CONFIRMED
                    && reservation.getAssignedRoom() != null
                    && !"PAID".equalsIgnoreCase(reservation.getPaymentStatus())
                    && !hasActivePointPaymentRequest(reservation, paymentRequests)) {
                payableReservations.add(reservation);
            }
        }
        return payableReservations;
    }

    private boolean hasActivePointPaymentRequest(Reservation reservation,
            ListInterface<RedemptionRequest> paymentRequests) {
        for (RedemptionRequest request : paymentRequests) {
            boolean sameReservation = request.getConfirmationNumber()
                    .equalsIgnoreCase(reservation.getConfirmationNumber());
            boolean sameMember = request.getMemberId()
                    .equalsIgnoreCase(reservation.getGuest().getGuestId());
            boolean requestStillActive = request.getStatus() != null
                    && !request.getStatus().toLowerCase().startsWith("rejected");

            if (sameReservation && sameMember && requestStillActive) {
                return true;
            }
        }
        return false;
    }

    private void displayPayableReservations(ListInterface<Reservation> reservations) {
        String border = "+-----+------------+----------+------------+------------+--------------+";
        System.out.println(border);
        System.out.printf("| %-3s | %-10s | %-8s | %-10s | %-10s | %-12s |%n",
                "No.", "Res ID", "Room", "Check-In", "Check-Out", "Amount (RM)");
        System.out.println(border);

        for (int position = 1; position <= reservations.getNumberOfEntries(); position++) {
            Reservation reservation = reservations.getEntry(position);
            System.out.printf("| %-3d | %-10s | %-8s | %-10s | %-10s | %12.2f |%n",
                    position,
                    reservation.getConfirmationNumber(),
                    reservation.getAssignedRoom().getRoomNumber(),
                    reservation.getCheckInDate(),
                    reservation.getCheckOutDate(),
                    calculateReservationAmount(reservation));
        }
        System.out.println(border);
        System.out.println("Enter 0 to cancel.");
    }

    private int promptReservationSelection(int reservationCount) {
        while (true) {
            String input = InputHelper.inputString(scanner,
                    "Select reservation number: ").trim();
            try {
                int selection = Integer.parseInt(input);
                if (selection == 0 || (selection >= 1 && selection <= reservationCount)) {
                    return selection;
                }
            } catch (NumberFormatException exception) {
                // Re-prompt below.
            }
            MessageUI.displayError("Please select a number from 1 to "
                    + reservationCount + ", or 0 to cancel.");
        }
    }

    private Reservation findReservationByConfirmationNumber() {
        String confirmationNumber = promptRequiredText("Confirmation number: ");
        Reservation reservation = ConsoleAnimation.runWithSpinner(
                () -> reservationManager.findByConfirmationNumber(confirmationNumber),
                "Fetching reservation information");
        if (reservation == null) {
            MessageUI.displayError("Reservation not found.");
        }
        return reservation;
    }


    private void submitReservationRequest(LoyaltyProfile profile) {
        System.out.println("\n--- Submit Reservation Request ---");
        MessageUI.displaySuccess("Member identity confirmed.");
        System.out.println("Member ID        : " + profile.getMemberId());
        System.out.println("Member Name      : " + profile.getName());
        System.out.println("Phone Number     : " + profile.getPhoneNumber());
        System.out.println("Loyalty Tier     : " + profile.getLoyaltyTier());
        submitReservationRequest(new Guest(profile.getMemberId(), profile.getName(),
                profile.getPhoneNumber(), profile.getLoyaltyTier()));
    }

    private void submitReservationRequest(Guest guest) {
        LocalDate checkInDate = promptCheckInDate();
        LocalDate checkOutDate = promptCheckOutDate(checkInDate);

        System.out.println("\n--- Review Reservation Request ---");
        System.out.println("Guest ID         : " + guest.getGuestId());
        System.out.println("Guest Name       : " + guest.getFullName());
        System.out.println("Phone Number     : " + guest.getPhoneNumber());
        System.out.println("Loyalty Tier     : " + guest.getLoyaltyTier());
        System.out.println("Room Type        : " + Room.ROOM_TYPE);
        System.out.println("Check-in Date    : " + checkInDate);
        System.out.println("Check-out Date   : " + checkOutDate);
        System.out.println("Allocation       : Automatic by loyalty priority");

        if (!confirmYes("Submit this reservation request? (Y/N): ")) {
            MessageUI.displayInfo("Reservation request cancelled.");
            return;
        }

        // save as PENDING first, room will be assigned later by heap priority
        Reservation reservation = ConsoleProgress.run(
                () -> reservationManager.submitPriorityReservationRequest(
                        guest, checkInDate, checkOutDate),
                "Processing your booking...",
                "Saving reservation request...",
                "Updating priority queue...");

        MessageUI.displaySuccess("Reservation request submitted successfully.");
        System.out.println("Request Number : " + reservation.getConfirmationNumber());
        System.out.println("Status         : " + reservation.getStatus());
        System.out.println("Pending Count  : " + reservationManager.getPendingPriorityReservationCount());
    }

    // show pending reservations based on heap priority order
    private void displayPendingPriorityReservations() {
        Iterator<Reservation> iterator = ConsoleProgress.run(
                reservationManager::getPendingPriorityReservationIterator,
                "Fetching pending reservation information...",
                "Ordering requests by priority...",
                "Preparing queue results...");
        ReservationTablePrinter.printPending(iterator,
                reservationManager.getPendingPriorityReservationCount());
    }

    // staff clicks this to start automatic room allocation
    private void allocateAvailableRooms() {
        int pendingCount = reservationManager.getPendingPriorityReservationCount(); // show how many requests are pending

        if (pendingCount == 0) {
            MessageUI.displayInfo("No pending priority reservation requests.");
            return;
        }

        System.out.println("\n--- Allocate Available Rooms ---");
        System.out.println("Pending Requests : " + pendingCount);

        if (!confirmYes("Allocate available rooms by loyalty priority? (Y/N): ")) {
            MessageUI.displayInfo("Allocation cancelled.");
            return;
        }

        AllocationResult result = ConsoleProgress.run(
                reservationManager::allocateAvailableRooms,
                "Loading priority queue...",
                "Assigning available rooms...",
                "Saving allocation results...");
        MessageUI.displaySuccess("Allocation completed.");
        System.out.println("Confirmed : " + result.getConfirmedCount());
        System.out.println("Rejected  : " + result.getRejectedCount());

        displaySuccessfulRoomAllocations();
    }


    // search reservation using sequential search
    private void searchReservation() {
        System.out.println("\n--- Search Reservation ---");
        ListInterface<Reservation> matches = findReservationsByPrompt();

        if (matches.isEmpty()) {
            MessageUI.displayError("Reservation not found.");
            return;
        }

        for (int i = 1; i <= matches.getNumberOfEntries(); i++) {
            displayReservationDetails(matches.getEntry(i));
        }
    }

    private ListInterface<Reservation> findReservationsByPrompt() {
        String searchValue = promptRequiredText("Enter reservation ID / Member ID / Guest Name: ");
        // returns all matched records, not only the first one
        return ConsoleAnimation.runWithSpinner(
                () -> reservationManager.findMatchingReservations(searchValue),
                "Searching reservation records");
    }

    public void displayReservations(ListInterface<Reservation> reservations) {
        ReservationTablePrinter.printAll(reservations);
    }

    private void displaySuccessfulRoomAllocations() {
        SortedListInterface<Reservation> successfulReservations = new SortedArrayList<>(
                (left, right) -> left.compareTo(right) * -1); // SortedArrayList keeps successful guests in priority order
        Iterator<Reservation> iterator = reservationManager.getReservations().iterator();

        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            if (reservation.getStatus() == ReservationStatus.CONFIRMED
                    && reservation.getAssignedRoom() != null) {
                successfulReservations.add(reservation);
            }
        }

        ReservationTablePrinter.printSuccessfulAllocations(successfulReservations);
    }

    
    // show all reservations 
    private void displayReservationDetails(Reservation reservation) {
        ReservationTablePrinter.printDetails(reservation);
    }

    private void displayReportMenu() {
        new ReservationReportUI(scanner, reservationManager).start();
    }

    private double calculateReservationAmount(Reservation reservation) {
        Room room = reservation.getAssignedRoom();
        if (room == null) {
            return 0.00;
        }

        long nights = reservation.getCheckOutDate().toEpochDay()
                - reservation.getCheckInDate().toEpochDay();
        return nights * room.getPricePerNight();
    }

    private LocalDate promptCheckInDate() {
        while (true) {
            LocalDate checkInDate = promptDate("Check-in date (yyyy-MM-dd): ");

            if (!checkInDate.isBefore(LocalDate.now())) {
                return checkInDate;
            }

            MessageUI.displayError("Check-in date cannot be before today.");
        }
    }

    private LocalDate promptCheckOutDate(LocalDate checkInDate) {
        while (true) {
            LocalDate checkOutDate = promptDate("Check-out date (yyyy-MM-dd): ");

            if (Validation.isValidStay(checkInDate, checkOutDate)) {
                return checkOutDate;
            }

            MessageUI.displayError("Check-out date must be after check-in date.");
        }
    }

    private LocalDate promptDate(String prompt) {
        while (true) {
            String input = InputHelper.inputString(scanner, prompt).trim();

            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException ex) {
                MessageUI.displayError("Dates must use yyyy-MM-dd format.");
            }
        }
    }

    private String promptRequiredText(String prompt) {
        while (true) {
            String value = InputHelper.inputString(scanner, prompt).trim();

            if (Validation.isNonBlank(value)) {
                return value;
            }

            MessageUI.displayError("This field is required.");
        }
    }

    private boolean confirmYes(String prompt) {
        while (true) {
            String input = InputHelper.inputString(scanner, prompt).trim();

            if (input.equalsIgnoreCase("Y")) {
                return true;
            }

            if (input.equalsIgnoreCase("N")) {
                return false;
            }

            MessageUI.displayError("Please enter Y or N.");
        }
    }

    public ReservationManager getReservationManager() {
        return reservationManager;
    }

    public static void main(String[] args) {
        new ReservationUI(new Scanner(System.in)).start();
    }
}
