package ai.unifiedprocess.roombooking.booking.service;

import ai.unifiedprocess.roombooking.room.service.BusinessRuleException;

/** UC-002 A2: the end of the time slot is not after its start (UC-002 BR-001). */
public class EndNotAfterStartException extends BusinessRuleException {

    public EndNotAfterStartException() {
        super(BookRoom.END_NOT_AFTER_START);
    }
}
