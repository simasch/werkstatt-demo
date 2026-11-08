package ai.unifiedprocess.roombooking.core.service;

/** A command is well-formed but rejected because it violates a business rule of the specification. */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
