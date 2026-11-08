package ai.unifiedprocess.roombooking.booking.domain;

import java.time.LocalDateTime;

public record Booking(Long id, Long roomId, String bookedBy, LocalDateTime startsAt, LocalDateTime endsAt,
        String purpose) {
}
