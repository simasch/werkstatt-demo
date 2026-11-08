package ai.unifiedprocess.roombooking.booking.domain;

import static ai.unifiedprocess.roombooking.database.Tables.BOOKING;

import java.time.LocalDateTime;
import java.util.Optional;

import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

/** Data access for the {@link Booking} aggregate. */
@Repository
public class BookingRepository {

    private final DSLContext ctx;

    public BookingRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    /**
     * The booking of the room that overlaps the time slot and starts first, empty when none overlaps.
     */
    public Optional<Booking> findFirstOverlapping(long roomId, LocalDateTime startsAt, LocalDateTime endsAt) {
        return ctx
                .select(BOOKING.ID, BOOKING.ROOM_ID, BOOKING.BOOKED_BY, BOOKING.STARTS_AT, BOOKING.ENDS_AT,
                        BOOKING.PURPOSE)
                .from(BOOKING)
                .where(BOOKING.ROOM_ID.eq(roomId))
                // UC-002 BR-002: Overlap; a booking ending as the other starts does not.
                .and(BOOKING.STARTS_AT.lt(endsAt))
                .and(BOOKING.ENDS_AT.gt(startsAt))
                .orderBy(BOOKING.STARTS_AT, BOOKING.ID)
                .limit(1)
                .fetchOptional(Records.mapping(Booking::new));
    }

    /** Stores a new booking and returns its generated id. */
    public long insert(Booking booking) {
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
