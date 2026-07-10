package LoyaltyAndRewardsService;
import java.util.Scanner;


public class LoyaltyandRewardsService {

    public static void displayMenu(Scanner scanner) {
        System.out.println("1. Member");
        System.out.println("2. Tier");
        System.out.println("0. Exit");
        System.out.print("Please Enter A number:");
    }

    public static void memberOperator(Scanner scanner, MemberControl memberLinkedList, TierControl tierLinkedList) {
        boolean exit = false;

        while (!exit) {
            System.out.println("1. New Member");
            System.out.println("2. Remove Member");
            System.out.println("3. Update Member Info");
            System.out.println("4. Add Point for Member");
            System.out.println("5. Member List");
            System.out.println("0. Return Main Menu");
            System.out.print("Please Enter A number:");

            int userEntry = scanner.nextInt();
            switch (userEntry) {
                case 1: {
                    scanner.nextLine();

                    String name = promptText(scanner, "Enter User Name: ");

                    int point = promptInt(scanner, "Enter Current Member Point: ");

                    String newMemberId = memberLinkedList.generateMemberId();
                    scanner.nextLine();

                    String tierId = tierLinkedList.getTierIdByPoint(point);
                    Member member = new Member(newMemberId, name, point, tierId);
                    memberLinkedList.addMember(member);

                    break;
                }

                case 2: {
                    if (memberLinkedList.size() < 1) {
                        System.out.println("Not Member Record");
                        break;
                    }

                    scanner.nextLine();
                    memberLinkedList.displayAllMember();

                    System.out.print("Enter Member ID:");
                    String memberId = scanner.nextLine();

                    if (memberLinkedList.findMember(memberId)) {
                        memberLinkedList.deleteMemberById(memberId);
                        System.out.println("Delete member successfully");
                    } else {
                        System.out.println("Member Not Found");
                    }
                    break;
                }

                case 3: {
                    scanner.nextLine();
                    if (memberLinkedList.size() < 1) {
                        System.out.println("Not Member Record");
                        break;
                    }

                    memberLinkedList.displayAllMember();

                    System.out.print("Enter Member ID to Update:");
                    String memberId = scanner.nextLine();

                    if (memberLinkedList.findMember(memberId)) {
                        System.out.print("Enter Member New Name:");
                        String newName = scanner.nextLine();
                        System.out.print("Enter Member New Point:");
                        int newPoint = scanner.nextInt();

                        memberLinkedList.updateMemberById(memberId, newName, newPoint);
                    } else {
                        System.out.print("Member Not Found");
                    }

                    break;
                }

                case 4: {
                    if (memberLinkedList.size() < 1) {
                        System.out.println("Not Member Record");
                        break;
                    }
                    scanner.nextLine();

                    String memberId = promptText(scanner, "Enter a Member ID:");

                    if (memberLinkedList.findMember(memberId)) {
                        int addPoint = promptInt(scanner, "Please Enter a Added Point: ");

                        int newPoint = memberLinkedList.addMemberPoint(memberId, addPoint);

                        System.out.println("New point " + Integer.toString(addPoint) + " added successful");
                        System.out.println("Current point : " + Integer.toString(newPoint));
                    }
                    break;
                }
                case 5: {
                    memberLinkedList.displayAllMember();
                    break;
                }

                case 0:
                    exit = true;
                    break;
                default:
                    break;
            }
        }
    }

    public static void tierOperator(Scanner scanner, TierControl tierLinkedList) {
        boolean exit = false;

        while (!exit) {
            System.out.println("1. New Tier Level");
            System.out.println("2. Remove Tier Level");
            System.out.println("3. Update Tier Level Info");
            System.out.println("4. Tier List");
            System.out.println("0. Return Main Menu");

            int userEntry = promptInt(scanner, "Please Enter A number:");

            switch (userEntry) {
                case 1: {
                    scanner.nextLine();
                    String tierLevel = promptText(scanner, "Tier Level Name: ");
                    int minPoint = promptInt(scanner, "Min Point: ");
                    int maxPoint = promptInt(scanner, "Max Point: ");

                    String tierId = tierLinkedList.generateTierId();
                    Tier tier = new Tier(tierId, tierLevel, minPoint, maxPoint);
                    tierLinkedList.addTierLevel(tier);
                    break;
                }
                case 2: {
                    scanner.nextLine();
                    if (tierLinkedList.size() < 1) {
                        System.out.println("Not Tier Record");
                        break;
                    }

                    tierLinkedList.displayAllTierLevel();

                    String tierId = promptText(scanner, "Enter Tier ID:");

                    if (tierLinkedList.findTier(tierId)) {
                        tierLinkedList.removeTierLevel(tierId);
                        System.out.println("Delete Tier Level successfully");
                    } else {
                        System.out.println("Tier Level Not Found");
                    }
                    break;
                }
                case 3: {
                    scanner.nextLine();
                    if (tierLinkedList.size() < 1) {
                        System.out.println("Not Tier Record");
                        break;
                    }

                    tierLinkedList.displayAllTierLevel();

                    String tierId = promptText(scanner, "Enter Tier ID to Update:");

                    if (tierLinkedList.findTier(tierId)) {
                        String newName = promptText(scanner, "Enter New Tier Level Name:");
                        int minPoint = promptInt(scanner, "Enter New Min Point:");
                        int maxPoint = promptInt(scanner, "Enter New Max Point:");

                        tierLinkedList.updateTierLevelById(tierId, newName, minPoint, maxPoint);
                    } else {
                        System.out.print("Tier Level Not Found");
                    }

                    break;
                }
                case 4: {
                    tierLinkedList.displayAllTierLevel();
                    break;
                }
                case 0:
                    exit = true;
                    break;
                default:
                    break;
            }
        }
    }

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);
        boolean exit = false;

        TierControl tierLevelList = new TierControl();
        tierLevelList.loadFromTierFile();

        MemberControl memberList = new MemberControl(tierLevelList);
        memberList.loadFromMemberFile();
        while (!exit) {
            displayMenu(input);
            int menuSelected = input.nextInt();
            switch (menuSelected) {
                case 1:
                    memberOperator(input, memberList, tierLevelList);
                    break;
                case 2:
                    tierOperator(input, tierLevelList);
                    break;
                case 0:
                    exit = true;
                default:
                    break;
            }
        }

        memberList.saveToMemberFile();
        tierLevelList.saveToTierFile();
        input.close();
    }

    // Helper Function
    private static String promptText(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    private static int promptInt(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextInt();
    }
}
