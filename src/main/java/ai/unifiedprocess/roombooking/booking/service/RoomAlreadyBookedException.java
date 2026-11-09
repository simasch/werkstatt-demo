package ai.unifiedprocess.roombooking.booking.service;

import ai.unifiedprocess.roombooking.core.service.BusinessRuleException;

/** UC-002 A3 (BR-002): another booking of the room overlaps the entered time slot. */
public class RoomAlreadyBookedException extends BusinessRuleException {

    public static final String MESSAGE = "The room is already booked for an overlapping time slot.";

    public RoomAlreadyBookedException() {
        super(MESSAGE);
    }
}
