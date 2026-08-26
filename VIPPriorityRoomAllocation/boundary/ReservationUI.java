package VIPPriorityRoomAllocation.boundary;

import VIPPriorityRoomAllocation.control.ReservationManager;
import VIPPriorityRoomAllocation.control.ReservationManager.AllocationResult;
import VIPPriorityRoomAllocation.dao.LoyaltyLookupDAO.LoyaltyProfile;
import VIPPriorityRoomAllocation.entity.Guest;
import VIPPriorityRoomAllocation.entity.LoyaltyTier;
import VIPPriorityRoomAllocation.entity.Reservation;
import VIPPriorityRoomAllocation.entity.ReservationStatus;
import VIPPriorityRoomAllocation.entity.Room;
import VIPPriorityRoomAllocation.utility.InputValidator;
import VIPPriorityRoomAllocation.utility.MessageUI;
import VIPPriorityRoomAllocation.utility.ReportPdfExporter;
import VIPPriorityRoomAllocation.utility.ReportPdfExporter.ChartType;
import adt.ArrayList;
import adt.ListInterface;
import adt.SortedArrayList;
import adt.SortedListInterface;
import common.src.InputHelper;
import common.src.ConsoleStyle;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Scanner;

/**
 * @author Wan Yin
 */
public class ReservationUI {

    private final ReservationManager reservationManager;
    private final Scanner scanner;

    public ReservationUI() {
        this(new Scanner(System.in));
    }

    public ReservationUI(Scanner scanner) {
        this.scanner = scanner;
        reservationManager = new ReservationManager();
    }

    public void start() {
        boolean exit = false;

        while (!exit) {
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
        }
    }

    /**
     * Opens the customer-facing reservation functions for one member whose ID
     * was already verified by the member portal.
     */
    public void startMember(String memberId) {
        LoyaltyProfile profile = reservationManager.findLoyaltyProfile(memberId);
        if (profile == null || !profile.getMemberId().equalsIgnoreCase(memberId.trim())) {
            MessageUI.displayError("Member record is no longer available. Please sign in again.");
            return;
        }

        boolean exit = false;
        while (!exit) {
            displayMemberMenu(profile);
            String choice = InputHelper.inputString(scanner, "Select an option: ").trim();
            switch (choice) {
                case "1":
                    submitReservationRequest(profile);
                    break;
                case "2":
                    displayReservations(
                            reservationManager.findReservationsByGuestId(profile.getMemberId()));
                    break;
                case "0":
                    exit = true;
                    break;
                default:
                    MessageUI.displayError("Invalid option. Please try again.");
            }
        }
    }

    private void displayStaffMenu() {
        System.out.println(ConsoleStyle.title("\n--- Staff: Priority Room Allocation ---") + "\n"
                + ".-----.----------------------------------------.\n"
                + "| No. |                Function                |\n"
                + ":-----+----------------------------------------:\n"
                + "| 1.  | View Priority Waiting Queue            |\n"
                + ":-----+----------------------------------------:\n"
                + "| 2.  | Allocate Rooms by Priority             |\n"
                + ":-----+----------------------------------------:\n"
                + "| 3.  | Search Reservation                     |\n"
                + ":-----+----------------------------------------:\n"
                + "| 4.  | View All Reservations                  |\n"
                + ":-----+----------------------------------------:\n"
                + "| 5.  | View Reports                           |\n"
                + ":-----+----------------------------------------:\n"
                + "| 0.  | Back                                   |\n"
                + "'-----'----------------------------------------'");
    }

    private void displayMemberMenu(LoyaltyProfile profile) {
        System.out.println(ConsoleStyle.title("\n--- Member Reservations ---"));
        System.out.println("Signed in as " + profile.getName() + " ("
                + profile.getMemberId() + ", " + profile.getLoyaltyTier() + ")");
        System.out.println(ConsoleStyle.menu("1. Submit Reservation Request\n"
                + "2. View My Reservations\n0. Back"));
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

        Reservation reservation = reservationManager.submitPriorityReservationRequest(
                guest, checkInDate, checkOutDate);

        MessageUI.displaySuccess("Reservation request submitted successfully.");
        System.out.println("Request Number : " + reservation.getConfirmationNumber());
        System.out.println("Status         : " + reservation.getStatus());
        System.out.println("Pending Count  : " + reservationManager.getPendingPriorityReservationCount());
    }

    //put the reservation inside the  waiting queue
    private void displayPendingPriorityReservations() {
        Iterator<Reservation> iterator = reservationManager.getPendingPriorityReservationIterator();

        if (!iterator.hasNext()) {
            MessageUI.displayInfo("No pending priority reservation requests.");
            return;
        }

        System.out.println("\n--- Pending Priority Reservations ---");
        String border = "+----------+--------------+----------------------+-----------+------------+------------+";
        System.out.println(border);
        System.out.printf("| %-8s | %-12s | %-20s | %-9s | %-10s | %-10s |%n",
                "Priority", "Request No.", "Guest Name", "Tier", "Check-In", "Status");
        System.out.println(border);

        int position = 1;
        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            System.out.printf("| %-8d | %-12s | %-20.20s | %-9s | %-10s | %-10s |%n",
                    position++,
                    reservation.getConfirmationNumber(),
                    reservation.getGuest().getFullName(),
                    reservation.getGuest().getLoyaltyTier(),
                    reservation.getCheckInDate(),
                    reservation.getStatus());
        }

        System.out.println(border);
        System.out.println("Total pending requests: "
                + reservationManager.getPendingPriorityReservationCount());
    }

    //allocate room to guest based on priority level
    private void allocateAvailableRooms() {
        int pendingCount = reservationManager.getPendingPriorityReservationCount(); //show how many prequest pending

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

        AllocationResult result = reservationManager.allocateAvailableRooms();
        MessageUI.displaySuccess("Allocation completed.");
        System.out.println("Confirmed : " + result.getConfirmedCount());
        System.out.println("Rejected  : " + result.getRejectedCount());

        displaySuccessfulRoomAllocations();
    }


    //search reservation 
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
        return reservationManager.findMatchingReservations(searchValue);
    }

    public void displayReservations(ListInterface<Reservation> reservations) {
        if (reservations.isEmpty()) {
            MessageUI.displayInfo("No reservation record found.");
            return;
        }

        String border = "+-----+------------+--------------------+-----------+----------+------------+------------+-------------+";
        System.out.println("\n--- All Reservations ---");
        System.out.println(border);
        System.out.printf("| %-3s | %-10s | %-18s | %-9s | %-8s | %-10s | %-10s | %-11s |%n",
                "No.", "Res ID", "Guest Name", "Tier", "Room", "Check-In", "Payment", "Status");
        System.out.println(border);

        Iterator<Reservation> iterator = reservations.iterator();
        int number = 1;

        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            Room room = reservation.getAssignedRoom();
            String roomNumber = room == null ? "-" : room.getRoomNumber();

            System.out.printf("| %-3d | %-10s | %-18.18s | %-9s | %-8s | %-10s | %-10s | %-11s |%n",
                    number++,
                    reservation.getConfirmationNumber(),
                    reservation.getGuest().getFullName(),
                    reservation.getGuest().getLoyaltyTier(),
                    roomNumber,
                    reservation.getCheckInDate(),
                    reservation.getPaymentStatus(),
                    reservation.getStatus());
        }

        System.out.println(border);
        System.out.println("Total reservations: " + reservations.getNumberOfEntries());
    }

    private void displaySuccessfulRoomAllocations() {
        SortedListInterface<Reservation> successfulReservations = new SortedArrayList<>(
                (left, right) -> left.compareTo(right) * -1); // sorted list shows successful guests by highest loyalty priority first
        Iterator<Reservation> iterator = reservationManager.getReservations().iterator();

        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            if (reservation.getStatus() == ReservationStatus.CONFIRMED
                    && reservation.getAssignedRoom() != null) {
                successfulReservations.add(reservation);
            }
        }

        if (successfulReservations.isEmpty()) {
            MessageUI.displayInfo("No successful room allocation found.");
            return;
        }

        String border = "+-----+------------+--------------------+-----------+----------+------------+-------------+";
        System.out.println("\n--- Successful Room Allocations ---");
        System.out.println(border);
        System.out.printf("| %-3s | %-10s | %-18s | %-9s | %-8s | %-10s | %-11s |%n",
                "No.", "Res ID", "Guest Name", "Tier", "Room", "Check-In", "Status");
        System.out.println(border);

        for (int i = 1; i <= successfulReservations.getNumberOfEntries(); i++) {
            Reservation reservation = successfulReservations.getEntry(i);
            System.out.printf("| %-3d | %-10s | %-18.18s | %-9s | %-8s | %-10s | %-11s |%n",
                    i,
                    reservation.getConfirmationNumber(),
                    reservation.getGuest().getFullName(),
                    reservation.getGuest().getLoyaltyTier(),
                    reservation.getAssignedRoom().getRoomNumber(),
                    reservation.getCheckInDate(),
                    reservation.getStatus());
        }

        System.out.println(border);
        System.out.println("Total successful allocations: "
                + successfulReservations.getNumberOfEntries());
    }

    
    // show all reservations 
    private void displayReservationDetails(Reservation reservation) {
        Guest guest = reservation.getGuest();
        Room room = reservation.getAssignedRoom();
        long numberOfNights = reservation.getCheckOutDate().toEpochDay()
                - reservation.getCheckInDate().toEpochDay();
        double totalPrice = room == null ? 0.00 : numberOfNights * room.getPricePerNight();

        System.out.println("\n--- Reservation Details ---");
        System.out.println("Reservation ID   : " + reservation.getConfirmationNumber());
        System.out.println("Guest ID         : " + guest.getGuestId());
        System.out.println("Guest Name       : " + guest.getFullName());
        System.out.println("Phone Number     : " + guest.getPhoneNumber());
        System.out.println("Loyalty Tier     : " + guest.getLoyaltyTier());
        System.out.println("Room Type        : " + Room.ROOM_TYPE);
        System.out.println("Room / Unit No.  : " + (room == null ? "Not assigned" : room.getRoomNumber()));
        System.out.println("Check-in Date    : " + reservation.getCheckInDate());
        System.out.println("Check-out Date   : " + reservation.getCheckOutDate());
        System.out.println("Number of Nights : " + numberOfNights);

        if (room != null) {
            System.out.printf("Price per Night  : RM%.2f%n", room.getPricePerNight());
            System.out.printf("Total Price      : RM%.2f%n", totalPrice);
        }

        System.out.println("Payment Method   : " + reservation.getPaymentMethod());
        System.out.println("Payment Status   : " + reservation.getPaymentStatus());
        System.out.println("Status           : " + reservation.getStatus());
    }

    //MENU NEED TO MAKE THE CHANGES 

    private void displayReportMenu() {
        boolean back = false;

        while (!back) {
            System.out.println("\n--- Reservation Reports ---\n"
                    + "1. Monthly Reservation Summary\n"
                    + "2. Monthly Room Allocation Report\n"
                    + "0. Back");
            String choice = InputHelper.inputString(scanner, "Select an option: ").trim();

            switch (choice) {
                case "1":
                    displayMonthlyReservationSummary();
                    break;
                case "2":
                    displayMonthlyRoomAllocationReport();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    MessageUI.displayError("Invalid option. Please try again.");
            }
        }
    }

    private void displayMonthlyReservationSummary() {
        YearMonth reportMonth = promptReportMonth();
        String report = buildMonthlyReservationSummary(reportMonth);

        if (displayReport(report, "No reservation records found for " + reportMonth + ".")) {
            offerPdfExport("Monthly Reservation Summary", report, ChartType.RESERVATION_STATUS);
        }
    }

    private void displayMonthlyRoomAllocationReport() {
        YearMonth reportMonth = promptReportMonth();
        String report = buildMonthlyRoomAllocationReport(reportMonth);

        if (displayReport(report, "No allocated room records found for " + reportMonth + ".")) {
            offerPdfExport("Monthly Room Allocation Report", report, ChartType.TIER_ALLOCATION);
        }
    }

    private String buildMonthlyReservationSummary(YearMonth reportMonth) {
        SortedListInterface<Reservation> reportReservations = new SortedArrayList<>(
                (left, right) -> left.getBookingDateTime().compareTo(right.getBookingDateTime())); // sorted list arranges report by booking time
        Iterator<Reservation> iterator = reservationManager.getReservations().iterator();
        int[] statusCounts = new int[ReservationStatus.values().length];
        double totalRevenue = 0.00;

        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            if (!YearMonth.from(reservation.getCheckInDate()).equals(reportMonth)) {
                continue;
            }

            reportReservations.add(reservation);
            statusCounts[reservation.getStatus().ordinal()]++;
            if (reservation.getAssignedRoom() != null
                    && reservation.getStatus() != ReservationStatus.REJECTED) {
                totalRevenue += calculateReservationAmount(reservation);
            }
        }

        if (reportReservations.isEmpty()) {
            return "";
        }

        StringBuilder report = new StringBuilder();
        report.append("=== Monthly Reservation Summary: ").append(reportMonth).append(" ===\n");
        report.append("Total Reservations : ").append(reportReservations.getNumberOfEntries()).append('\n');
        report.append("Pending            : ")
                .append(statusCounts[ReservationStatus.PENDING.ordinal()])
                .append('\n');
        report.append("Allocated Rooms    : ")
                .append(statusCounts[ReservationStatus.CONFIRMED.ordinal()]
                        + statusCounts[ReservationStatus.CHECKED_IN.ordinal()]
                        + statusCounts[ReservationStatus.CHECKED_OUT.ordinal()])
                .append('\n');
        report.append("Checked-Out        : ")
                .append(statusCounts[ReservationStatus.CHECKED_OUT.ordinal()])
                .append('\n');
        report.append("Rejected           : ")
                .append(statusCounts[ReservationStatus.REJECTED.ordinal()]).append('\n');
        report.append(String.format("Total Revenue      : RM%.2f%n%n", totalRevenue));
        appendMonthlyReportHeader(report);

        for (int i = 1; i <= reportReservations.getNumberOfEntries(); i++) {
            appendMonthlyReportLine(report, reportReservations.getEntry(i));
        }
        appendMonthlyReportBorder(report);
        appendStatusChartData(report, statusCounts);
        return report.toString();
    }

    private String buildMonthlyRoomAllocationReport(YearMonth reportMonth) {
        SortedListInterface<Reservation> reportReservations = new SortedArrayList<>(
                (left, right) -> left.getAssignedRoom().getRoomNumber()
                        .compareToIgnoreCase(right.getAssignedRoom().getRoomNumber())); // sorted list arranges allocated rooms by room number
        Iterator<Reservation> iterator = reservationManager.getReservations().iterator();
        int[] tierCounts = new int[LoyaltyTier.values().length];

        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();

            if (YearMonth.from(reservation.getCheckInDate()).equals(reportMonth)
                    && reservation.getAssignedRoom() != null
                    && reservation.getStatus() != ReservationStatus.REJECTED) {
                reportReservations.add(reservation);
                tierCounts[reservation.getGuest().getLoyaltyTier().ordinal()]++;
            }
        }

        if (reportReservations.isEmpty()) {
            return "";
        }

        StringBuilder report = new StringBuilder();
        report.append("=== Monthly Room Allocation Report: ").append(reportMonth).append(" ===\n");
        report.append("Total Allocated Reservations : ")
                .append(reportReservations.getNumberOfEntries()).append('\n');
        report.append("Room Type                    : ").append(Room.ROOM_TYPE).append("\n\n");
        appendAllocationReportHeader(report);

        for (int i = 1; i <= reportReservations.getNumberOfEntries(); i++) {
            appendAllocationReportLine(report, reportReservations.getEntry(i));
        }
        appendAllocationReportBorder(report);
        appendTierChartData(report, tierCounts);
        return report.toString();
    }

    private boolean displayReport(String report, String emptyMessage) {
        if (report.isEmpty()) {
            MessageUI.displayInfo(emptyMessage);
            return false;
        }

        System.out.println(removeChartData(report));
        return true;
    }

    private void offerPdfExport(String title, String report, ChartType chartType) {
        String selection = InputHelper.inputString(
                scanner, "Generate chart PDF and open it? (Y/N): ");
        if (!selection.equalsIgnoreCase("Y") && !selection.equalsIgnoreCase("Yes")) {
            return;
        }

        Path pdfPath;
        try {
            pdfPath = ReportPdfExporter.export(title, report, chartType);
        } catch (IOException ex) {
            MessageUI.displayError("Unable to generate PDF: " + ex.getMessage());
            return;
        }

        MessageUI.displaySuccess("PDF generated: " + pdfPath);
        try {
            if (!ReportPdfExporter.open(pdfPath)) {
                MessageUI.displayInfo("Open the PDF manually from the path shown above.");
            }
        } catch (IOException ex) {
            MessageUI.displayInfo("The PDF was generated but could not be opened automatically: "
                    + ex.getMessage());
        }
    }

    private String removeChartData(String report) {
        int chartDataIndex = report.indexOf("=== Reservation Status Chart Data ===");
        if (chartDataIndex < 0) {
            chartDataIndex = report.indexOf("=== Loyalty Tier Allocation Chart Data ===");
        }
        return chartDataIndex < 0 ? report : report.substring(0, chartDataIndex).trim();
    }

    private YearMonth promptReportMonth() {
        while (true) {
            String input = InputHelper.inputString(scanner, "Enter report month (yyyy-MM): ").trim();
            try {
                return YearMonth.parse(input);
            } catch (DateTimeParseException ex) {
                MessageUI.displayError("Invalid month. Please use yyyy-MM format.");
            }
        }
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

    private void appendMonthlyReportHeader(StringBuilder report) {
        appendMonthlyReportBorder(report);
        report.append(String.format("| %-10s | %-18s | %-9s | %-16s | %-10s | %-7s | %-12s |%n",
                "Res ID", "Guest Name", "Tier", "Booking Time", "Check-In", "Room", "Amount"));
        appendMonthlyReportBorder(report);
    }

    private void appendMonthlyReportLine(StringBuilder report, Reservation reservation) {
        Room room = reservation.getAssignedRoom();
        String roomNumber = room == null ? "-" : room.getRoomNumber();
        String amount = room == null ? "-" : String.format("RM%.2f", calculateReservationAmount(reservation));
        String bookingTime = reservation.getBookingDateTime()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        report.append(String.format("| %-10s | %-18.18s | %-9s | %-16s | %-10s | %-7s | %-12s |%n",
                reservation.getConfirmationNumber(),
                reservation.getGuest().getFullName(),
                reservation.getGuest().getLoyaltyTier(),
                bookingTime,
                reservation.getCheckInDate(),
                roomNumber,
                amount));
    }

    private void appendMonthlyReportBorder(StringBuilder report) {
        report.append("+------------+--------------------+-----------+------------------+------------+---------+--------------+\n");
    }

    private void appendAllocationReportHeader(StringBuilder report) {
        appendAllocationReportBorder(report);
        report.append(String.format("| %-12s | %-18s | %-9s | %-12s | %-12s | %-12s |%n",
                "Res ID", "Guest Name", "Tier", "Room No.", "Check-In", "Status"));
        appendAllocationReportBorder(report);
    }

    private void appendAllocationReportLine(StringBuilder report, Reservation reservation) {
        Room room = reservation.getAssignedRoom();
        String roomNumber = room == null ? "-" : room.getRoomNumber();

        report.append(String.format("| %-12s | %-18.18s | %-9s | %-12s | %-12s | %-12s |%n",
                reservation.getConfirmationNumber(),
                reservation.getGuest().getFullName(),
                reservation.getGuest().getLoyaltyTier(),
                roomNumber,
                reservation.getCheckInDate(),
                reservation.getStatus()));
    }

    private void appendAllocationReportBorder(StringBuilder report) {
        report.append("+--------------+--------------------+-----------+--------------+--------------+--------------+\n");
    }

    private void appendStatusChartData(StringBuilder report, int[] statusCounts) {
        report.append("\n=== Reservation Status Chart Data ===\n");
        report.append(String.format("| %-14s | %-5s |%n", "Label", "Value"));
        for (ReservationStatus status : ReservationStatus.values()) {
            report.append(String.format("| %-14s | %-5d |%n",
                    status, statusCounts[status.ordinal()]));
        }
    }

    private void appendTierChartData(StringBuilder report, int[] tierCounts) {
        report.append("\n=== Loyalty Tier Allocation Chart Data ===\n");
        report.append(String.format("| %-14s | %-5s |%n", "Label", "Value"));
        for (LoyaltyTier tier : LoyaltyTier.values()) {
            report.append(String.format("| %-14s | %-5d |%n",
                    tier, tierCounts[tier.ordinal()]));
        }
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

            if (InputValidator.isValidStay(checkInDate, checkOutDate)) {
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

            if (InputValidator.isNonBlank(value)) {
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
        new ReservationUI().start();
    }
}
