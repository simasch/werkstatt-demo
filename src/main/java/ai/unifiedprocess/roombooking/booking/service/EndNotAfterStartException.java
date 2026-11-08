package ai.unifiedprocess.roombooking.booking.service;

/**
 * UC-002 A2: the end of the time slot is not after its start.
 */
public class EndNotAfterStartException extends RuntimeException {

    public EndNotAfterStartException() {
        super("End must be after start.");
    }
}
