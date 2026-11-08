package ai.unifiedprocess.roombooking.booking.domain;

import java.time.LocalDateTime;

/**
 * A time slot for which an employee has booked a room.
 */
public record Booking(Long id, Long roomId, String bookedBy, LocalDateTime startsAt, LocalDateTime endsAt,
        String purpose) {
}
