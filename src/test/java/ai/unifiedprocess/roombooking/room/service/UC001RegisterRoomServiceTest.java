package ai.unifiedprocess.roombooking.room.service;

import ai.unifiedprocess.roombooking.TestcontainersConfiguration;
import ai.unifiedprocess.roombooking.usecase.UseCase;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Service tests for UC-001 "Register Room": only the units vaadin-flow-ui-test reported it cannot reach. The
 * Name and Equipment fields of RoomsView carry setMaxLength(50) / setMaxLength(200), so a real actor can never
 * type past those limits — BR-001's and BR-004's rejected length cases are exercised here, directly against
 * RegisterRoom's Jakarta Bean Validation constraints, instead of through the view. The rest of UC-001 is tested
 * in UC001RegisterRoomTest (UI layer, this use case's integration layer); this class adds nothing else.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UC001RegisterRoomServiceTest {

    @Autowired
    private RoomService service;

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-001"})
    void name_longer_than_50_characters_is_rejected() {
        RegisterRoom command = new RegisterRoom("a".repeat(51), 10, null);

        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> service.register(command))
                .satisfies(e -> assertThat(e.getConstraintViolations())
                        .extracting(ConstraintViolation::getMessage)
                        .containsExactly("Name must be at most 50 characters."));
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-004"})
    void equipment_longer_than_200_characters_is_rejected() {
        RegisterRoom command = new RegisterRoom("Overflow Room", 10, "e".repeat(201));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> service.register(command))
                .satisfies(e -> assertThat(e.getConstraintViolations())
                        .extracting(ConstraintViolation::getMessage)
                        .containsExactly("Equipment must be at most 200 characters."));
    }
}
