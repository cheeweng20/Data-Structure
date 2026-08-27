package VIPPriorityRoomAllocation.utility;

import VIPPriorityRoomAllocation.entity.LoyaltyTier;
import VIPPriorityRoomAllocation.entity.Reservation;
import VIPPriorityRoomAllocation.entity.ReservationStatus;
import VIPPriorityRoomAllocation.entity.Room;
import adt.ListInterface;
import adt.SortedArrayList;
import adt.SortedListInterface;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

/** Builds reservation report text and the chart-data sections used by PDF export. */
public final class ReservationReportFormatter {

    private ReservationReportFormatter() {
    }

    public static String buildMonthlyReservationSummary(
            ListInterface<Reservation> reservations, YearMonth reportMonth) {
        SortedListInterface<Reservation> reportReservations = new SortedArrayList<>(
                (left, right) -> left.getBookingDateTime()
                        .compareTo(right.getBookingDateTime()));
        Iterator<Reservation> iterator = reservations.iterator();
        int[] statusCounts = new int[ReservationStatus.values().length];
        double totalRevenue = 0.00;

        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            if (!YearMonth.from(reservation.getCheckInDate()).equals(reportMonth)) {
                continue;
            }

            reportReservations.add(reservation);
            statusCounts[reservation.getStatus().ordinal()]++;
            if (reservation.getAssignedRoom() != null
                    && reservation.getStatus() != ReservationStatus.REJECTED) {
                totalRevenue += calculateReservationAmount(reservation);
            }
        }

        if (reportReservations.isEmpty()) {
            return "";
        }

        StringBuilder report = new StringBuilder();
        report.append("=== Monthly Reservation Summary: ").append(reportMonth).append(" ===\n");
        report.append("Total Reservations : ")
                .append(reportReservations.getNumberOfEntries()).append('\n');
        report.append("Pending            : ")
                .append(statusCounts[ReservationStatus.PENDING.ordinal()]).append('\n');
        report.append("Allocated Rooms    : ")
                .append(statusCounts[ReservationStatus.CONFIRMED.ordinal()]
                        + statusCounts[ReservationStatus.CHECKED_IN.ordinal()]
                        + statusCounts[ReservationStatus.CHECKED_OUT.ordinal()])
                .append('\n');
        report.append("Checked-Out        : ")
                .append(statusCounts[ReservationStatus.CHECKED_OUT.ordinal()]).append('\n');
        report.append("Rejected           : ")
                .append(statusCounts[ReservationStatus.REJECTED.ordinal()]).append('\n');
        report.append(String.format("Total Revenue      : RM%.2f%n%n", totalRevenue));
        appendMonthlyReportHeader(report);

        for (int i = 1; i <= reportReservations.getNumberOfEntries(); i++) {
            appendMonthlyReportLine(report, reportReservations.getEntry(i));
        }
        appendMonthlyReportBorder(report);
        appendStatusChartData(report, statusCounts);
        return report.toString();
    }

    public static String buildMonthlyRoomAllocationReport(
            ListInterface<Reservation> reservations, YearMonth reportMonth) {
        SortedListInterface<Reservation> reportReservations = new SortedArrayList<>(
                (left, right) -> left.getAssignedRoom().getRoomNumber()
                        .compareToIgnoreCase(right.getAssignedRoom().getRoomNumber()));
        Iterator<Reservation> iterator = reservations.iterator();
        int[] tierCounts = new int[LoyaltyTier.values().length];

        while (iterator.hasNext()) {
            Reservation reservation = iterator.next();
            if (YearMonth.from(reservation.getCheckInDate()).equals(reportMonth)
                    && reservation.getAssignedRoom() != null
                    && reservation.getStatus() != ReservationStatus.REJECTED) {
                reportReservations.add(reservation);
                tierCounts[reservation.getGuest().getLoyaltyTier().ordinal()]++;
            }
        }

        if (reportReservations.isEmpty()) {
            return "";
        }

        StringBuilder report = new StringBuilder();
        report.append("=== Monthly Room Allocation Report: ").append(reportMonth).append(" ===\n");
        report.append("Total Allocated Reservations : ")
                .append(reportReservations.getNumberOfEntries()).append('\n');
        report.append("Room Type                    : ").append(Room.ROOM_TYPE).append("\n\n");
        appendAllocationReportHeader(report);

        for (int i = 1; i <= reportReservations.getNumberOfEntries(); i++) {
            appendAllocationReportLine(report, reportReservations.getEntry(i));
        }
        appendAllocationReportBorder(report);
        appendTierChartData(report, tierCounts);
        return report.toString();
    }

    private static double calculateReservationAmount(Reservation reservation) {
        Room room = reservation.getAssignedRoom();
        if (room == null) {
            return 0.00;
        }
        long nights = reservation.getCheckOutDate().toEpochDay()
                - reservation.getCheckInDate().toEpochDay();
        return nights * room.getPricePerNight();
    }

    private static void appendMonthlyReportHeader(StringBuilder report) {
        appendMonthlyReportBorder(report);
        report.append(String.format("| %-10s | %-18s | %-9s | %-16s | %-10s | %-7s | %-12s |%n",
                "Res ID", "Guest Name", "Tier", "Booking Time", "Check-In", "Room", "Amount"));
        appendMonthlyReportBorder(report);
    }

    private static void appendMonthlyReportLine(StringBuilder report,
            Reservation reservation) {
        Room room = reservation.getAssignedRoom();
        String roomNumber = room == null ? "-" : room.getRoomNumber();
        String amount = room == null ? "-"
                : String.format("RM%.2f", calculateReservationAmount(reservation));
        String bookingTime = reservation.getBookingDateTime()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        report.append(String.format("| %-10s | %-18.18s | %-9s | %-16s | %-10s | %-7s | %-12s |%n",
                reservation.getConfirmationNumber(),
                reservation.getGuest().getFullName(),
                reservation.getGuest().getLoyaltyTier(),
                bookingTime,
                reservation.getCheckInDate(),
                roomNumber,
                amount));
    }

    private static void appendMonthlyReportBorder(StringBuilder report) {
        report.append("+------------+--------------------+-----------+------------------+------------+---------+--------------+\n");
    }

    private static void appendAllocationReportHeader(StringBuilder report) {
        appendAllocationReportBorder(report);
        report.append(String.format("| %-12s | %-18s | %-9s | %-12s | %-12s | %-12s |%n",
                "Res ID", "Guest Name", "Tier", "Room No.", "Check-In", "Status"));
        appendAllocationReportBorder(report);
    }

    private static void appendAllocationReportLine(StringBuilder report,
            Reservation reservation) {
        Room room = reservation.getAssignedRoom();
        String roomNumber = room == null ? "-" : room.getRoomNumber();

        report.append(String.format("| %-12s | %-18.18s | %-9s | %-12s | %-12s | %-12s |%n",
                reservation.getConfirmationNumber(),
                reservation.getGuest().getFullName(),
                reservation.getGuest().getLoyaltyTier(),
                roomNumber,
                reservation.getCheckInDate(),
                reservation.getStatus()));
    }

    private static void appendAllocationReportBorder(StringBuilder report) {
        report.append("+--------------+--------------------+-----------+--------------+--------------+--------------+\n");
    }

    private static void appendStatusChartData(StringBuilder report, int[] statusCounts) {
        report.append("\n=== Reservation Status Chart Data ===\n");
        report.append(String.format("| %-14s | %-5s |%n", "Label", "Value"));
        for (ReservationStatus status : ReservationStatus.values()) {
            report.append(String.format("| %-14s | %-5d |%n",
                    status, statusCounts[status.ordinal()]));
        }
    }

    private static void appendTierChartData(StringBuilder report, int[] tierCounts) {
        report.append("\n=== Loyalty Tier Allocation Chart Data ===\n");
        report.append(String.format("| %-14s | %-5s |%n", "Label", "Value"));
        for (LoyaltyTier tier : LoyaltyTier.values()) {
            report.append(String.format("| %-14s | %-5d |%n",
                    tier, tierCounts[tier.ordinal()]));
        }
    }
}
