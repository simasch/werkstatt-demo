package ai.unifiedprocess.roombooking.booking.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import ai.unifiedprocess.roombooking.core.service.BusinessRuleException;

/** UC-002 A1: the time slot overlaps another booking of the same room. */
public class RoomAlreadyBookedException extends BusinessRuleException {

    // UC-002 BR-004: dates in messages are shown as yyyy-MM-dd HH:mm.
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public RoomAlreadyBookedException(LocalDateTime startsAt, LocalDateTime endsAt) {
        super("The room is already booked from " + startsAt.format(DATE_TIME_FORMAT) + " to "
                + endsAt.format(DATE_TIME_FORMAT) + ".");
    }
}
