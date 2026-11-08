package ai.unifiedprocess.roombooking.room.service;

import ai.unifiedprocess.roombooking.core.service.BusinessRuleException;

/** UC-001 A1: another room already has the name. */
public class RoomNameAlreadyExistsException extends BusinessRuleException {

    public RoomNameAlreadyExistsException(String name) {
        super("A room named " + name + " already exists.");
    }
}
