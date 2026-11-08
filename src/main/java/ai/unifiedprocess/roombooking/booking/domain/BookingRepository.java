package ai.unifiedprocess.roombooking.booking.domain;

import static ai.unifiedprocess.roombooking.database.tables.Booking.BOOKING;

import java.time.LocalDateTime;
import java.util.Optional;

import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

@Repository
public class BookingRepository {

    private final DSLContext ctx;

    public BookingRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    // UC-002 BR-002: two bookings of the same room never overlap; one that ends when another starts does not
    // overlap it.
    public Optional<Booking> findOverlapping(Long roomId, LocalDateTime startsAt, LocalDateTime endsAt) {
        return ctx.select(BOOKING.ID, BOOKING.ROOM_ID, BOOKING.BOOKED_BY, BOOKING.STARTS_AT, BOOKING.ENDS_AT,
                BOOKING.PURPOSE)
                .from(BOOKING)
                .where(BOOKING.ROOM_ID.eq(roomId))
                .and(BOOKING.STARTS_AT.lessThan(endsAt))
                .and(BOOKING.ENDS_AT.greaterThan(startsAt))
                .fetchOptional(Records.mapping(Booking::new));
    }

    public long insert(Booking booking) {
        return ctx.insertInto(BOOKING)
                .set(BOOKING.ROOM_ID, booking.roomId())
                .set(BOOKING.BOOKED_BY, booking.bookedBy())
                .set(BOOKING.STARTS_AT, booking.startsAt())
                .set(BOOKING.ENDS_AT, booking.endsAt())
                .set(BOOKING.PURPOSE, booking.purpose())
                .returning(BOOKING.ID)
                .fetchOne(BOOKING.ID);
    }
}
