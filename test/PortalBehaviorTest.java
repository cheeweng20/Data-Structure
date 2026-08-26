import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import LoyaltyAndRewardsService.control.LoyaltyServiceControl;
import LoyaltyAndRewardsService.dao.MemberDao;
import LoyaltyAndRewardsService.dao.PointTransactionDao;
import LoyaltyAndRewardsService.dao.RequestDao;
import LoyaltyAndRewardsService.dao.TierDao;
import VIPPriorityRoomAllocation.control.ReservationManager;
import VIPPriorityRoomAllocation.dao.LoyaltyLookupDAO;
import VIPPriorityRoomAllocation.dao.ReservationDAO;
import VIPPriorityRoomAllocation.dao.RoomDAO;
import VIPPriorityRoomAllocation.entity.Reservation;
import common.control.StaffAuthenticationControl;
import common.control.MemberIdentityControl;
import LoyaltyAndRewardsService.entity.Member;
import LoyaltyAndRewardsService.utility.Verification;

/**
 * Lightweight regression tests for the two top-level portals.
 *
 * <p>This class intentionally uses only the JDK. Run with assertions enabled or
 * invoke its main method directly; failures throw AssertionError.</p>
 */
public final class PortalBehaviorTest {
    private PortalBehaviorTest() {
    }

    public static void main(String[] args) throws Exception {
        testStaffAuthentication();
        testMemberIdentityVerification();
        testMemberValidationAndTemporaryData();
        testExactMemberReservationFiltering();
        System.out.println("PortalBehaviorTest: PASS");
    }

    private static void testMemberIdentityVerification() {
        Member member = new Member("M001", "Alice Tan", "AB12345",
                "+60 12-345 6789", 10, 10, "T003", "T003");
        check(MemberIdentityControl.verify(member, "ab12345"),
                "passport verification should be case-insensitive");
        check(MemberIdentityControl.verify(member, "+60123456789"),
                "phone verification should ignore formatting");
        check(!MemberIdentityControl.verify(member, "wrong"),
                "incorrect member proof should fail");
        Member malformedPhone = new Member("M002", "Legacy Guest", "XY98765",
                "-", 0, 0, "T003", "T003");
        check(!MemberIdentityControl.verify(malformedPhone, "."),
                "empty normalized phone values must not authenticate");
        check(!MemberIdentityControl.verify(null, "AB12345"),
                "missing member should fail safely");
    }

    private static void testStaffAuthentication() {
        StaffAuthenticationControl policy =
                new StaffAuthenticationControl("operator", "s3cret", 2);
        check(policy.getMaxAttempts() == 2, "explicit attempt bound should be retained");
        check(policy.authenticate("operator", "s3cret"), "valid staff credentials should pass");
        check(!policy.authenticate("operator", "wrong"), "wrong password should fail");
        check(!policy.authenticate("wrong", "s3cret"), "wrong username should fail");
        check(!policy.authenticate(null, "s3cret"), "null username should fail safely");
        check(!policy.authenticate("operator", null), "null password should fail safely");

        StaffAuthenticationControl defaults = new StaffAuthenticationControl();
        check(defaults.getMaxAttempts() == 3, "default policy should allow three attempts");

        expectIllegalArgument(() -> new StaffAuthenticationControl(null, "password"));
        expectIllegalArgument(() -> new StaffAuthenticationControl("user", ""));
        expectIllegalArgument(() -> new StaffAuthenticationControl("user", "password", 0));
        expectIllegalArgument(() -> new StaffAuthenticationControl(" ", "password", 1));
    }

    private static void testMemberValidationAndTemporaryData() throws IOException {
        check(Verification.isValidMemberName("A Valid Guest"), "valid member name rejected");
        check(!Verification.isValidMemberName("x"), "short member name accepted");
        check(Verification.isValidPassport("AB12345"), "valid passport rejected");
        check(!Verification.isValidPassport("A-123"), "punctuated passport accepted");
        check(Verification.isValidPhoneNumber("+60123456789"), "valid phone rejected");
        check(!Verification.isValidPhoneNumber("abc"), "alphabetic phone accepted");

        Path directory = Files.createTempDirectory("portal-loyalty-test-");
        try {
            Path members = write(directory, "members.csv",
                    "MemberId,Name,Passport,PhoneNumber,Point,LifetimePointsEarned,TierId,LastNotifiedTierId\n"
                            + "M001,Alice Tan,AB12345,+60111111111,10,10,T003,T003\n"
                            + "M009,Other Guest,XY98765,+60122222222,0,0,T003,T003\n");
            Path tiers = write(directory, "tiers.csv",
                    "TierId,TierLevel,MinPoint,MaxPoint\n"
                            + "T003,Classic,0,199\n"
                            + "T002,Silver,200,499\n");
            Path transactions = write(directory, "transactions.csv",
                    "TransactionId,MemberId,PointsEarned,PointsRemaining,EarnedDate,ExpiryDate,SourceReference\n");
            Path requests = write(directory, "requests.csv",
                    "RequestId,MemberId,ConfirmationNumber,PointsRequested,RequestDate,Status\n");

            LoyaltyServiceControl loyalty = new LoyaltyServiceControl(
                    new MemberDao(members.toString()),
                    new PointTransactionDao(transactions.toString()),
                    new RequestDao(requests.toString()),
                    new TierDao(tiers.toString()));

            check(loyalty.getMemberCount() == 2, "temporary member records were not loaded");
            check(loyalty.getMemberById("m001") != null,
                    "member lookup should be case-insensitive");
            check(loyalty.getMemberById("M404") == null,
                    "unknown member should not be returned");
            check(!loyalty.isMemberNameAvailable(" alice tan ", null),
                    "duplicate member name should be rejected");
            check(loyalty.isMemberNameAvailable("alice tan", "M001"),
                    "editing the same member should allow its existing name");
            check(loyalty.isMemberNameAvailable("New Guest", null),
                    "new member name should be available");
            check(!loyalty.isPassportAvailable(" ab12345 ", null),
                    "duplicate passport should be rejected case-insensitively");
            check(loyalty.isPassportAvailable("AB12345", "m001"),
                    "editing the same member should allow its existing passport");
            check(!loyalty.isPhoneNumberAvailable("+60 1111-11111", null),
                    "phone uniqueness should ignore formatting characters");
            check(loyalty.isPhoneNumberAvailable("+60 1111-11111", "M001"),
                    "editing the same member should allow its existing phone");

            String id = loyalty.createMember("New Guest", "ZX12345", "+60133333333");
            check("M010".equals(id), "new member ID should follow highest existing ID");
            check(loyalty.getMemberById(id) != null, "new member should be available in session");
            check(Files.readAllLines(members).stream().anyMatch(line -> line.startsWith(id + ",")),
                    "new member should persist to injected member DAO path");
        } finally {
            deleteTree(directory);
        }
    }

    private static void testExactMemberReservationFiltering() throws IOException {
        Path directory = Files.createTempDirectory("portal-reservation-test-");
        try {
            Path reservations = write(directory, "reservations.csv",
                    "ConfirmationNumber,GuestId,GuestName,PhoneNumber,LoyaltyTier,AssignedRoomNumber,AssignedRoomPrice,AssignedRoomStatus,CheckInDate,CheckOutDate,BookingDateTime,PaymentMethod,PaymentStatus,Status,TemporaryCheckOutAt\n"
                            + "C001,M001,Alice,+60111111111,PLATINUM,,,,2026-08-01,2026-08-02,2026-07-01T10:00,,,PENDING,\n"
                            + "C002,M0010,Prefix Match,+60111111112,GOLD,,,,2026-08-03,2026-08-04,2026-07-02T10:00,,,PENDING,\n"
                            + "C003,m001,Case Match,+60111111113,SILVER,,,,2026-08-05,2026-08-06,2026-07-03T10:00,,,PENDING,\n"
                            + "C004,M002,Other Member,+60111111114,CLASSIC,,,,2026-08-07,2026-08-08,2026-07-04T10:00,,,PENDING,\n");
            Path rooms = write(directory, "rooms.csv", "RoomNumber,PricePerNight,Status\n");
            Path members = write(directory, "members.csv", "MemberId,Name,Passport,PhoneNumber,Point,LifetimePointsEarned,TierId,LastNotifiedTierId\n");
            Path tiers = write(directory, "tiers.csv", "TierId,TierLevel,MinPoint,MaxPoint\n");

            ReservationManager manager = new ReservationManager(
                    new ReservationDAO(reservations.toString()),
                    new RoomDAO(rooms.toString()),
                    new LoyaltyLookupDAO(members.toString(), tiers.toString()));

            List<Reservation> exact = toJavaList(manager.findReservationsByGuestId(" m001 "));
            check(exact.size() == 2, "exact guest-ID filter should match case-insensitively only");
            check(exact.stream().map(Reservation::getConfirmationNumber)
                    .collect(Collectors.toSet()).containsAll(List.of("C001", "C003")),
                    "exact guest-ID filter returned an unexpected reservation");
            check(toJavaList(manager.findReservationsByGuestId("M00")).isEmpty(),
                    "partial guest-ID filter should return no reservations");
            check(toJavaList(manager.findReservationsByGuestId(null)).isEmpty(),
                    "null guest-ID filter should return no reservations");
        } finally {
            deleteTree(directory);
        }
    }

    private static Path write(Path directory, String fileName, String content) throws IOException {
        Path file = directory.resolve(fileName);
        Files.writeString(file, content);
        return file;
    }

    private static List<Reservation> toJavaList(adt.ListInterface<Reservation> values) {
        List<Reservation> result = new java.util.ArrayList<>();
        Iterator<Reservation> iterator = values.iterator();
        while (iterator.hasNext()) {
            result.add(iterator.next());
        }
        return result;
    }

    private static void deleteTree(Path directory) throws IOException {
        try (Stream<Path> paths = Files.walk(directory)) {
            paths.sorted((left, right) -> right.compareTo(left)).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            });
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expectIllegalArgument(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("expected IllegalArgumentException");
    }
}
