package ai.unifiedprocess.roombooking.room.domain;

/** The entries of a room to register; the name is already trimmed. */
public record NewRoom(String name, int capacity, String equipment) {
}
