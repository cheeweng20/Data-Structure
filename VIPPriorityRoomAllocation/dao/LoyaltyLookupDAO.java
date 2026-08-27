package VIPPriorityRoomAllocation.dao;

import VIPPriorityRoomAllocation.entity.LoyaltyTier;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

// Reads existing loyalty module files to determine reservation priority.
public class LoyaltyLookupDAO {

    private final String memberFileName;
    private final String tierFileName;

    public LoyaltyLookupDAO() {
        this("LoyaltyAndRewardsService/src/member.csv", // Chee Weng member data
                "LoyaltyAndRewardsService/src/tier.csv");
    }

    public LoyaltyLookupDAO(String memberFileName, String tierFileName) {
        this.memberFileName = memberFileName;
        this.tierFileName = tierFileName;
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
                if (fields.length < 7) {
                    continue; // skip rows that do not match the current loyalty CSV format
                }

                String memberIdValue = fields[0].trim();
                String phoneNumber = fields[3].trim();
                boolean memberIdMatches = memberIdValue.equalsIgnoreCase(memberId.trim());
                boolean phoneMatches = normalize(phoneNumber).equals(searchValue); // allow phone search too

                if (!memberIdMatches && !phoneMatches) {
                    continue; // skip rows that are not the selected member
                }

                return new LoyaltyProfile(memberIdValue, fields[1].trim(), phoneNumber,
                        findTierById(fields[6].trim()));
            }
        } catch (IOException ex) {
            return null; // if loyalty file cannot be read, treat as no member found
        }

        return null;
    }

    private LoyaltyTier findTierById(String tierId) {
        if (tierId == null || tierId.trim().isEmpty()) {
            return LoyaltyTier.CLASSIC;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(tierFileName))) {
            reader.readLine(); // skip CSV header
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] fields = line.split(",", -1);
                if (fields.length >= 2 && fields[0].equalsIgnoreCase(tierId.trim())) {
                    return LoyaltyTier.fromTierName(fields[1]); // convert tier name to allocation priority
                }
            }
        } catch (IOException ex) {
            return LoyaltyTier.CLASSIC; // fallback keeps allocation running safely
        }

        return LoyaltyTier.CLASSIC;
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    public static class LoyaltyProfile {

        private final String memberId;
        private final String name;
        private final String phoneNumber;
        private final LoyaltyTier loyaltyTier;

        public LoyaltyProfile(String memberId, String name, String phoneNumber, LoyaltyTier loyaltyTier) {
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

        public LoyaltyTier getLoyaltyTier() {
            return loyaltyTier;
        }
    }
}
