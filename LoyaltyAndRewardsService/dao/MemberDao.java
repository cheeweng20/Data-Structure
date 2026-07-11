package LoyaltyAndRewardsService.dao;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;

import LoyaltyAndRewardsService.control.MemberControl;
import LoyaltyAndRewardsService.entity.Member;

public class MemberDao {
    private static final String FILE_NAME = "src/member.csv";

        // CSV File Reader and Writer
    public static void loadFromMemberFile(MemberControl memberList) {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            reader.readLine(); // Skip file header
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",");

                String memberId = fields[0];
                String name = fields[1];
                int point = Integer.parseInt(fields[2]);
                String tierId = fields[3];

                memberList.addMember(new Member(memberId, name, point, tierId));
            }
        } catch (FileNotFoundException e) {
            System.out.println("No existing member data found, starting fresh.");
            createMemberCSVFile();

        } catch (IOException e) {
            System.out.println("Error reading member file: " + e.getMessage());
        }
    }

    public static void saveToMemberFile(MemberControl memberList) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME))) {
            writer.println("MemberId,Name,Email,TierId");
            for (int i = 1; i <= memberList.size(); i++) {
                Member member = memberList.getEntry(i);
                writer.println(member.toCsvLine());
            }
        } catch (IOException e) {
            System.out.println("Error saving member file: " + e.getMessage());
        }
    }

    private static void createMemberCSVFile() {
        try (PrintWriter writer = new PrintWriter(FILE_NAME)) {
            writer.println("MemberId,Name,Email,TierId");

            System.out.println("CSV File created success !");
        } catch (IOException e) {
            System.out.println("Error creating member file: " + e.getMessage());
        }
    }
}
