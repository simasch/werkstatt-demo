package ai.unifiedprocess.roombooking.booking.domain;

import java.time.LocalDateTime;

/** The entries of a booking to store for a room. */
public record NewBooking(long roomId, String bookedBy, LocalDateTime startsAt, LocalDateTime endsAt,
        String purpose) {
}
