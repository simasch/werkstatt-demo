package ai.unifiedprocess.roombooking.booking.service;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The entries of the booking form for the selected room (UC-002 step 5). The name is kept as
 * entered; an empty purpose field is no purpose.
 */
public record BookRoom(
        // UC-002 BR-003: The room is required.
        @NotNull(message = BookRoom.ROOM_REQUIRED) Long roomId,
        // UC-002 BR-003: The employee's name is required; spaces only count as missing.
        @NotBlank(message = BookRoom.NAME_REQUIRED)
        // UC-002 BR-004: The employee's name has at most 100 characters.
        @Size(max = 100, message = BookRoom.NAME_TOO_LONG) String bookedBy,
        // UC-002 BR-003: The start time is required.
        @NotNull(message = BookRoom.START_REQUIRED) LocalDateTime startsAt,
        // UC-002 BR-003: The end time is required.
        @NotNull(message = BookRoom.END_REQUIRED) LocalDateTime endsAt,
        // UC-002 BR-004: The purpose has at most 200 characters.
        @Size(max = 200, message = BookRoom.PURPOSE_TOO_LONG) String purpose) {

    public static final String ROOM_REQUIRED = "Room is required.";
    public static final String NAME_REQUIRED = "Name is required.";
    public static final String NAME_TOO_LONG = "Name has at most 100 characters.";
    public static final String START_REQUIRED = "Start time is required.";
    public static final String END_REQUIRED = "End time is required.";
    public static final String PURPOSE_TOO_LONG = "Purpose has at most 200 characters.";

    public BookRoom {
        purpose = purpose == null || purpose.isEmpty() ? null : purpose;
    }
}
