package ai.unifiedprocess.roombooking.booking.service;

import ai.unifiedprocess.roombooking.core.service.BusinessRuleException;

/** UC-002 A2: the end of the time slot is not after the start. */
public class EndNotAfterStartException extends BusinessRuleException {

    public EndNotAfterStartException() {
        super("End must be after start.");
    }
}
