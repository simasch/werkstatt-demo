package ai.unifiedprocess.roombooking.room.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * UC-001 steps 5-7: the entries of the room form.
 */
public record RegisterRoom(
        // UC-001 BR-001: Name is required and at most 50 characters, trimmed before checking.
        @NotBlank(message = "Name is required.")
        @Size(max = 50, message = "Name must be at most 50 characters.") String name,
        // UC-001 BR-003: Capacity is required and a whole number from 1 to 50.
        @NotNull(message = "Capacity must be between 1 and 50.")
        @Min(value = 1, message = "Capacity must be between 1 and 50.")
        @Max(value = 50, message = "Capacity must be between 1 and 50.") Integer capacity,
        // UC-001 BR-004: Equipment is optional and at most 200 characters.
        @Size(max = 200, message = "Equipment must be at most 200 characters.") String equipment) {

    public RegisterRoom {
        name = name == null ? null : name.strip();
    }
}
