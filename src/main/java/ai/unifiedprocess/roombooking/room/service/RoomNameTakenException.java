package ai.unifiedprocess.roombooking.room.service;

import ai.unifiedprocess.roombooking.core.service.BusinessRuleException;

/** UC-001 A3: another room already has the entered name. */
public class RoomNameTakenException extends BusinessRuleException {

    public static final String MESSAGE = "Another room already has this name.";

    public RoomNameTakenException() {
        super(MESSAGE);
    }
}
