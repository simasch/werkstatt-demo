package ai.unifiedprocess.roombooking.room.domain;

/**
 * A meeting room of the office that employees can book.
 */
public record Room(Long id, String name, int capacity, String equipment) {
}
