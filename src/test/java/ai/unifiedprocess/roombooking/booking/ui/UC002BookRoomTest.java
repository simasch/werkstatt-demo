package ai.unifiedprocess.roombooking.booking.ui;

import ai.unifiedprocess.roombooking.TestcontainersConfiguration;
import ai.unifiedprocess.roombooking.usecase.UseCase;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-002 "Book Room". The view books through BookingService/BookingRepository against the room
 * seeded by R__uc001_rooms.sql ("Atrium", "Boardroom") and the booking seeded by R__uc002_bookings.sql (an
 * "Existing Booker" booking of "Atrium" from 2030-06-01 09:00 to 10:00, used by the overlap tests); the test drives
 * the view only, never the service or the repository.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UC002BookRoomTest extends SpringBrowserlessTest {

    // Every booked-by name a test may create or attempt to create; never the name of the shared test data
    private static final List<String> CREATED_NAMES = List.of(
            "Alice Success", "Bob NoPurpose", "Carol Trim", "Dave NoStart", "Erin NoEnd", "Frank EndBeforeStart",
            "Grace NoRoom", "Ivy Adjacent", "Henry Overlap");

    @Autowired
    private JdbcClient jdbcClient;

    @AfterEach
    void removeCreatedBookings() {
        for (String bookedBy : CREATED_NAMES) {
            jdbcClient.sql("delete from booking where booked_by = ?").param(bookedBy).update();
        }
    }

    @Test
    @UseCase(id = "UC-002")
    void booking_a_room_shows_the_message_and_stores_the_booking() {
        navigate(BookRoomView.class);

        test(find(ComboBox.class).withCaption("Room").single()).selectItem("Atrium");
        test(find(TextField.class).withCaption("Booked by").single()).setValue("Alice Success");
        test(find(DateTimePicker.class).withCaption("Start").single())
                .setValue(LocalDateTime.of(2030, 1, 10, 10, 0));
        test(find(DateTimePicker.class).withCaption("End").single())
                .setValue(LocalDateTime.of(2030, 1, 10, 11, 0));
        test(find(TextField.class).withCaption("Purpose").single()).setValue("Planning");
        test(find(Button.class).withText("Book").single()).click();

        // Step 5: the exact message the specification states, dates formatted per BR-004
        Notification notification = find(Notification.class).single();
        assertThat(test(notification).getText()).isEqualTo("Room Atrium booked from 2030-01-10 10:00 to 2030-01-10 11:00.");
        // Success postcondition: the booking is stored for the room and the time slot
        assertThat(jdbcClient.sql("""
                        select r.name, b.starts_at, b.ends_at, b.purpose from booking b
                        join room r on r.id = b.room_id where b.booked_by = ?""")
                .param("Alice Success")
                .query((rs, rowNum) -> rs.getString(1) + "/" + rs.getObject(2, LocalDateTime.class) + "/"
                        + rs.getObject(3, LocalDateTime.class) + "/" + rs.getString(4))
                .list()).containsExactly("Atrium/2030-01-10T10:00/2030-01-10T11:00/Planning");
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-003"})
    void booking_without_purpose_succeeds() {
        navigate(BookRoomView.class);

        test(find(ComboBox.class).withCaption("Room").single()).selectItem("Boardroom");
        test(find(TextField.class).withCaption("Booked by").single()).setValue("Bob NoPurpose");
        test(find(DateTimePicker.class).withCaption("Start").single())
                .setValue(LocalDateTime.of(2030, 1, 11, 9, 0));
        test(find(DateTimePicker.class).withCaption("End").single())
                .setValue(LocalDateTime.of(2030, 1, 11, 9, 30));
        test(find(Button.class).withText("Book").single()).click();

        // BR-003: purpose is optional
        Notification notification = find(Notification.class).single();
        assertThat(test(notification).getText()).isEqualTo("Room Boardroom booked from 2030-01-11 09:00 to 2030-01-11 09:30.");
        assertThat(jdbcClient.sql("select purpose from booking where booked_by = ?")
                .param("Bob NoPurpose").query(String.class).list()).containsExactly("");
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-003"})
    void booked_by_is_trimmed_before_it_is_stored() {
        navigate(BookRoomView.class);

        test(find(ComboBox.class).withCaption("Room").single()).selectItem("Boardroom");
        test(find(TextField.class).withCaption("Booked by").single()).setValue("  Carol Trim  ");
        test(find(DateTimePicker.class).withCaption("Start").single())
                .setValue(LocalDateTime.of(2030, 1, 12, 9, 0));
        test(find(DateTimePicker.class).withCaption("End").single())
                .setValue(LocalDateTime.of(2030, 1, 12, 9, 30));
        test(find(Button.class).withText("Book").single()).click();

        // BR-003: leading and trailing spaces are removed before "Booked by" is checked and stored
        assertThat(find(Notification.class).exists()).isTrue();
        assertThat(jdbcClient.sql("select booked_by from booking where booked_by = ?")
                .param("Carol Trim").query(String.class).list()).containsExactly("Carol Trim");
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A1: Room Already Booked", businessRules = {"BR-002"}, requirements = {"NFR-001"})
    void room_already_booked_is_rejected_and_keeps_the_entries() {
        navigate(BookRoomView.class);

        ComboBox<?> roomField = find(ComboBox.class).withCaption("Room").single();
        test(roomField).selectItem("Atrium");
        TextField bookedByField = find(TextField.class).withCaption("Booked by").single();
        test(bookedByField).setValue("Henry Overlap");
        test(find(DateTimePicker.class).withCaption("Start").single())
                .setValue(LocalDateTime.of(2030, 6, 1, 9, 30));
        test(find(DateTimePicker.class).withCaption("End").single())
                .setValue(LocalDateTime.of(2030, 6, 1, 10, 30));
        test(find(Button.class).withText("Book").single()).click();

        // A1: the overlapping booking's own start and end, formatted per BR-004
        Notification notification = find(Notification.class).single();
        assertThat(test(notification).getText())
                .isEqualTo("The room is already booked from 2030-06-01 09:00 to 2030-06-01 10:00.");
        // NFR-001: every entry of the form is kept
        assertThat(test(roomField).getSelected()).isNotNull();
        assertThat(bookedByField.getValue()).isEqualTo("Henry Overlap");
        // Failure postcondition: no booking is stored
        assertThat(jdbcClient.sql("select count(*) from booking where booked_by = ?")
                .param("Henry Overlap").query(Long.class).single()).isZero();
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-002"})
    void booking_that_starts_when_another_ends_does_not_overlap() {
        navigate(BookRoomView.class);

        test(find(ComboBox.class).withCaption("Room").single()).selectItem("Atrium");
        test(find(TextField.class).withCaption("Booked by").single()).setValue("Ivy Adjacent");
        test(find(DateTimePicker.class).withCaption("Start").single())
                .setValue(LocalDateTime.of(2030, 6, 1, 10, 0));
        test(find(DateTimePicker.class).withCaption("End").single())
                .setValue(LocalDateTime.of(2030, 6, 1, 11, 0));
        test(find(Button.class).withText("Book").single()).click();

        // BR-002: a booking that ends at the moment another one starts does not overlap it
        Notification notification = find(Notification.class).single();
        assertThat(test(notification).getText())
                .isEqualTo("Room Atrium booked from 2030-06-01 10:00 to 2030-06-01 11:00.");
        assertThat(jdbcClient.sql("select count(*) from booking where booked_by = ?")
                .param("Ivy Adjacent").query(Long.class).single()).isEqualTo(1);
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-001"}, requirements = {"NFR-001"})
    void missing_start_is_rejected_and_keeps_the_other_entries() {
        navigate(BookRoomView.class);

        test(find(ComboBox.class).withCaption("Room").single()).selectItem("Atrium");
        TextField bookedByField = find(TextField.class).withCaption("Booked by").single();
        test(bookedByField).setValue("Dave NoStart");
        test(find(DateTimePicker.class).withCaption("End").single())
                .setValue(LocalDateTime.of(2030, 1, 13, 10, 0));
        test(find(Button.class).withText("Book").single()).click();

        // BR-001: a missing start is rejected with this exact message at "Start"
        DateTimePicker startField = find(DateTimePicker.class).withCaption("Start").single();
        assertThat(startField.isInvalid()).isTrue();
        assertThat(startField.getErrorMessage()).isEqualTo("Start is required.");
        // NFR-001: every other entry of the form is kept
        assertThat(bookedByField.getValue()).isEqualTo("Dave NoStart");
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-001"})
    void missing_end_is_rejected() {
        navigate(BookRoomView.class);

        test(find(ComboBox.class).withCaption("Room").single()).selectItem("Atrium");
        test(find(TextField.class).withCaption("Booked by").single()).setValue("Erin NoEnd");
        test(find(DateTimePicker.class).withCaption("Start").single())
                .setValue(LocalDateTime.of(2030, 1, 14, 9, 0));
        test(find(Button.class).withText("Book").single()).click();

        // BR-001: a missing end is rejected with this exact message at "End"
        DateTimePicker endField = find(DateTimePicker.class).withCaption("End").single();
        assertThat(endField.isInvalid()).isTrue();
        assertThat(endField.getErrorMessage()).isEqualTo("End is required.");
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-001"})
    void end_not_after_start_is_rejected() {
        navigate(BookRoomView.class);

        test(find(ComboBox.class).withCaption("Room").single()).selectItem("Atrium");
        test(find(TextField.class).withCaption("Booked by").single()).setValue("Frank EndBeforeStart");
        test(find(DateTimePicker.class).withCaption("Start").single())
                .setValue(LocalDateTime.of(2030, 1, 15, 9, 0));
        test(find(DateTimePicker.class).withCaption("End").single())
                .setValue(LocalDateTime.of(2030, 1, 15, 9, 0));
        test(find(Button.class).withText("Book").single()).click();

        // BR-001: the end is not after the start, rejected with this exact message at "End"
        DateTimePicker endField = find(DateTimePicker.class).withCaption("End").single();
        assertThat(endField.isInvalid()).isTrue();
        assertThat(endField.getErrorMessage()).isEqualTo("End must be after start.");
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-003"})
    void missing_room_is_rejected() {
        navigate(BookRoomView.class);

        test(find(TextField.class).withCaption("Booked by").single()).setValue("Grace NoRoom");
        test(find(DateTimePicker.class).withCaption("Start").single())
                .setValue(LocalDateTime.of(2030, 1, 16, 9, 0));
        test(find(DateTimePicker.class).withCaption("End").single())
                .setValue(LocalDateTime.of(2030, 1, 16, 10, 0));
        test(find(Button.class).withText("Book").single()).click();

        // BR-003: a missing room is rejected with this exact message
        ComboBox<?> roomField = find(ComboBox.class).withCaption("Room").single();
        assertThat(roomField.isInvalid()).isTrue();
        assertThat(roomField.getErrorMessage()).isEqualTo("Room is required.");
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-003"})
    void empty_booked_by_is_rejected() {
        navigate(BookRoomView.class);

        test(find(ComboBox.class).withCaption("Room").single()).selectItem("Atrium");
        test(find(DateTimePicker.class).withCaption("Start").single())
                .setValue(LocalDateTime.of(2030, 1, 17, 9, 0));
        test(find(DateTimePicker.class).withCaption("End").single())
                .setValue(LocalDateTime.of(2030, 1, 17, 10, 0));
        test(find(Button.class).withText("Book").single()).click();

        // BR-003: an empty "Booked by" is rejected with this exact message
        TextField bookedByField = find(TextField.class).withCaption("Booked by").single();
        assertThat(bookedByField.isInvalid()).isTrue();
        assertThat(bookedByField.getErrorMessage()).isEqualTo("Booked by is required.");
    }
}
