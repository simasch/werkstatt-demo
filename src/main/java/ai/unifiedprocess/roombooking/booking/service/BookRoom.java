package ai.unifiedprocess.roombooking.booking.service;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * UC-002 step 3: the entries of the booking form.
 *
 * @param roomId the id of the selected room
 * @param bookedBy the name of the employee; leading and trailing spaces are removed
 * @param startsAt the start of the time slot
 * @param endsAt the end of the time slot
 * @param purpose optional text; an empty text means no purpose
 */
public record BookRoom(
        // UC-002 BR-003: The room is required.
        @NotNull(message = BookRoom.ROOM_REQUIRED) Long roomId,
        // UC-002 BR-003: Booked by is required and at most 100 characters long.
        @NotBlank(message = BookRoom.BOOKED_BY_REQUIRED)
        @Size(max = 100, message = BookRoom.BOOKED_BY_TOO_LONG) String bookedBy,
        // UC-002 BR-001: The start is required.
        @NotNull(message = BookRoom.START_REQUIRED) LocalDateTime startsAt,
        // UC-002 BR-001: The end is required.
        @NotNull(message = BookRoom.END_REQUIRED) LocalDateTime endsAt,
        // UC-002 BR-003: The purpose is optional and at most 200 characters long.
        @Size(max = 200, message = BookRoom.PURPOSE_TOO_LONG) String purpose) {

    public static final String ROOM_REQUIRED = "Room is required.";

    public static final String BOOKED_BY_REQUIRED = "Booked by is required.";

    public static final String BOOKED_BY_TOO_LONG = "Booked by must be at most 100 characters.";

    public static final String START_REQUIRED = "Start is required.";

    public static final String END_REQUIRED = "End is required.";

    public static final String END_NOT_AFTER_START = "End must be after start.";

    public static final String PURPOSE_TOO_LONG = "Purpose must be at most 200 characters.";

    // UC-002 BR-003: Spaces at both ends of Booked by are removed before it is checked.
    public BookRoom {
        bookedBy = bookedBy == null ? null : bookedBy.strip();
        purpose = purpose == null || purpose.isEmpty() ? null : purpose;
    }
}
