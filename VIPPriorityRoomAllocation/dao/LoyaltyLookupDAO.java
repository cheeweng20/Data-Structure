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
        this("LoyaltyAndRewardsService/src/member.csv",
                "LoyaltyAndRewardsService/src/tier.csv");
    }

    public LoyaltyLookupDAO(String memberFileName, String tierFileName) {
        this.memberFileName = memberFileName;
        this.tierFileName = tierFileName;
    }

    public LoyaltyProfile findProfile(String memberId) {
        if (memberId == null || memberId.trim().isEmpty()) {
            return null;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(memberFileName))) {
            reader.readLine();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] fields = line.split(",", -1);
                if (fields.length < 4 || !fields[0].equalsIgnoreCase(memberId.trim())) {
                    continue;
                }

                int points = Integer.parseInt(fields[2]);
                return new LoyaltyProfile(fields[0], fields[1], findTierByPoints(points));
            }
        } catch (IOException | NumberFormatException ex) {
            return null;
        }

        return null;
    }

    private LoyaltyTier findTierByPoints(int points) {
        LoyaltyTier matchedTier = LoyaltyTier.CLASSIC;
        int highestMinimumPoint = -1;

        try (BufferedReader reader = new BufferedReader(new FileReader(tierFileName))) {
            reader.readLine();
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] fields = line.split(",", -1);
                if (fields.length < 4) {
                    continue;
                }

                int minimumPoint = Integer.parseInt(fields[2]);
                int maximumPoint = Integer.parseInt(fields[3]);
                boolean noUpperLimit = maximumPoint == 0;
                boolean withinRange = points >= minimumPoint && (noUpperLimit || points <= maximumPoint);

                if (withinRange && minimumPoint > highestMinimumPoint) {
                    matchedTier = LoyaltyTier.fromTierName(fields[1]);
                    highestMinimumPoint = minimumPoint;
                }
            }
        } catch (IOException | NumberFormatException ex) {
            return LoyaltyTier.CLASSIC;
        }

        return matchedTier;
    }

    public static class LoyaltyProfile {

        private final String memberId;
        private final String name;
        private final LoyaltyTier loyaltyTier;

        public LoyaltyProfile(String memberId, String name, LoyaltyTier loyaltyTier) {
            this.memberId = memberId;
            this.name = name;
            this.loyaltyTier = loyaltyTier;
        }

        public String getMemberId() {
            return memberId;
        }

        public String getName() {
            return name;
        }

        public LoyaltyTier getLoyaltyTier() {
            return loyaltyTier;
        }
    }
}
