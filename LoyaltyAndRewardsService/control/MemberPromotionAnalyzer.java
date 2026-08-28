package LoyaltyAndRewardsService.control;

import VIPPriorityRoomAllocation.dao.ReservationDAO;
import VIPPriorityRoomAllocation.entity.Reservation;
import VIPPriorityRoomAllocation.entity.ReservationStatus;
import java.time.DayOfWeek;
import java.time.LocalDate;

/** Selects a promotion by analysing a member's completed booking history. */
public final class MemberPromotionAnalyzer {
    private static final int MINIMUM_HISTORY_SIZE = 2;
    private static final double HISTORY_POINT_MULTIPLIER = 1.5;

    public enum StayPattern {
        WEEKEND,
        WEEKDAY,
        NONE
    }

    public record PromotionOffer(String message, double pointMultiplier,
            StayPattern eligiblePattern, int historySize) {
        public boolean appliesTo(LocalDate checkInDate) {
            if (checkInDate == null || eligiblePattern == StayPattern.NONE) {
                return false;
            }
            boolean weekend = isWeekend(checkInDate);
            return eligiblePattern == StayPattern.WEEKEND ? weekend : !weekend;
        }
    }

    private final ReservationDAO reservationDao;

    public MemberPromotionAnalyzer() {
        this(new ReservationDAO());
    }

    public MemberPromotionAnalyzer(ReservationDAO reservationDao) {
        this.reservationDao = reservationDao;
    }

    public PromotionOffer analyze(String memberId) {
        return analyze(memberId, null);
    }

    /**
     * Analyses earlier stays. The current reservation can be excluded so it
     * does not qualify itself for a promotion during checkout.
     */
    public PromotionOffer analyze(String memberId, String excludedConfirmationNumber) {
        int historySize = 0;
        int weekendStays = 0;
        for (Reservation reservation : reservationDao.retrieveFromFile()) {
            if (!belongsToMember(reservation, memberId)
                    || isExcluded(reservation, excludedConfirmationNumber)
                    || !isHistoricalStay(reservation)) {
                continue;
            }
            historySize++;
            if (isWeekendStay(reservation)) {
                weekendStays++;
            }
        }

        if (historySize < MINIMUM_HISTORY_SIZE) {
            return new PromotionOffer(
                    "Complete at least two stays to unlock a booking-history offer.",
                    1.0, StayPattern.NONE, historySize);
        }

        if (weekendStays * 2 == historySize) {
            return new PromotionOffer(
                    "Your completed stays are evenly split between weekends and weekdays; "
                            + "complete another stay to reveal a preferred booking pattern.",
                    1.0, StayPattern.NONE, historySize);
        }

        StayPattern pattern = weekendStays * 2 > historySize
                ? StayPattern.WEEKEND : StayPattern.WEEKDAY;
        String patternName = pattern == StayPattern.WEEKEND ? "weekend" : "weekday";
        return new PromotionOffer(
                "Based on your " + historySize + " previous stays, you prefer "
                        + patternName + " bookings. Earn 1.5x points on your next "
                        + patternName + " stay.",
                HISTORY_POINT_MULTIPLIER, pattern, historySize);
    }

    private static boolean belongsToMember(Reservation reservation, String memberId) {
        return reservation.getGuest() != null && memberId != null
                && reservation.getGuest().getGuestId().equalsIgnoreCase(memberId.trim());
    }

    private static boolean isExcluded(Reservation reservation, String confirmationNumber) {
        return confirmationNumber != null
                && reservation.getConfirmationNumber()
                        .equalsIgnoreCase(confirmationNumber.trim());
    }

    private static boolean isHistoricalStay(Reservation reservation) {
        return reservation.getStatus() == ReservationStatus.CHECKED_OUT;
    }

    private static boolean isWeekendStay(Reservation reservation) {
        return isWeekend(reservation.getCheckInDate());
    }

    private static boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY
                || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }
}
