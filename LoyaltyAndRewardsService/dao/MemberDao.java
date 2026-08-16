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
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",", -1);

                String memberId = fields[0];
                String name = fields[1];
                boolean hasContactFields = fields.length >= 6;
                String passport = hasContactFields ? fields[2] : "";
                String phoneNumber = hasContactFields ? fields[3] : "";
                int point = Integer.parseInt(fields[hasContactFields ? 4 : 2]);
                String tierId = fields[hasContactFields ? 5 : 3];
                int notifiedTierIndex = hasContactFields ? 6 : 4;
                String lastNotifiedTierId = fields.length > notifiedTierIndex
                        && !fields[notifiedTierIndex].isBlank()
                        ? fields[notifiedTierIndex]
                        : tierId;

                memberList.addMember(
                        new Member(memberId, name, passport, phoneNumber, point, tierId,
                                lastNotifiedTierId));
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
