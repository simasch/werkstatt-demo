package ai.unifiedprocess.roombooking.room.service;

/** UC-001 A1: another room already has the name (UC-001 BR-002). */
public class RoomNameAlreadyUsedException extends BusinessRuleException {

    public RoomNameAlreadyUsedException(String name) {
        super("A room named " + name + " already exists.");
    }
}
