package ai.unifiedprocess.roombooking.room.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The entries of the room form (UC-001 step 5). The name is stripped of leading and trailing
 * spaces, and blank equipment is no equipment, before any rule is checked.
 */
public record RegisterRoom(
        // UC-001 BR-003: The name is required; a name of spaces only counts as missing.
        @NotBlank(message = RegisterRoom.NAME_REQUIRED)
        // UC-001 BR-005: The name has at most 50 characters.
        @Size(max = 50, message = RegisterRoom.NAME_TOO_LONG) String name,
        // UC-001 BR-003: The capacity is required.
        @NotNull(message = RegisterRoom.CAPACITY_REQUIRED)
        // UC-001 BR-004: The capacity is a whole number of people from 1 to 50.
        @Min(value = 1, message = RegisterRoom.CAPACITY_OUT_OF_RANGE)
        @Max(value = 50, message = RegisterRoom.CAPACITY_OUT_OF_RANGE) Integer capacity,
        // UC-001 BR-005: The equipment has at most 200 characters.
        @Size(max = 200, message = RegisterRoom.EQUIPMENT_TOO_LONG) String equipment) {

    public static final String NAME_REQUIRED = "Name is required.";
    public static final String NAME_TOO_LONG = "Name has at most 50 characters.";
    public static final String CAPACITY_REQUIRED = "Capacity is required.";
    public static final String CAPACITY_OUT_OF_RANGE = "Capacity must be a whole number from 1 to 50.";
    public static final String EQUIPMENT_TOO_LONG = "Equipment has at most 200 characters.";

    public RegisterRoom {
        // UC-001 BR-002: The room name is stored without leading or trailing spaces.
        name = name == null ? null : name.strip();
        equipment = equipment == null || equipment.isBlank() ? null : equipment;
    }
}
