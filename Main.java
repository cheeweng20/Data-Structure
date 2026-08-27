import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import FrontDeskService.boundary.FrontDeskUI;
import HousekeepingAndTaskLog.boundary.HousekeepingUI;
import LoyaltyAndRewardsService.boundary.LoyaltyUI;
import LoyaltyAndRewardsService.control.LoyaltyServiceControl;
import LoyaltyAndRewardsService.entity.Member;
import LoyaltyAndRewardsService.utility.Verification;
import VIPPriorityRoomAllocation.boundary.ReservationUI;
import common.control.StaffAuthenticationControl;
import common.control.MemberIdentityControl;
import common.src.ConsoleStyle;
import common.src.Logo;

/**
 * Application entry point and the two public resort portals.
 *
 * <p>All choices in this class are read as complete lines. A single Scanner is
 * shared with each module UI and is deliberately never closed here.</p>
 */
public class Main {
    private static final String CANCEL = "cancel";
    private static int failedStaffAttempts;
    private static final int MAX_MEMBER_VERIFICATION_ATTEMPTS = 3;
    private static final Map<String, Integer> MEMBER_VERIFICATION_FAILURES =
            new HashMap<>();

    private Main() {
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        StaffAuthenticationControl staffAuthentication = new StaffAuthenticationControl();
        boolean exit = false;

        while (!exit) {
            displayMainMenu();
            String choice = readLine(scanner, "Select an option: ");
            if (choice == null) {
                break;
            }

            switch (choice) {
                case "1":
                    staffLogin(scanner, staffAuthentication);
                    break;
                case "2":
                    memberGuestPortal(scanner);
                    break;
                case "0":
                    exit = true;
                    break;
                default:
                    System.out.println(ConsoleStyle.error("Invalid option. Please enter 1, 2, or 0."));
                    break;
            }
        }

        System.out.println(ConsoleStyle.success("Thank you for using TARUMT Resort."));
    }

    private static void displayMainMenu() {
        Logo.displayMain();
        System.out.println(ConsoleStyle.menu(
                "\n--- TARUMT Resort ---\n"
                        + "1. Staff Login\n"
                        + "2. Member / Guest\n"
                        + "0. Exit\n"));
    }

    /**
     * Authenticates staff before exposing any of the operational modules.
     * Three failed attempts are allowed; entering 0 or cancel backs out.
     */
    private static void staffLogin(Scanner scanner,
            StaffAuthenticationControl authentication) {
        System.out.println(ConsoleStyle.title("\n--- Staff Login ---"));
        System.out.println(ConsoleStyle.muted(
                "Enter 0 or cancel at any prompt to return to the main menu."));

        if (!authentication.isConfigured()) {
            System.out.println(ConsoleStyle.error(
                    "Staff login is not configured. Set RESORT_STAFF_USERNAME and "
                            + "RESORT_STAFF_PASSWORD, or explicitly enable RESORT_DEMO_MODE=true."));
            return;
        }
        if (failedStaffAttempts >= authentication.getMaxAttempts()) {
            System.out.println(ConsoleStyle.error(
                    "Staff login is locked for this application session."));
            return;
        }

        while (failedStaffAttempts < authentication.getMaxAttempts()) {
            String username = readLine(scanner, "Username: ");
            if (isCancelled(username)) {
                return;
            }
            String password = readLine(scanner, "Password: ");
            if (isCancelled(password)) {
                return;
            }

            if (authentication.authenticate(username, password)) {
                failedStaffAttempts = 0;
                System.out.println(ConsoleStyle.success("Staff login successful."));
                staffPortal(scanner);
                return;
            }

            failedStaffAttempts++;
            int remaining = authentication.getMaxAttempts() - failedStaffAttempts;
            if (remaining > 0) {
                System.out.println(ConsoleStyle.error(
                        "Invalid staff credentials. Attempts remaining: " + remaining));
            }
        }

        System.out.println(ConsoleStyle.error("Staff login cancelled after too many failed attempts."));
    }

    /**
     * The staff-only gateway to the four existing service modules.
     * Module controls are created by their UI only after this authenticated
     * session selects a module, preventing controls from being kept stale.
     */
    private static void staffPortal(Scanner scanner) {
        boolean back = false;
        while (!back) {
            System.out.println(ConsoleStyle.title("\n--- Staff Portal ---"));
            System.out.println(ConsoleStyle.menu(
                    "1. VIP & Loyalty Tier Priority Room Allocation\n"
                            + "2. Housekeeping and Task Log\n"
                            + "3. Front Desk Service\n"
                            + "4. Loyalty and Rewards Service\n"
                            + "0. Back\n"));
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
            System.out.println(ConsoleStyle.title("\n--- Member / Guest Portal ---"));
            System.out.println(ConsoleStyle.menu(
                    "1. Existing Member Login\n"
                            + "2. Guest Registration\n"
                            + "0. Back\n"));
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
                    System.out.println(ConsoleStyle.error("Invalid member / guest option."));
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

        Member member = loyalty.getMemberById(memberId);
        if (member == null) {
            System.out.println(ConsoleStyle.error("Member ID not found."));
            return;
        }

        String normalizedMemberId = member.getMemberId().toUpperCase();
        int failedAttempts = MEMBER_VERIFICATION_FAILURES.getOrDefault(
                normalizedMemberId, 0);
        if (failedAttempts >= MAX_MEMBER_VERIFICATION_ATTEMPTS) {
            System.out.println(ConsoleStyle.error(
                    "Member verification is locked for this application session."));
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
            System.out.println(ConsoleStyle.error("Member verification failed."));
            return;
        }

        MEMBER_VERIFICATION_FAILURES.remove(normalizedMemberId);

        showMemberHome(scanner, loyalty, member);
    }

    private static void registerGuest(Scanner scanner, LoyaltyServiceControl loyalty) {
        System.out.println(ConsoleStyle.title("\n--- Guest Registration ---"));
        System.out.println(ConsoleStyle.muted("Enter 0 or cancel at any prompt to stop registration."));

        String name = promptMemberName(scanner);
        if (name == null) {
            return;
        }
        if (!loyalty.isMemberNameAvailable(name, null)) {
            System.out.println(ConsoleStyle.error("That member name is already in use."));
            return;
        }

        String passport = promptPassport(scanner);
        if (passport == null) {
            return;
        }
        if (!loyalty.isPassportAvailable(passport, null)) {
            System.out.println(ConsoleStyle.error(
                    "That passport is already registered to a member."));
            return;
        }

        String phoneNumber = promptPhoneNumber(scanner);
        if (phoneNumber == null) {
            return;
        }
        if (!loyalty.isPhoneNumberAvailable(phoneNumber, null)) {
            System.out.println(ConsoleStyle.error(
                    "That phone number is already registered to a member."));
            return;
        }

        String memberId = loyalty.createMember(name, passport, phoneNumber);
        System.out.println(ConsoleStyle.success(
                "Registration successful. Your Member ID is " + memberId + "."));
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
            if (Verification.isValidMemberName(value)) {
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
            if (Verification.isValidPassport(value)) {
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
            if (Verification.isValidPhoneNumber(value)) {
                return value.trim();
            }
            System.out.println(ConsoleStyle.error("Invalid phone number."));
        }
    }

    private static void showMemberHome(Scanner scanner, LoyaltyServiceControl loyalty,
            Member member) {
        System.out.println(ConsoleStyle.title("\n--- Member Home ---"));
        System.out.println("Member ID : " + member.getMemberId());
        System.out.println("Name      : " + member.getName());
        System.out.println("Phone     : " + member.getPhoneNumber());
        System.out.println("Tier      : " + loyalty.getTierName(member.getTierId()));
        System.out.println("Points    : " + member.getPoint());
        System.out.println("\n" + loyalty.generatePersonalizedPromotion(member.getMemberId()));

        while (true) {
            System.out.println(ConsoleStyle.menu(
                    "\n1. Make a Reservation\n"
                            + "0. Back to Member / Guest Portal\n"));
            String choice = readLine(scanner, "Select an option: ");
            if (choice == null || choice.equals("0")) {
                return;
            }
            if (choice.equals("1")) {
                // ReservationUI.startMember is the member-only entry point.
                new ReservationUI(scanner).startMember(member.getMemberId());
            } else {
                System.out.println(ConsoleStyle.error("Invalid member home option."));
            }
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
