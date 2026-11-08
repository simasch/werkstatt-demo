package ai.unifiedprocess.roombooking.room.service;

/** A request that is well-formed but breaks a business rule; the message is the specification's text. */
public abstract class BusinessRuleException extends RuntimeException {

    protected BusinessRuleException(String message) {
        super(message);
    }
}
