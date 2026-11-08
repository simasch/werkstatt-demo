package ai.unifiedprocess.roombooking.booking.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * UC-002 steps 2-3: the entries of the booking form.
 */
public record BookRoom(
        // UC-002 BR-003: Room is required.
        @NotNull(message = "Room is required.") Long roomId,
        // UC-002 BR-003: Booked by is required and at most 100 characters, trimmed first.
        @NotBlank(message = "Booked by is required.")
        @Size(max = 100, message = "Booked by must be at most 100 characters.") String bookedBy,
        // UC-002 BR-001: Start is required.
        @NotNull(message = "Start is required.") LocalDateTime startsAt,
        // UC-002 BR-001: End is required.
        @NotNull(message = "End is required.") LocalDateTime endsAt,
        // UC-002 BR-003: Purpose is optional and at most 200 characters.
        @Size(max = 200, message = "Purpose must be at most 200 characters.") String purpose) {

    public BookRoom {
        bookedBy = bookedBy == null ? null : bookedBy.strip();
    }
}
