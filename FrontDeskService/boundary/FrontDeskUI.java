package FrontDeskService.boundary;

import FrontDeskService.control.FrontDeskControl;
import FrontDeskService.reporting.ReportPdfExporter;
import FrontDeskService.reporting.ReportPdfExporter.ChartType;
import LoyaltyAndRewardsService.entity.PromotionOffer;
import FrontDeskService.control.FrontDeskControl.CheckInResult;
import FrontDeskService.control.FrontDeskControl.CheckOutResult;
import FrontDeskService.control.FrontDeskControl.LateCheckoutResult;
import FrontDeskService.control.FrontDeskControl.LateCheckoutStatus;
import FrontDeskService.entity.LateCheckoutExtension;
import VIPPriorityRoomAllocation.entity.Reservation;
import VIPPriorityRoomAllocation.entity.ReservationStatus;
import VIPPriorityRoomAllocation.entity.Room;
import adt.ListInterface;
import common.ui.ConsoleStyle;
import common.ui.ConsoleProgress;
import common.ui.ConsoleAnimation;
import common.ui.InputHelper;
import common.ui.InputHelper.EndOfInputException;
import common.ui.Logo;
import common.ui.MessageUI;
import common.utility.Validation;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Scanner;

/** Console boundary for Front Desk guest services.
 * @author Yi Ren
 */
public class FrontDeskUI {
    private static final String REPORT_BORDER =
            "+------------+----------------------+--------------+------------------------+--------------+";

    private final Scanner scanner;
    private final FrontDeskControl control;

    public FrontDeskUI(Scanner scanner) {
        this.scanner = scanner;
        control = new FrontDeskControl();
    }

    public void start() {
        try {
            boolean exit = false;
            while (!exit) {
                InputHelper.clearScreen();
                displayMenu();
                String choice = InputHelper.inputString(scanner, "Select an option: ").trim();
                switch (choice) {
                    case "1":
                        checkInGuest();
                        break;
                    case "2":
                        checkOutGuest();
                        break;
                    case "3":
                        viewBilling();
                        break;
                    case "4":
                        outstandingBalanceReport();
                        break;
                    case "5":
                        paymentMethodReport();
                        break;
                    case "0":
                        exit = true;
                        break;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
                if (!exit) {
                    InputHelper.pressEnterToContinue(scanner);
                }
            }
        } catch (EndOfInputException exception) {
            // EOF behaves like selecting Back.
        }
    }

    private void displayMenu() {
        Logo.display();
        System.out.println(ConsoleStyle.menu(ConsoleStyle.menuBox("FRONT DESK SERVICE",
                "section|GUEST STAY OPERATIONS",
                "1|Check-In Guest",
                "2|Check-Out Guest",
                "section|BILLING AND REPORTING",
                "3|View Billing Details",
                "4|Outstanding Balance Report",
                "5|Payment Method Report",
                "0|Back")));
    }

    private void checkInGuest() {
        System.out.println("\n--- Guest Check-In ---");
        Reservation reservation = selectReservationBySearch();
        if (reservation == null) {
            return;
        }

        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            System.out.println("Only confirmed reservations can be checked in.");
            System.out.println("Current status: " + reservation.getStatus());
            return;
        }
        if (reservation.getCheckInDate().isAfter(LocalDate.now())) {
            System.out.println("Check-in date is " + reservation.getCheckInDate()
                    + ". Guest cannot check in yet.");
            return;
        }

        displayReservationDetails(reservation);
        if ("POINTS_PENDING".equalsIgnoreCase(reservation.getPaymentStatus())) {
            System.out.println("Check-in blocked: points payment is awaiting Loyalty approval.");
            return;
        }

        String paymentMethod = reservation.getPaymentMethod();
        if ("PAID".equalsIgnoreCase(reservation.getPaymentStatus())) {
            System.out.println("Payment has already been completed using "
                    + paymentMethodLabel(reservation) + ".");
        } else {
            paymentMethod = promptPaymentMethod();
            if (!confirmYes("Confirm payment? (Y/N): ")) {
                System.out.println("Payment cancelled. Check-in not completed.");
                return;
            }
        }

        if (!confirmYes("Confirm guest check-in? (Y/N): ")) {
            System.out.println("Check-in cancelled.");
            return;
        }

        final String selectedPaymentMethod = paymentMethod;
        CheckInResult result = ConsoleProgress.run(
                () -> control.checkInReservation(
                        reservation.getConfirmationNumber(), selectedPaymentMethod),
                "Verifying reservation details...",
                "Processing payment and check-in...",
                "Updating room status...");
        if (result == CheckInResult.SUCCESS) {
            ConsoleAnimation.success("Check-in successful.");
            displayReservationDetails(
                    control.findByConfirmationNumber(reservation.getConfirmationNumber()));
        } else {
            displayCheckInFailure(result);
        }
    }

    private void checkOutGuest() {
        System.out.println("\n--- Guest Check-Out ---");
        Reservation reservation = selectReservationBySearch();
        if (reservation == null) {
            return;
        }

        if (reservation.getStatus() != ReservationStatus.CHECKED_IN) {
            System.out.println("Only checked-in reservations can be checked out.");
            System.out.println("Current status: " + reservation.getStatus());
            return;
        }

        displayReservationDetails(reservation);
        System.out.println(ConsoleStyle.menu(ConsoleStyle.menuBox("GUEST CHECK-OUT",
                "1|Complete Guest Check-Out",
                "2|Extend Check-Out Time (Notify Housekeeping)",
                "0|Cancel")));
        System.out.print(ConsoleStyle.prompt("Select an option: "));

        switch (scanner.nextLine().trim()) {
            case "1":
                completeGuestCheckOut(reservation);
                break;
            case "2":
                extendGuestCheckOut(reservation);
                break;
            case "0":
                System.out.println("Check-out action cancelled.");
                break;
            default:
                System.out.println("Invalid option. Please select 0, 1, or 2.");
        }
    }

    private void completeGuestCheckOut(Reservation reservation) {
        if (!confirmYes("Confirm guest check-out? (Y/N): ")) {
            System.out.println("Check-out cancelled.");
            return;
        }

        CheckOutResult result = ConsoleProgress.run(
                () -> control.checkOutReservation(reservation.getConfirmationNumber()),
                "Verifying guest stay...",
                "Processing check-out...",
                "Updating room status...");
        if (result == CheckOutResult.SUCCESS) {
            ConsoleAnimation.success("Guest checked out successfully.");
            if (control.getLastAwardedPoints() > 0) {
                PromotionOffer offer = control.getLastAppliedPromotionOffer();
                String earnedPoints = String.valueOf(control.getLastAwardedPoints());
                if (offer != null) {
                    int originalPoints = (int) Math.floor(control.calculateBill(reservation));
                    earnedPoints += " (" + originalPoints + " * "
                            + offer.getPointMultiplier() + ")";
                }
                System.out.println("Points Earned   : " + earnedPoints);
            } else if (control.didLastLoyaltyAwardFail()) {
                MessageUI.displayError("Check-out was saved, but loyalty points could not "
                        + "be awarded. Staff should retry or review the loyalty files.");
            }
            displayReservationDetails(
                    control.findByConfirmationNumber(reservation.getConfirmationNumber()));
        } else {
            ConsoleAnimation.error("Check-out failed: " + getCheckOutFailureMessage(result));
        }
    }

    /**
     * This action lives inside Front Desk's Check-Out screen. It keeps the
     * guest checked in and removes any task created for today's checkout.
     */
    private void extendGuestCheckOut(Reservation reservation) {
        System.out.println("\n--- Extend Check-Out Time ---");
        LocalDateTime extendedCheckOutAt = promptFutureDateTime(
                "Extended check-out time (yyyy-MM-dd HH:mm): ");
        String reason = promptRequiredText("Reason for extension: ");

        System.out.println("New temporary check-out time: " + extendedCheckOutAt);
        if (!confirmYes("Save this extension? (Y/N): ")) {
            System.out.println("Extension cancelled.");
            return;
        }

        LateCheckoutResult result = control.extendCheckOut(
                reservation.getConfirmationNumber(), extendedCheckOutAt, reason);
        if (result.isSuccessful()) {
            System.out.println("Check-out time extended.");
            displayReservationDetails(
                    control.findByConfirmationNumber(reservation.getConfirmationNumber()));
        } else {
            System.out.println("Unable to extend check-out: "
                    + getLateCheckoutFailureMessage(result.getStatus()));
        }
    }

    private void viewBilling() {
        Reservation reservation = findReservationByConfirmationNumber();
        if (reservation == null) {
            return;
        }

        displayReservationDetails(reservation);
        if (reservation.getAssignedRoom() == null) {
            System.out.println("Billing is unavailable until a room has been assigned.");
            return;
        }
        System.out.printf("Total stay charge: RM %.2f%n", control.calculateBill(reservation));
        System.out.println("Payment method   : " + reservation.getPaymentMethod());
        System.out.println("Payment status   : " + reservation.getPaymentStatus());
    }

    private void outstandingBalanceReport() {
        ListInterface<Reservation> reservations = ConsoleProgress.run(
                control::getOutstandingBalanceReport,
                "Fetching billing information...",
                "Calculating outstanding balances...",
                "Preparing report...");
        if (reservations.isEmpty()) {
            System.out.println("\nNo outstanding balance records found.");
            return;
        }

        String report = buildOutstandingBalanceReport(reservations);
        System.out.println(report);
        offerPdfExport("Outstanding Balance Report", report,
                ChartType.OUTSTANDING_BALANCE);
    }

    private void paymentMethodReport() {
        ListInterface<Reservation> reservations = ConsoleProgress.run(
                control::getPaymentMethodReport,
                "Fetching payment information...",
                "Grouping paid reservations...",
                "Preparing report...");
        if (reservations.isEmpty()) {
            System.out.println("\nNo paid room records found.");
            return;
        }

        String report = buildPaymentMethodReport(reservations);
        System.out.println(report);
        offerPdfExport("Payment Method Room Report", report,
                ChartType.PAYMENT_METHOD);
    }

    private String buildOutstandingBalanceReport(ListInterface<Reservation> reservations) {
        StringBuilder report = createReportTable(
                "Outstanding Balance Report", "Payment Status", "Balance (RM)");
        int total = 0;
        for (int position = 1; position <= reservations.getNumberOfEntries(); position++) {
            Reservation reservation = reservations.getEntry(position);
            appendReportRow(report, reservation, roomLabel(reservation),
                    String.valueOf(reservation.getPaymentStatus()));
            total++;
        }
        report.append(REPORT_BORDER).append(System.lineSeparator());
        report.append("Total outstanding balance records: ").append(total)
                .append(System.lineSeparator());
        return report.toString();
    }

    private String buildPaymentMethodReport(ListInterface<Reservation> reservations) {
        StringBuilder report = createReportTable(
                "Payment Method Room Report", "Payment Method", "Bill (RM)");
        int total = 0;
        for (int position = 1; position <= reservations.getNumberOfEntries(); position++) {
            Reservation reservation = reservations.getEntry(position);
            appendReportRow(report, reservation, roomLabel(reservation),
                    paymentMethodLabel(reservation));
            total++;
        }
        report.append(REPORT_BORDER).append(System.lineSeparator());
        report.append("Total paid room records: ").append(total)
                .append(System.lineSeparator());
        return report.toString();
    }

    private StringBuilder createReportTable(String title, String paymentColumn,
            String amountColumn) {
        StringBuilder report = new StringBuilder("\n--- ").append(title)
                .append(" ---").append(System.lineSeparator());
        report.append(REPORT_BORDER).append(System.lineSeparator());
        report.append(String.format("| %-10s | %-20s | %-12s | %-22s | %-12s |%n",
                "Room", "Guest", "Confirm No.", paymentColumn, amountColumn));
        report.append(REPORT_BORDER).append(System.lineSeparator());
        return report;
    }

    private void appendReportRow(StringBuilder report, Reservation reservation,
            String room, String paymentDetails) {
        report.append(String.format("| %-10s | %-20.20s | %-12s | %-22.22s | %-12.2f |%n",
                room,
                reservation.getGuest().getFullName(),
                reservation.getConfirmationNumber(),
                paymentDetails,
                control.calculateBill(reservation)));
    }

    private String roomLabel(Reservation reservation) {
        return reservation.getAssignedRoom() == null
                ? "Unassigned" : reservation.getAssignedRoom().getRoomNumber();
    }

    private void offerPdfExport(String title, String report, ChartType chartType) {
        String selection = InputHelper.inputString(
                scanner, "Generate chart PDF and open it? (Y/N): ");
        if (!selection.equalsIgnoreCase("Y") && !selection.equalsIgnoreCase("Yes")) {
            return;
        }

        Path pdfPath;
        try {
            pdfPath = ConsoleAnimation.runIoWithSpinner(
                    () -> ReportPdfExporter.export(title, report, chartType),
                    "Generating report PDF");
        } catch (IOException exception) {
            MessageUI.displayError("Unable to generate PDF: " + exception.getMessage());
            return;
        }

        MessageUI.displaySuccess("PDF generated: " + pdfPath);
        try {
            if (!ReportPdfExporter.open(pdfPath)) {
                MessageUI.displayInfo("Open the PDF manually from the path shown above.");
            }
        } catch (IOException exception) {
            MessageUI.displayInfo(
                    "The PDF was generated but could not be opened automatically: "
                            + exception.getMessage());
        }
    }

    private Reservation findReservationByConfirmationNumber() {
        String confirmationNumber = InputHelper.inputString(
                scanner, "8-digit confirmation number: ").trim();
        if (!Validation.isValidConfirmationNumber(confirmationNumber)) {
            System.out.println("Confirmation number must contain exactly 8 digits.");
            return null;
        }

        Reservation reservation = ConsoleAnimation.runWithSpinner(
                () -> control.findByConfirmationNumber(confirmationNumber),
                "Fetching reservation information");
        if (reservation == null) {
            System.out.println("Guest record not found.");
        }
        return reservation;
    }

    private Reservation selectReservationBySearch() {
        ListInterface<Reservation> matches = findReservationsByPrompt();
        if (matches.isEmpty()) {
            System.out.println("Reservation not found.");
            return null;
        }

        if (matches.getNumberOfEntries() == 1) {
            return matches.getEntry(1);
        }

        System.out.println("\n--- Matching Reservations ---");
        String border = "+-----+--------------+----------------------+-----------+----------------+";
        System.out.println(border);
        System.out.printf("| %-3s | %-12s | %-20s | %-9s | %-14s |%n",
                "No.", "Reservation", "Guest", "Tier", "Status");
        System.out.println(border);
        for (int position = 1; position <= matches.getNumberOfEntries(); position++) {
            Reservation reservation = matches.getEntry(position);
            System.out.printf("| %-3d | %-12s | %-20.20s | %-9s | %-14s |%n",
                    position,
                    reservation.getConfirmationNumber(),
                    reservation.getGuest().getFullName(),
                    reservation.getGuest().getLoyaltyTier(),
                    reservation.getStatus());
        }
        System.out.println(border);

        while (true) {
            int selection = promptPositiveInteger("Select reservation number: ");
            if (selection <= matches.getNumberOfEntries()) {
                return matches.getEntry(selection);
            }
            System.out.println("Please select a number from 1 to "
                    + matches.getNumberOfEntries() + ".");
        }
    }

    private ListInterface<Reservation> findReservationsByPrompt() {
        String searchValue = promptRequiredText(
                "Enter reservation ID / Member ID / Guest Name: ");
        return ConsoleAnimation.runWithSpinner(
                () -> control.findMatchingReservations(searchValue),
                "Searching reservation records");
    }

    private void displayReservationList(ListInterface<Reservation> reservations) {
        if (reservations.isEmpty()) {
            System.out.println("No matching reservation found.");
            return;
        }

        System.out.printf("%-10s %-20s %-10s %-12s %-12s%n",
                "Confirm No.", "Guest", "Room", "Payment", "Bill (RM)");
        Iterator<Reservation> iterator = reservations.iterator();
        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            String room = reservation.getAssignedRoom() == null ? "Unassigned"
                    : reservation.getAssignedRoom().getRoomNumber();
            System.out.printf("%-10s %-20s %-10s %-12s %-12.2f%n",
                    reservation.getConfirmationNumber(),
                    reservation.getGuest().getFullName(),
                    room,
                    reservation.getPaymentStatus(),
                    control.calculateBill(reservation));
        }
    }

    private String paymentMethodLabel(Reservation reservation) {
        String paymentMethod = reservation.getPaymentMethod();
        return paymentMethod == null || paymentMethod.trim().isEmpty()
                ? "Unspecified" : paymentMethod.trim();
    }

    private String promptPaymentMethod() {
        while (true) {
            System.out.println(ConsoleStyle.menu(ConsoleStyle.menuBox("PAYMENT METHOD",
                    "1|Cash", "2|Credit / Debit Card", "3|Touch n Go",
                    "4|Online Banking")));
            switch (InputHelper.inputString(scanner, "Select payment method: ").trim()) {
                case "1":
                    return "Cash";
                case "2":
                    return "Credit / Debit Card";
                case "3":
                    return "Touch n Go";
                case "4":
                    return "Online Banking";
                default:
                    System.out.println("Invalid payment method. Please select 1 to 4.");
            }
        }
    }

    private void displayReservationDetails(Reservation reservation) {
        Room room = reservation.getAssignedRoom();
        long nights = Math.max(1, reservation.getCheckOutDate().toEpochDay()
                - reservation.getCheckInDate().toEpochDay());

        System.out.println("\n--- Reservation Details ---");
        System.out.println("Reservation ID   : " + reservation.getConfirmationNumber());
        System.out.println("Guest ID         : " + reservation.getGuest().getGuestId());
        System.out.println("Guest Name       : " + reservation.getGuest().getFullName());
        System.out.println("Phone Number     : " + reservation.getGuest().getPhoneNumber());
        System.out.println("Loyalty Tier     : " + reservation.getGuest().getLoyaltyTier());
        System.out.println("Room Type        : " + Room.ROOM_TYPE);
        System.out.println("Room / Unit No.  : "
                + (room == null ? "Not assigned" : room.getRoomNumber()));
        System.out.println("Check-in Date    : " + reservation.getCheckInDate());
        System.out.println("Check-out Date   : " + reservation.getCheckOutDate());
        LateCheckoutExtension extension = control.findLateCheckoutExtension(
                reservation.getConfirmationNumber());
        if (extension != null) {
            System.out.println("Extended Check-Out: " + extension.getExtendedCheckOutAt());
        }
        System.out.println("Number of Nights : " + nights);
        if (room != null) {
            System.out.printf("Price per Night  : RM%.2f%n", room.getPricePerNight());
            System.out.printf("Total Price      : RM%.2f%n", control.calculateBill(reservation));
        }
        System.out.println("Payment Method   : " + reservation.getPaymentMethod());
        System.out.println("Payment Status   : " + reservation.getPaymentStatus());
        System.out.println("Status           : " + reservation.getStatus());
    }

    private void displayCheckInFailure(CheckInResult result) {
        switch (result) {
            case PAYMENT_REQUIRED:
                ConsoleAnimation.error("Check-in failed: payment is required.");
                break;
            case ROOM_NOT_RESERVED:
                ConsoleAnimation.error("Check-in failed: the assigned room is not reserved.");
                break;
            default:
                ConsoleAnimation.error("Check-in failed: " + getCheckInFailureMessage(result));
        }
    }

    private String getCheckInFailureMessage(CheckInResult result) {
        switch (result) {
            case RESERVATION_NOT_FOUND:
                return "reservation not found.";
            case NOT_CONFIRMED:
                return "only confirmed reservations can be checked in.";
            case CHECK_IN_DATE_NOT_REACHED:
                return "the check-in date has not been reached.";
            case POINTS_PAYMENT_PENDING:
                return "member-points payment is awaiting Loyalty approval.";
            default:
                return "unable to complete check-in.";
        }
    }

    private String getCheckOutFailureMessage(CheckOutResult result) {
        switch (result) {
            case RESERVATION_NOT_FOUND:
                return "reservation not found.";
            case NOT_CHECKED_IN:
                return "only checked-in reservations can be checked out.";
            case ROOM_NOT_OCCUPIED:
                return "the assigned room is not occupied.";
            default:
                return "unable to complete check-out.";
        }
    }

    private String getLateCheckoutFailureMessage(LateCheckoutStatus status) {
        switch (status) {
            case RESERVATION_NOT_FOUND:
                return "reservation not found.";
            case NOT_CHECKED_IN:
                return "only checked-in guests can extend check-out.";
            case ROOM_NOT_OCCUPIED:
                return "the assigned room is not occupied.";
            case INVALID_EXTENDED_CHECK_OUT_TIME:
                return "the extended check-out time must be in the future.";
            case REASON_REQUIRED:
                return "a reason is required.";
            default:
                return "unable to save the extension.";
        }
    }

    private LocalDateTime promptFutureDateTime(String prompt) {
        while (true) {
            LocalDateTime value = promptDateTime(prompt);
            if (value.isAfter(LocalDateTime.now())) {
                return value;
            }
            System.out.println("Enter a future date and time.");
        }
    }

    private LocalDateTime promptDateTime(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            try {
                return LocalDateTime.parse(value.replace(' ', 'T'));
            } catch (DateTimeParseException exception) {
                System.out.println("Use yyyy-MM-dd HH:mm, for example 2026-08-27 14:00.");
            }
        }
    }

    private String promptRequiredText(String prompt) {
        while (true) {
            String value = InputHelper.inputString(scanner, prompt).trim();
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("This field is required.");
        }
    }

    private int promptPositiveInteger(String prompt) {
        while (true) {
            try {
                int value = Integer.parseInt(
                        InputHelper.inputString(scanner, prompt).trim());
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException exception) {
                // Re-prompt below.
            }
            System.out.println("Enter a positive whole number.");
        }
    }

    private boolean confirmYes(String prompt) {
        while (true) {
            String answer = InputHelper.inputString(scanner, prompt).trim();
            if (answer.equalsIgnoreCase("Y")) {
                return true;
            }
            if (answer.equalsIgnoreCase("N")) {
                return false;
            }
            System.out.println("Please enter Y or N.");
        }
    }
}
