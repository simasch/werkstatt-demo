package ai.unifiedprocess.roombooking.booking.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * UC-002 A1: the room is already booked for an overlapping time slot.
 */
public class RoomAlreadyBookedException extends RuntimeException {

    // UC-002 BR-004: Messages show a date and time as yyyy-MM-dd HH:mm.
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public RoomAlreadyBookedException(LocalDateTime startsAt, LocalDateTime endsAt) {
        super("The room is already booked from " + startsAt.format(FORMAT) + " to " + endsAt.format(FORMAT) + ".");
    }
}
