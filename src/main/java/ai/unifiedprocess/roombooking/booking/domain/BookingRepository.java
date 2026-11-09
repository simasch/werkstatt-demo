package ai.unifiedprocess.roombooking.booking.domain;

import static ai.unifiedprocess.roombooking.database.Tables.BOOKING;

import java.time.LocalDateTime;
import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

/** Reads and writes the bookings of the rooms. */
@Repository
public class BookingRepository {

    private final DSLContext ctx;

    public BookingRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    /** The booked time slots of a room, ordered by start time. */
    public List<BookedTimeSlot> findByRoom(long roomId) {
        return ctx.select(BOOKING.ID, BOOKING.BOOKED_BY, BOOKING.STARTS_AT, BOOKING.ENDS_AT, BOOKING.PURPOSE)
                .from(BOOKING)
                .where(BOOKING.ROOM_ID.eq(roomId))
                .orderBy(BOOKING.STARTS_AT, BOOKING.ID)
                .fetch(Records.mapping(BookedTimeSlot::new));
    }

    /**
     * Whether another booking of the room overlaps the time span from {@code startsAt}
     * (inclusive) to {@code endsAt} (exclusive).
     */
    public boolean existsOverlapping(long roomId, LocalDateTime startsAt, LocalDateTime endsAt) {
        return ctx.fetchExists(BOOKING, BOOKING.ROOM_ID.eq(roomId)
                // UC-002 BR-002: Spans [start, end) intersect; touching bookings do not overlap.
                .and(BOOKING.STARTS_AT.lt(endsAt))
                .and(BOOKING.ENDS_AT.gt(startsAt)));
    }

    /**
     * Stores a new booking and returns its id. Throws a {@code DataIntegrityViolationException}
     * when another booking of the room overlaps it (exclusion constraint
     * {@code booking_no_overlap_ex}).
     */
    public long insert(NewBooking booking) {
        return ctx.insertInto(BOOKING)
                .set(BOOKING.ROOM_ID, booking.roomId())
                .set(BOOKING.BOOKED_BY, booking.bookedBy())
                .set(BOOKING.STARTS_AT, booking.startsAt())
                .set(BOOKING.ENDS_AT, booking.endsAt())
                .set(BOOKING.PURPOSE, booking.purpose())
                .returning(BOOKING.ID)
                .fetchSingle(BOOKING.ID);
    }
}
