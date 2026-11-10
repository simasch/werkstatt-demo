package ai.unifiedprocess.roombooking.booking.service;

import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import ai.unifiedprocess.roombooking.TestcontainersConfiguration;
import ai.unifiedprocess.roombooking.usecase.UseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Service tests for UC-002 "Book Room" — only what the integration layer (the Browserless test
 * {@code UC002BookRoomTest}) cannot reach: an overlapping booking of the same room committed by a competing
 * transaction after {@link BookingService#book} has checked for overlaps and before it stores the booking
 * (step 7, A3, BR-002). Test data: the room Studio of {@code R__uc002_book_room.sql}. Not
 * {@code @Transactional}; every booking a test creates has a name starting with "UC002" and is removed
 * afterwards.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UC002BookRoomServiceTest {

    private static final LocalDateTime DAY = LocalDateTime.of(2030, 3, 5, 0, 0);

    private static final String NAME = "UC002 Dana";

    private static final String COMPETITOR = "UC002 Erin";

    @Autowired
    private BookingService bookingService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcClient jdbcClient;

    @AfterEach
    void removeCreatedBookings() {
        jdbcClient.sql("delete from booking where booked_by like 'UC002%'").update();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A3: Overlapping Booking", businessRules = {"BR-002"})
    void booking_saved_meanwhile_between_check_and_insert_is_rejected() throws Exception {
        long studioId = jdbcClient.sql("select id from room where name = 'Studio'").query(Long.class).single();
        CompletableFuture<Long> booking;
        try (Connection competitor = dataSource.getConnection()) {
            // Competing employee: books 16:30–17:30 but has not committed yet, so the overlap check of the
            // booking under test does not see it
            competitor.setAutoCommit(false);
            try (var insert = competitor.prepareStatement(
                    "insert into booking (room_id, booked_by, starts_at, ends_at, purpose) values (?, ?, ?, ?, ?)")) {
                insert.setLong(1, studioId);
                insert.setString(2, COMPETITOR);
                insert.setTimestamp(3, Timestamp.valueOf(at(16, 30)));
                insert.setTimestamp(4, Timestamp.valueOf(at(17, 30)));
                insert.setString(5, "Review");
                insert.executeUpdate();
            }

            booking = CompletableFuture.supplyAsync(() -> bookingService.book(
                    new BookRoom(studioId, NAME, at(16, 0), at(17, 0), "Planning")));

            // The booking has passed its overlap check and waits at the exclusion constraint for the competitor
            awaitInsertBlockedOnLock(booking);
            competitor.commit();
        }

        // Step 7 / A3 step 1: the booking is rejected because the room is already booked for an overlapping
        // time slot
        try {
            Long id = booking.get(30, TimeUnit.SECONDS);
            fail("Booking stored an overlapping booking of the room, id " + id);
        }
        catch (ExecutionException ex) {
            assertThat(ex.getCause())
                    .isInstanceOf(RoomAlreadyBookedException.class)
                    .hasMessage(RoomAlreadyBookedException.MESSAGE);
        }

        // Failure postconditions: no booking stored by the rejected booking, the competitor's booking unchanged,
        // no two bookings of the room overlap
        List<Map<String, Object>> stored = jdbcClient.sql(
                        "select booked_by, starts_at, ends_at from booking where room_id = ? and booked_by like 'UC002%'")
                .param(studioId)
                .query((rs, n) -> Map.<String, Object>of(
                        "booked_by", rs.getString("booked_by"),
                        "starts_at", rs.getTimestamp("starts_at").toLocalDateTime(),
                        "ends_at", rs.getTimestamp("ends_at").toLocalDateTime()))
                .list();
        assertThat(stored).containsExactly(
                Map.of("booked_by", COMPETITOR, "starts_at", at(16, 30), "ends_at", at(17, 30)));
        assertThat(bookingService.findBookedTimeSlots(studioId))
                .filteredOn(slot -> slot.bookedBy().startsWith("UC002"))
                .extracting(slot -> slot.bookedBy())
                .containsExactly(COMPETITOR);
    }

    private static LocalDateTime at(int hour, int minute) {
        return DAY.withHour(hour).withMinute(minute);
    }

    private void awaitInsertBlockedOnLock(CompletableFuture<Long> booking) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < deadline) {
            if (booking.isDone()) {
                fail("Booking finished before the competitor committed: " + booking.handle((id, ex) ->
                        ex != null ? ex : id).join());
            }
            Integer waiting = jdbcClient.sql("""
                            select count(*) from pg_stat_activity
                            where datname = current_database() and wait_event_type = 'Lock'
                              and query ilike 'insert into%booking%'""")
                    .query(Integer.class).single();
            if (waiting > 0) {
                return;
            }
            Thread.sleep(50);
        }
        fail("Booking never reached the insert");
    }
}
