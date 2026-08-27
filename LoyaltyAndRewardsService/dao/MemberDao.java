package LoyaltyAndRewardsService.dao;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import LoyaltyAndRewardsService.entity.Member;
import adt.LinkedList;
import adt.ListInterface;

/**
 * @author Chee Weng
 */
public class MemberDao {
    private static final String DEFAULT_FILE_NAME =
            "LoyaltyAndRewardsService/src/member.csv";
    private final String fileName;

    public MemberDao() {
        this(DEFAULT_FILE_NAME);
    }

    public MemberDao(String fileName) {
        this.fileName = fileName;
    }

    // CSV File Reader and Writer
    public ListInterface<Member> retrieveFromFile() {
        ListInterface<Member> members = new LinkedList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
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
                    if (fields.length < 6) {
                        throw new IllegalArgumentException("expected at least 6 columns");
                    }

                    String memberId = fields[0].trim();
                    String name = fields[1].trim();
                    String passport = fields[2].trim();
                    String phoneNumber = fields[3].trim();
                    int point = Integer.parseInt(fields[4].trim());
                    int lifetimePointsEarned = Integer.parseInt(fields[5].trim());

                    if (memberId.isEmpty() || name.isEmpty() || passport.isEmpty()
                            || phoneNumber.isEmpty() || point < 0
                            || lifetimePointsEarned < point) {
                        throw new IllegalArgumentException("invalid required member value");
                    }

                    members.add(new Member(memberId, name, passport, phoneNumber, point,
                            lifetimePointsEarned));
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
        return members;
    }

    public void saveToFile(ListInterface<Member> members) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
            writer.println("MemberId,Name,Passport,PhoneNumber,Point,LifetimePointsEarned");
            for (Member member : members) {
                writer.println(member.toCsvLine());
            }
        } catch (IOException e) {
            System.out.println("Error saving member file: " + e.getMessage());
        }
    }

    private void createMemberCSVFile() {
        try (PrintWriter writer = new PrintWriter(fileName)) {
            writer.println("MemberId,Name,Passport,PhoneNumber,Point,LifetimePointsEarned");

            System.out.println("CSV File created success !");
        } catch (IOException e) {
            System.out.println("Error creating member file: " + e.getMessage());
        }
    }
}
