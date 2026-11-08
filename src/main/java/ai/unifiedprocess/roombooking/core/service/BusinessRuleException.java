package ai.unifiedprocess.roombooking.core.service;

/** A well-formed command that a business rule does not allow; the message is the text shown to the actor. */
public abstract class BusinessRuleException extends RuntimeException {

    protected BusinessRuleException(String message) {
        super(message);
    }
}
