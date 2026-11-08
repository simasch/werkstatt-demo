package ai.unifiedprocess.roombooking.room.domain;

/**
 * A meeting room of the office that employees can book.
 *
 * @param id the unique identifier, {@code null} before the room is stored
 * @param name the name of the room as shown at the door
 * @param capacity the number of people the room seats
 * @param equipment free text such as projector or whiteboard, {@code null} when none is given
 */
public record Room(Long id, String name, int capacity, String equipment) {
}
