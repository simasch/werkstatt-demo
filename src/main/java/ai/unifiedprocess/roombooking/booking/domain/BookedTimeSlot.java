package ai.unifiedprocess.roombooking.booking.domain;

import java.time.LocalDateTime;

/** A booked time slot of a room: who booked it, from when to when, and for what. */
public record BookedTimeSlot(long id, String bookedBy, LocalDateTime startsAt, LocalDateTime endsAt,
        String purpose) {
}
