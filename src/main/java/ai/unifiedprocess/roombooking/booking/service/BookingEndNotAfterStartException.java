package ai.unifiedprocess.roombooking.booking.service;

import ai.unifiedprocess.roombooking.core.service.BusinessRuleException;

/** UC-002 A2 (BR-001): the entered end time is not after the start time. */
public class BookingEndNotAfterStartException extends BusinessRuleException {

    public static final String MESSAGE = "End time must be after the start time.";

    public BookingEndNotAfterStartException() {
        super(MESSAGE);
    }
}
