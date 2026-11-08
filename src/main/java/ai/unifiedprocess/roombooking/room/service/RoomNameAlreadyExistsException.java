package ai.unifiedprocess.roombooking.room.service;

/**
 * UC-001 A1: another room already uses the chosen name.
 */
public class RoomNameAlreadyExistsException extends RuntimeException {

    public RoomNameAlreadyExistsException(String name) {
        super("A room named " + name + " already exists.");
    }
}
