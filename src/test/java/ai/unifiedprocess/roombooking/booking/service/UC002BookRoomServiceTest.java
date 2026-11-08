package ai.unifiedprocess.roombooking.booking.service;

import ai.unifiedprocess.roombooking.TestcontainersConfiguration;
import ai.unifiedprocess.roombooking.usecase.UseCase;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Service tests for UC-002 "Book Room": only the units vaadin-flow-ui-test reported it cannot reach. The
 * Booked by and Purpose fields of BookRoomView carry setMaxLength(100) / setMaxLength(200), so a real actor can
 * never type past those limits — BR-003's rejected length cases are exercised here, directly against BookRoom's
 * Jakarta Bean Validation constraints, instead of through the view. The rest of UC-002 is tested in
 * UC002BookRoomTest (UI layer, this use case's integration layer); this class adds nothing else.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UC002BookRoomServiceTest {

    @Autowired
    private BookingService service;

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-003"})
    void booked_by_longer_than_100_characters_is_rejected() {
        BookRoom command = new BookRoom(1L, "a".repeat(101), LocalDateTime.of(2031, 1, 1, 9, 0),
                LocalDateTime.of(2031, 1, 1, 10, 0), null);

        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> service.book(command))
                .satisfies(e -> assertThat(e.getConstraintViolations())
                        .extracting(ConstraintViolation::getMessage)
                        .containsExactly("Booked by must be at most 100 characters."));
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-003"})
    void purpose_longer_than_200_characters_is_rejected() {
        BookRoom command = new BookRoom(1L, "Overflow Purpose", LocalDateTime.of(2031, 1, 2, 9, 0),
                LocalDateTime.of(2031, 1, 2, 10, 0), "p".repeat(201));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .isThrownBy(() -> service.book(command))
                .satisfies(e -> assertThat(e.getConstraintViolations())
                        .extracting(ConstraintViolation::getMessage)
                        .containsExactly("Purpose must be at most 200 characters."));
    }
}
