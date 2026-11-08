package ai.unifiedprocess.roombooking.room.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * UC-001 step 5: the entries of the room form.
 *
 * @param name the name of the room; leading and trailing spaces are removed
 * @param capacity the number of people the room seats
 * @param equipment optional free text; an empty text means no equipment
 */
public record RegisterRoom(
        // UC-001 BR-001: The name is required and at most 50 characters long.
        @NotBlank(message = RegisterRoom.NAME_REQUIRED)
        @Size(max = 50, message = RegisterRoom.NAME_TOO_LONG) String name,
        // UC-001 BR-003: The capacity is required and a whole number from 1 to 50.
        @NotNull(message = RegisterRoom.CAPACITY_RANGE) @Min(value = 1, message = RegisterRoom.CAPACITY_RANGE)
        @Max(value = 50, message = RegisterRoom.CAPACITY_RANGE) Integer capacity,
        // UC-001 BR-004: The equipment is optional and at most 200 characters long.
        @Size(max = 200, message = RegisterRoom.EQUIPMENT_TOO_LONG) String equipment) {

    public static final String NAME_REQUIRED = "Name is required.";

    public static final String NAME_TOO_LONG = "Name must be at most 50 characters.";

    public static final String CAPACITY_RANGE = "Capacity must be between 1 and 50.";

    public static final String EQUIPMENT_TOO_LONG = "Equipment must be at most 200 characters.";

    // UC-001 BR-001: Leading and trailing spaces are removed before the name is checked.
    public RegisterRoom {
        name = name == null ? null : name.strip();
        equipment = equipment == null || equipment.isEmpty() ? null : equipment;
    }
}
