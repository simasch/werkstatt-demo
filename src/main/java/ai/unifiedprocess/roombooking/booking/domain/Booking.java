package ai.unifiedprocess.roombooking.booking.domain;

import java.time.LocalDateTime;

/**
 * A time slot for which an employee has booked a room.
 *
 * @param id the unique identifier, {@code null} before the booking is stored
 * @param roomId the id of the booked room
 * @param bookedBy the name of the employee who booked
 * @param startsAt the start of the time slot
 * @param endsAt the end of the time slot
 * @param purpose what the meeting is about, {@code null} when none is given
 */
public record Booking(Long id, long roomId, String bookedBy, LocalDateTime startsAt, LocalDateTime endsAt,
        String purpose) {
}
