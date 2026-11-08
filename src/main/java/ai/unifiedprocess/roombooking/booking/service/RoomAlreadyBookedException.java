package ai.unifiedprocess.roombooking.booking.service;

import ai.unifiedprocess.roombooking.booking.domain.Booking;
import ai.unifiedprocess.roombooking.room.service.BusinessRuleException;

/** UC-002 A1: the time slot overlaps another booking of the same room (UC-002 BR-002). */
public class RoomAlreadyBookedException extends BusinessRuleException {

    private final transient Booking overlapping;

    /**
     * @param overlapping the overlapping booking that starts first
     */
    public RoomAlreadyBookedException(Booking overlapping) {
        super("The room is already booked from " + BookingDateFormat.format(overlapping.startsAt()) + " to "
                + BookingDateFormat.format(overlapping.endsAt()) + ".");
        this.overlapping = overlapping;
    }

    /** The overlapping booking that starts first. */
    public Booking overlapping() {
        return overlapping;
    }
}
