package LoyaltyAndRewardsService.dao;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import LoyaltyAndRewardsService.control.LoyaltyServiceControl;
import LoyaltyAndRewardsService.entity.Member;

/**
 * @author Chee Weng
 */
public class MemberDao {
    private static final String FILE_NAME = "LoyaltyAndRewardsService/src/member.csv";

        // CSV File Reader and Writer
    public static void loadFromMemberFile(LoyaltyServiceControl memberList) {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            reader.readLine(); // Skip file header
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    String[] fields = line.split(",", -1);
                    if (fields.length < 4) {
                        throw new IllegalArgumentException("expected at least 4 columns");
                    }

                    String memberId = fields[0].trim();
                    String name = fields[1].trim();
                    boolean hasContactFields = fields.length >= 6;
                    String passport = hasContactFields ? fields[2].trim() : "";
                    String phoneNumber = hasContactFields ? fields[3].trim() : "";
                    int point = Integer.parseInt(fields[hasContactFields ? 4 : 2].trim());
                    String tierId = fields[hasContactFields ? 5 : 3].trim();
                    int notifiedTierIndex = hasContactFields ? 6 : 4;
                    String lastNotifiedTierId = fields.length > notifiedTierIndex
                            && !fields[notifiedTierIndex].isBlank()
                            ? fields[notifiedTierIndex].trim()
                            : tierId;

                    if (memberId.isEmpty() || name.isEmpty() || tierId.isEmpty() || point < 0) {
                        throw new IllegalArgumentException("invalid required member value");
                    }

                    memberList.addMember(
                            new Member(memberId, name, passport, phoneNumber, point, tierId,
                                    lastNotifiedTierId));
                } catch (RuntimeException exception) {
                    System.out.println("Skipping invalid member record at line "
                            + lineNumber + ": " + exception.getMessage());
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("No existing member data found, starting fresh.");
            createMemberCSVFile();

        } catch (IOException e) {
            System.out.println("Error reading member file: " + e.getMessage());
        }
    }

    public static void saveToMemberFile(LoyaltyServiceControl memberList) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            writer.println("MemberId,Name,Passport,PhoneNumber,Point,TierId,LastNotifiedTierId");
            for (int i = 1; i <= memberList.getMemberCount(); i++) {
                Member member = memberList.getMemberEntry(i);
                writer.println(member.toCsvLine());
            }
        } catch (IOException e) {
            System.out.println("Error saving member file: " + e.getMessage());
        }
    }

    private static void createMemberCSVFile() {
        try (PrintWriter writer = new PrintWriter(FILE_NAME)) {
            writer.println("MemberId,Name,Passport,PhoneNumber,Point,TierId,LastNotifiedTierId");

            System.out.println("CSV File created success !");
        } catch (IOException e) {
            System.out.println("Error creating member file: " + e.getMessage());
        }
    }
}
