package VIPPriorityRoomAllocation.dao;

import LoyaltyAndRewardsService.entity.Tier;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

// Reads existing loyalty module files to determine reservation priority.
public class LoyaltyLookupDAO {

    private final String memberFileName;

    public LoyaltyLookupDAO() {
        this("LoyaltyAndRewardsService/src/member.csv");
    }

    public LoyaltyLookupDAO(String memberFileName) {
        this.memberFileName = memberFileName;
    }

    public LoyaltyProfile findProfile(String memberId) {
        // search Chee Weng loyalty CSV by member ID or phone number
        if (memberId == null || memberId.trim().isEmpty()) {
            return null; // no search value entered, so no loyalty profile can be found
        }

        String searchValue = normalize(memberId);

        try (BufferedReader reader = new BufferedReader(new FileReader(memberFileName))) {
            reader.readLine(); // skip CSV header
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] fields = line.split(",", -1); // keep empty CSV columns
                if (fields.length < 6) {
                    continue; // skip rows that do not match the current loyalty CSV format
                }

                String memberIdValue = fields[0].trim();
                String phoneNumber = fields[3].trim();
                boolean memberIdMatches = memberIdValue.equalsIgnoreCase(memberId.trim());
                boolean phoneMatches = normalize(phoneNumber).equals(searchValue); // allow phone search too

                if (!memberIdMatches && !phoneMatches) {
                    continue; // skip rows that are not the selected member
                }

                try {
                    int totalExpenses = Integer.parseInt(fields[5].trim());
                    Tier loyaltyTier = Tier.fromPoints(totalExpenses);
                    return new LoyaltyProfile(memberIdValue, fields[1].trim(), phoneNumber,
                            loyaltyTier);
                } catch (NumberFormatException exception) {
                    continue;
                }
            }
        } catch (IOException ex) {
            return null; // if loyalty file cannot be read, treat as no member found
        }

        return null;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    public static class LoyaltyProfile {

        private final String memberId;
        private final String name;
        private final String phoneNumber;
        private final Tier loyaltyTier;

        public LoyaltyProfile(String memberId, String name, String phoneNumber, Tier loyaltyTier) {
            this.memberId = memberId;
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.loyaltyTier = loyaltyTier;
        }

        public String getMemberId() {
            return memberId;
        }

        public String getName() {
            return name;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }

        public Tier getLoyaltyTier() {
            return loyaltyTier;
        }
    }
}
