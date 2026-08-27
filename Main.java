import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import FrontDeskService.boundary.FrontDeskUI;
import HousekeepingAndTaskLog.boundary.HousekeepingUI;
import LoyaltyAndRewardsService.boundary.LoyaltyUI;
import LoyaltyAndRewardsService.control.LoyaltyServiceControl;
import LoyaltyAndRewardsService.entity.Member;
import VIPPriorityRoomAllocation.boundary.ReservationUI;
import common.control.MemberIdentityControl;
import common.src.ConsoleStyle;
import common.src.ConsoleProgress;
import common.src.ConsoleAnimation;
import common.src.InputHelper;
import common.src.Logo;
import common.utility.Validation;

/**
 * Application entry point and the two public resort portals.
 *
 * <p>All choices in this class are read as complete lines. A single Scanner is
 * shared with each module UI and is deliberately never closed here.</p>
 */
public class Main {
    private static final String CANCEL = "cancel";
    private static final int MAX_MEMBER_VERIFICATION_ATTEMPTS = 3;
    private static final Map<String, Integer> MEMBER_VERIFICATION_FAILURES =
            new HashMap<>();

    private Main() {
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;
        boolean startupShown = false;

        while (!exit) {
            InputHelper.clearScreen();
            if (!startupShown) {
                ConsoleAnimation.startup();
                startupShown = true;
            }
            displayMainMenu();
            String choice = readLine(scanner, "Select an option: ");
            if (choice == null) {
                break;
            }

            switch (choice) {
                case "1":
                    staffPortal(scanner);
                    break;
                case "2":
                    memberGuestPortal(scanner);
                    break;
                case "0":
                    exit = true;
                    break;
                default:
                    System.out.println(ConsoleStyle.error("Invalid option. Please enter 0, 1, or 2."));
                    InputHelper.pressEnterToContinue(scanner);
                    break;
            }
        }

        System.out.println(ConsoleStyle.success("Thank you for using TARUMT Resort."));
    }

    private static void displayMainMenu() {
        Logo.displayMain();
        System.out.println(ConsoleStyle.menu(ConsoleStyle.menuBox("TARUMT RESORT",
                "1|Staff", "2|Member / Guest", "0|Exit")));
    }

    /**
     * The staff gateway to the four existing service modules.
     * Module controls are created only after a module is selected, so each
     * visit reads the latest persisted reservation and room state.
     */
    private static void staffPortal(Scanner scanner) {
        boolean back = false;
        while (!back) {
            InputHelper.clearScreen();
            Logo.displayMain();
            System.out.println(ConsoleStyle.menu(ConsoleStyle.menuBox("STAFF PORTAL",
                    "1|VIP & Loyalty Tier Priority Room Allocation",
                    "2|Housekeeping and Task Log",
                    "3|Front Desk Service",
                    "4|Loyalty and Rewards Service",
                    "0|Back")));
            String choice = readLine(scanner, "Select a staff function: ");
            if (choice == null) {
                return;
            }

            switch (choice) {
                case "1":
                    new ReservationUI(scanner).start();
                    break;
                case "2":
                    new HousekeepingUI(scanner).start();
                    break;
                case "3":
                    new FrontDeskUI(scanner).start();
                    break;
                case "4":
                    new LoyaltyUI(scanner).start();
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println(ConsoleStyle.error("Invalid staff option."));
                    InputHelper.pressEnterToContinue(scanner);
                    break;
            }
        }
    }

    /**
     * Member and guest entry point. A LoyaltyServiceControl is scoped to this
     * portal session so registration and lookup use one consistent snapshot.
     */
    private static void memberGuestPortal(Scanner scanner) {
        LoyaltyServiceControl loyalty = new LoyaltyServiceControl();
        boolean back = false;

        while (!back) {
            InputHelper.clearScreen();
            Logo.displayMain();
            System.out.println(ConsoleStyle.menu(ConsoleStyle.menuBox("MEMBER / GUEST PORTAL",
                    "1|Existing Member Login", "2|Register as Member", "0|Back")));
            String choice = readLine(scanner, "Select an option: ");
            if (choice == null) {
                return;
            }

            switch (choice) {
                case "1":
                    existingMemberLogin(scanner, loyalty);
                    break;
                case "2":
                    registerGuest(scanner, loyalty);
                    break;
                case "0":
                    back = true;
                    break;
                default:
                    System.out.println(ConsoleStyle.error("Invalid member option."));
                    InputHelper.pressEnterToContinue(scanner);
                    break;
            }
        }
    }

    private static void existingMemberLogin(Scanner scanner, LoyaltyServiceControl loyalty) {
        String memberId = readLine(scanner,
                "Enter your Member ID (or 0 to cancel): ");
        if (isCancelled(memberId)) {
            return;
        }

        Member member = ConsoleProgress.run(
                () -> loyalty.getMemberById(memberId),
                "Fetching member information...",
                "Checking member records...",
                "Preparing member verification...");
        if (member == null) {
            ConsoleAnimation.error("Member ID not found.");
            InputHelper.pressEnterToContinue(scanner);
            return;
        }

        String normalizedMemberId = member.getMemberId().toUpperCase();
        int failedAttempts = MEMBER_VERIFICATION_FAILURES.getOrDefault(
                normalizedMemberId, 0);
        if (failedAttempts >= MAX_MEMBER_VERIFICATION_ATTEMPTS) {
            ConsoleAnimation.error(
                    "Member verification is locked for this application session.");
            InputHelper.pressEnterToContinue(scanner);
            return;
        }

        String identityProof = readLine(scanner,
                "Enter your registered passport or phone number: ");
        if (isCancelled(identityProof)
                || !MemberIdentityControl.verify(member, identityProof)) {
            if (!isCancelled(identityProof)) {
                MEMBER_VERIFICATION_FAILURES.put(normalizedMemberId,
                        failedAttempts + 1);
            }
            ConsoleAnimation.error("Member verification failed.");
            InputHelper.pressEnterToContinue(scanner);
            return;
        }

        MEMBER_VERIFICATION_FAILURES.remove(normalizedMemberId);
        showMemberHome(scanner, loyalty, member);
    }

    private static void registerGuest(Scanner scanner, LoyaltyServiceControl loyalty) {
        System.out.println(ConsoleStyle.title("\n--- Register as Member ---"));
        System.out.println(ConsoleStyle.muted("Enter 0 or cancel at any prompt to stop registration."));

        String name = promptMemberName(scanner);
        if (name == null) {
            return;
        }
        String passport = promptPassport(scanner);
        if (passport == null) {
            return;
        }
        if (!loyalty.isPassportAvailable(passport)) {
            System.out.println(ConsoleStyle.error(
                    "That passport is already registered to a member."));
            InputHelper.pressEnterToContinue(scanner);
            return;
        }

        String phoneNumber = promptPhoneNumber(scanner);
        if (phoneNumber == null) {
            return;
        }
        if (!loyalty.isPhoneNumberAvailable(phoneNumber)) {
            System.out.println(ConsoleStyle.error(
                    "That phone number is already registered to a member."));
            InputHelper.pressEnterToContinue(scanner);
            return;
        }

        String memberId = ConsoleProgress.run(
                () -> loyalty.createMember(name, passport, phoneNumber),
                "Processing member details...",
                "Creating member profile...",
                "Preparing member home...");
        ConsoleAnimation.success(
                "Registration successful. Your Member ID is " + memberId + ".");
        Member member = loyalty.getMemberById(memberId);
        if (member != null) {
            showMemberHome(scanner, loyalty, member);
        }
    }

    private static String promptMemberName(Scanner scanner) {
        while (true) {
            String value = readLine(scanner, "Full name: ");
            if (isCancelled(value)) {
                return null;
            }
            if (Validation.isValidMemberName(value)) {
                return value.trim();
            }
            System.out.println(ConsoleStyle.error(
                    "Invalid name. Use 3-20 letters, spaces, apostrophes, hyphens, or dots."));
        }
    }

    private static String promptPassport(Scanner scanner) {
        while (true) {
            String value = readLine(scanner, "Passport number: ");
            if (isCancelled(value)) {
                return null;
            }
            if (Validation.isValidPassport(value)) {
                return value.trim();
            }
            System.out.println(ConsoleStyle.error("Invalid passport. Use 5-20 letters or digits."));
        }
    }

    private static String promptPhoneNumber(Scanner scanner) {
        while (true) {
            String value = readLine(scanner, "Phone number: ");
            if (isCancelled(value)) {
                return null;
            }
            if (Validation.isValidPhoneNumber(value)) {
                return value.trim();
            }
            System.out.println(ConsoleStyle.error("Invalid phone number."));
        }
    }

    private static void showMemberHome(Scanner scanner, LoyaltyServiceControl loyalty,
            Member member) {
        while (true) {
            InputHelper.clearScreen();
            System.out.println(ConsoleStyle.title("\n--- Member Home ---"));
            System.out.println("Member ID : " + member.getMemberId());
            System.out.println("Name      : " + member.getName());
            System.out.println("Phone     : " + member.getPhoneNumber());
            System.out.println("Tier      : " + loyalty.getTierName(member));
            System.out.println("Points    : " + member.getPoint());
            displayMemberPromotionTable(
                    loyalty.generatePersonalizedPromotion(member.getMemberId()));
            System.out.println(ConsoleStyle.menu(ConsoleStyle.menuBox("MEMBER HOME",
                    "1|Make a Reservation", "2|View My Reservations",
                    "3|Pay Reservation with Points",
                    "0|Back to Member / Guest Portal")));
            String choice = readLine(scanner, "Select an option: ");
            if (choice == null || choice.equals("0")) {
                return;
            }
            if (choice.equals("1")) {
                // ReservationUI.startMember is the member-only entry point.
                new ReservationUI(scanner).startMember(member.getMemberId());
            } else if (choice.equals("2")) {
                new ReservationUI(scanner).viewMemberReservations(member.getMemberId());
                InputHelper.pressEnterToContinue(scanner);
            } else if (choice.equals("3")) {
                new ReservationUI(scanner).startMemberPayment(member.getMemberId());
                InputHelper.pressEnterToContinue(scanner);
            } else {
                System.out.println(ConsoleStyle.error("Invalid member home option."));
                InputHelper.pressEnterToContinue(scanner);
            }
        }
    }

    private static void displayMemberPromotionTable(String promotion) {
        final int labelWidth = 24;
        final int valueWidth = 76;
        String border = "+" + "-".repeat(labelWidth + 2)
                + "+" + "-".repeat(valueWidth + 2) + "+";

        System.out.println(ConsoleStyle.title("\n--- Personalized Promotion ---"));
        System.out.println(ConsoleStyle.tableBorder(border));
        System.out.println(ConsoleStyle.tableHeader(String.format(
                "| %-" + labelWidth + "s | %-" + valueWidth + "s |%n",
                "Promotion Item", "Details")));
        System.out.println(ConsoleStyle.tableBorder(border));

        String[] lines = promotion.split("\\R");
        for (String line : lines) {
            int separator = line.indexOf(':');
            if (separator < 0) {
                continue;
            }
            String label = line.substring(0, separator).trim();
            String value = line.substring(separator + 1).trim();
            printPromotionRow(border, label, value, labelWidth, valueWidth);
        }
        System.out.println(ConsoleStyle.tableBorder(border));
    }

    private static void printPromotionRow(String border, String label, String value,
            int labelWidth, int valueWidth) {
        String remaining = value;
        boolean firstLine = true;
        while (!remaining.isEmpty()) {
            int end = Math.min(valueWidth, remaining.length());
            if (end < remaining.length()) {
                int space = remaining.lastIndexOf(' ', end);
                if (space > 0) {
                    end = space;
                }
            }
            String line = remaining.substring(0, end).trim();
            System.out.printf("| %-" + labelWidth + "s | %-" + valueWidth + "s |%n",
                    firstLine ? label : "", line);
            remaining = remaining.substring(end).trim();
            firstLine = false;
        }
        if (value.isEmpty()) {
            System.out.printf("| %-" + labelWidth + "s | %-" + valueWidth + "s |%n",
                    label, "");
        }
    }

    private static boolean isCancelled(String value) {
        return value == null || value.equals("0") || value.equalsIgnoreCase(CANCEL);
    }

    private static String readLine(Scanner scanner, String prompt) {
        System.out.print(ConsoleStyle.prompt(prompt));
        return scanner.hasNextLine() ? scanner.nextLine().trim() : null;
    }
}
