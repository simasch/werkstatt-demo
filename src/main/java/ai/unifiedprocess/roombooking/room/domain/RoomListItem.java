package ai.unifiedprocess.roombooking.room.domain;

/** A row of the room overview: a registered room with its name, capacity, and equipment. */
public record RoomListItem(long id, String name, int capacity, String equipment) {
}
