package ai.unifiedprocess.roombooking.booking.ui;

import java.time.LocalDateTime;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.textfield.TextField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import ai.unifiedprocess.roombooking.TestcontainersConfiguration;
import ai.unifiedprocess.roombooking.room.domain.Room;
import ai.unifiedprocess.roombooking.room.ui.RoomsView;
import ai.unifiedprocess.roombooking.usecase.UseCase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-002 "Book Room". The view gets its rooms from the service layer
 * (RoomService) and books through BookingService (real BookingRepository and PostgreSQL); the
 * test drives the view only and seeds data through the Flyway test migrations
 * R__uc001_register_room.sql ("Alpha Room", "Zulu Room") and R__uc002_book_room.sql (one
 * existing booking of "Alpha Room" from 2026-12-01 10:00 to 11:00), never through the service.
 * Messages come from the specification.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UC002BookRoomTest extends SpringBrowserlessTest {

    // The one "Booked by" name these tests create bookings with; cleaned up after every test
    private static final String NEW_BOOKED_BY = "Carol Miller";

    @Autowired
    private JdbcClient jdbcClient;

    @AfterEach
    void removeCreatedBookings() {
        // Idempotent and targeted: a test may have failed before or after creating the booking
        jdbcClient.sql("delete from booking where booked_by = ?").param(NEW_BOOKED_BY).update();
    }

    @Test
    @UseCase(id = "UC-002")
    void choosing_book_room_in_the_main_menu_opens_the_booking_form() {
        navigate(RoomsView.class);

        // Step 1: Employee chooses "Book Room" in the main menu — the @Menu(title = "Book Room")
        // entry on BookRoomView, rendered as a SideNavItem by MainLayout's SideNav
        test(find(SideNav.class).single()).clickItem("Book Room");

        assertThat(getCurrentView()).isInstanceOf(BookRoomView.class);
        // Step 2: the booking form with the fields "Room", "Booked by", "Start", "End", "Purpose"
        assertThat(find(ComboBox.class).id("booking-room-field").getValue()).isNull();
        assertThat(find(TextField.class).id("booking-booked-by-field").getValue()).isEmpty();
        assertThat(find(DateTimePicker.class).id("booking-start-field").getValue()).isNull();
        assertThat(find(DateTimePicker.class).id("booking-end-field").getValue()).isNull();
        assertThat(find(TextField.class).id("booking-purpose-field").getValue()).isEmpty();
    }

    @Test
    @UseCase(id = "UC-002")
    void booking_a_room_trims_the_name_stores_the_booking_and_shows_the_message() {
        navigate(BookRoomView.class);

        LocalDateTime start = LocalDateTime.of(2026, 12, 2, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 2, 10, 0);
        // BR-003: leading and trailing spaces are removed before "Booked by" is checked and stored
        fillForm("Zulu Room", "  " + NEW_BOOKED_BY + "  ", start, end, "Standup");
        test(find(Button.class).id("book-room-button")).click();

        // Main success scenario step 5: the exact message, with the room, start, and end
        // (BR-004: yyyy-MM-dd HH:mm)
        Notification notification = find(Notification.class).single();
        assertThat(test(notification).getText())
                .isEqualTo("Room Zulu Room booked from 2026-12-02 09:00 to 2026-12-02 10:00.");
        // Success postcondition: the booking is stored for the room and the time slot (trimmed name)
        assertThat(jdbcClient.sql("""
                select r.name, b.booked_by, b.starts_at, b.ends_at, b.purpose
                from booking b join room r on r.id = b.room_id
                where b.booked_by = ?
                """)
                .param(NEW_BOOKED_BY)
                .query((rs, rowNum) -> rs.getString("name") + "/" + rs.getString("booked_by") + "/"
                        + rs.getTimestamp("starts_at").toLocalDateTime() + "/"
                        + rs.getTimestamp("ends_at").toLocalDateTime() + "/" + rs.getString("purpose"))
                .single())
                .isEqualTo("Zulu Room/" + NEW_BOOKED_BY + "/" + start + "/" + end + "/Standup");
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-003"})
    void booking_a_room_without_a_purpose_is_allowed() {
        navigate(BookRoomView.class);

        LocalDateTime start = LocalDateTime.of(2026, 12, 2, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 2, 10, 0);
        fillForm("Zulu Room", NEW_BOOKED_BY, start, end, "");
        test(find(Button.class).id("book-room-button")).click();

        Notification notification = find(Notification.class).single();
        assertThat(test(notification).getText())
                .isEqualTo("Room Zulu Room booked from 2026-12-02 09:00 to 2026-12-02 10:00.");
        assertThat(jdbcClient.sql("select purpose from booking where booked_by = ?")
                .param(NEW_BOOKED_BY).query(String.class).single()).isEmpty();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A1: Room Already Booked", businessRules = {"BR-002"})
    void booking_a_room_with_an_overlapping_time_slot_is_rejected_and_keeps_entries() {
        navigate(BookRoomView.class);

        // BR-002: "Alpha Room" is already booked from 2026-12-01 10:00 to 11:00 by the test data;
        // this time slot overlaps it
        LocalDateTime start = LocalDateTime.of(2026, 12, 1, 10, 30);
        LocalDateTime end = LocalDateTime.of(2026, 12, 1, 10, 45);
        fillForm("Alpha Room", NEW_BOOKED_BY, start, end, "Review");
        test(find(Button.class).id("book-room-button")).click();

        // A1 step 1: the message, with the start and end of the overlapping booking (BR-004 format)
        Notification notification = find(Notification.class).single();
        assertThat(test(notification).getText())
                .isEqualTo("The room is already booked from 2026-12-01 10:00 to 2026-12-01 11:00.");
        // Failure postcondition: no booking is stored
        assertThat(jdbcClient.sql("select count(*) from booking where booked_by = ?")
                .param(NEW_BOOKED_BY).query(Long.class).single()).isZero();
        // A1: every entry of the form is kept
        ComboBox<Room> roomField = find(ComboBox.class).id("booking-room-field");
        assertThat(roomField.getValue().name()).isEqualTo("Alpha Room");
        assertThat(find(TextField.class).id("booking-booked-by-field").getValue()).isEqualTo(NEW_BOOKED_BY);
        assertThat(find(DateTimePicker.class).id("booking-start-field").getValue()).isEqualTo(start);
        assertThat(find(DateTimePicker.class).id("booking-end-field").getValue()).isEqualTo(end);
        assertThat(find(TextField.class).id("booking-purpose-field").getValue()).isEqualTo("Review");
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-002"})
    void booking_a_room_ending_when_another_booking_starts_does_not_overlap_it() {
        navigate(BookRoomView.class);

        // BR-002: ends exactly when the test data's booking of "Alpha Room" starts (10:00) — does
        // not overlap it
        LocalDateTime start = LocalDateTime.of(2026, 12, 1, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 1, 10, 0);
        fillForm("Alpha Room", NEW_BOOKED_BY, start, end, "");
        test(find(Button.class).id("book-room-button")).click();

        Notification notification = find(Notification.class).single();
        assertThat(test(notification).getText())
                .isEqualTo("Room Alpha Room booked from 2026-12-01 09:00 to 2026-12-01 10:00.");
        assertThat(jdbcClient.sql("select count(*) from booking where booked_by = ?")
                .param(NEW_BOOKED_BY).query(Long.class).single()).isEqualTo(1);
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-002"})
    void booking_a_room_starting_when_another_booking_ends_does_not_overlap_it() {
        navigate(BookRoomView.class);

        // BR-002: starts exactly when the test data's booking of "Alpha Room" ends (11:00) — does
        // not overlap it
        LocalDateTime start = LocalDateTime.of(2026, 12, 1, 11, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 1, 12, 0);
        fillForm("Alpha Room", NEW_BOOKED_BY, start, end, "");
        test(find(Button.class).id("book-room-button")).click();

        Notification notification = find(Notification.class).single();
        assertThat(test(notification).getText())
                .isEqualTo("Room Alpha Room booked from 2026-12-01 11:00 to 2026-12-01 12:00.");
        assertThat(jdbcClient.sql("select count(*) from booking where booked_by = ?")
                .param(NEW_BOOKED_BY).query(Long.class).single()).isEqualTo(1);
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-003"})
    void booking_without_a_room_is_rejected_and_keeps_entries() {
        navigate(BookRoomView.class);

        LocalDateTime start = LocalDateTime.of(2026, 12, 3, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 3, 10, 0);
        test(find(TextField.class).id("booking-booked-by-field")).setValue(NEW_BOOKED_BY);
        test(find(DateTimePicker.class).id("booking-start-field")).setValue(start);
        test(find(DateTimePicker.class).id("booking-end-field")).setValue(end);
        test(find(TextField.class).id("booking-purpose-field")).setValue("Review");
        test(find(Button.class).id("book-room-button")).click();

        ComboBox<Room> roomField = find(ComboBox.class).id("booking-room-field");
        assertThat(roomField.isInvalid()).isTrue();
        assertThat(roomField.getErrorMessage()).isEqualTo("Room is required.");
        assertThat(jdbcClient.sql("select count(*) from booking where booked_by = ?")
                .param(NEW_BOOKED_BY).query(Long.class).single()).isZero();
        // A2 / NFR-001: every other entry of the form is kept
        assertThat(find(TextField.class).id("booking-booked-by-field").getValue()).isEqualTo(NEW_BOOKED_BY);
        assertThat(find(DateTimePicker.class).id("booking-start-field").getValue()).isEqualTo(start);
        assertThat(find(DateTimePicker.class).id("booking-end-field").getValue()).isEqualTo(end);
        assertThat(find(TextField.class).id("booking-purpose-field").getValue()).isEqualTo("Review");
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-003"})
    void booking_with_an_empty_booked_by_is_rejected_and_keeps_entries() {
        navigate(BookRoomView.class);

        LocalDateTime start = LocalDateTime.of(2026, 12, 3, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 3, 10, 0);
        test(find(ComboBox.class).id("booking-room-field")).selectItem("Zulu Room");
        test(find(DateTimePicker.class).id("booking-start-field")).setValue(start);
        test(find(DateTimePicker.class).id("booking-end-field")).setValue(end);
        test(find(Button.class).id("book-room-button")).click();

        TextField bookedByField = find(TextField.class).id("booking-booked-by-field");
        assertThat(bookedByField.isInvalid()).isTrue();
        assertThat(bookedByField.getErrorMessage()).isEqualTo("Booked by is required.");
        ComboBox<Room> roomField = find(ComboBox.class).id("booking-room-field");
        assertThat(roomField.getValue().name()).isEqualTo("Zulu Room");
        assertThat(find(DateTimePicker.class).id("booking-start-field").getValue()).isEqualTo(start);
        assertThat(find(DateTimePicker.class).id("booking-end-field").getValue()).isEqualTo(end);
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-003"})
    void booking_with_a_booked_by_longer_than_100_characters_is_rejected() {
        navigate(BookRoomView.class);

        test(find(ComboBox.class).id("booking-room-field")).selectItem("Zulu Room");
        // A browser drops keystrokes past the field's maxLength (100 — the tester throws for this
        // reason): set the 101-character name through the component's Java API instead
        find(TextField.class).id("booking-booked-by-field").setValue("A".repeat(101));
        LocalDateTime start = LocalDateTime.of(2026, 12, 3, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 3, 10, 0);
        test(find(DateTimePicker.class).id("booking-start-field")).setValue(start);
        test(find(DateTimePicker.class).id("booking-end-field")).setValue(end);
        test(find(Button.class).id("book-room-button")).click();

        TextField bookedByField = find(TextField.class).id("booking-booked-by-field");
        assertThat(bookedByField.isInvalid()).isTrue();
        assertThat(bookedByField.getErrorMessage()).isEqualTo("Booked by must be at most 100 characters.");
        assertThat(jdbcClient.sql("select count(*) from booking where room_id = (select id from room where name = ?)")
                .param("Zulu Room").query(Long.class).single()).isZero();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-003"})
    void booking_with_a_purpose_longer_than_200_characters_is_rejected() {
        navigate(BookRoomView.class);

        test(find(ComboBox.class).id("booking-room-field")).selectItem("Zulu Room");
        test(find(TextField.class).id("booking-booked-by-field")).setValue(NEW_BOOKED_BY);
        LocalDateTime start = LocalDateTime.of(2026, 12, 3, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 3, 10, 0);
        test(find(DateTimePicker.class).id("booking-start-field")).setValue(start);
        test(find(DateTimePicker.class).id("booking-end-field")).setValue(end);
        // A browser drops keystrokes past the field's maxLength (200): set the 201-character
        // purpose through the component's Java API instead of the tester
        find(TextField.class).id("booking-purpose-field").setValue("P".repeat(201));
        test(find(Button.class).id("book-room-button")).click();

        TextField purposeField = find(TextField.class).id("booking-purpose-field");
        assertThat(purposeField.isInvalid()).isTrue();
        assertThat(purposeField.getErrorMessage()).isEqualTo("Purpose must be at most 200 characters.");
        assertThat(jdbcClient.sql("select count(*) from booking where booked_by = ?")
                .param(NEW_BOOKED_BY).query(Long.class).single()).isZero();
        assertThat(find(TextField.class).id("booking-booked-by-field").getValue()).isEqualTo(NEW_BOOKED_BY);
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-001"})
    void booking_with_a_missing_start_is_rejected_and_keeps_entries() {
        navigate(BookRoomView.class);

        test(find(ComboBox.class).id("booking-room-field")).selectItem("Zulu Room");
        test(find(TextField.class).id("booking-booked-by-field")).setValue(NEW_BOOKED_BY);
        LocalDateTime end = LocalDateTime.of(2026, 12, 3, 10, 0);
        test(find(DateTimePicker.class).id("booking-end-field")).setValue(end);
        test(find(Button.class).id("book-room-button")).click();

        DateTimePicker startField = find(DateTimePicker.class).id("booking-start-field");
        assertThat(startField.isInvalid()).isTrue();
        assertThat(startField.getErrorMessage()).isEqualTo("Start is required.");
        assertThat(jdbcClient.sql("select count(*) from booking where booked_by = ?")
                .param(NEW_BOOKED_BY).query(Long.class).single()).isZero();
        assertThat(find(TextField.class).id("booking-booked-by-field").getValue()).isEqualTo(NEW_BOOKED_BY);
        assertThat(find(DateTimePicker.class).id("booking-end-field").getValue()).isEqualTo(end);
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-001"})
    void booking_with_a_missing_end_is_rejected_and_keeps_entries() {
        navigate(BookRoomView.class);

        test(find(ComboBox.class).id("booking-room-field")).selectItem("Zulu Room");
        test(find(TextField.class).id("booking-booked-by-field")).setValue(NEW_BOOKED_BY);
        LocalDateTime start = LocalDateTime.of(2026, 12, 3, 9, 0);
        test(find(DateTimePicker.class).id("booking-start-field")).setValue(start);
        test(find(Button.class).id("book-room-button")).click();

        DateTimePicker endField = find(DateTimePicker.class).id("booking-end-field");
        assertThat(endField.isInvalid()).isTrue();
        assertThat(endField.getErrorMessage()).isEqualTo("End is required.");
        assertThat(jdbcClient.sql("select count(*) from booking where booked_by = ?")
                .param(NEW_BOOKED_BY).query(Long.class).single()).isZero();
        assertThat(find(TextField.class).id("booking-booked-by-field").getValue()).isEqualTo(NEW_BOOKED_BY);
        assertThat(find(DateTimePicker.class).id("booking-start-field").getValue()).isEqualTo(start);
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-001"})
    void booking_with_an_end_not_after_the_start_is_rejected_and_keeps_entries() {
        navigate(BookRoomView.class);

        test(find(ComboBox.class).id("booking-room-field")).selectItem("Zulu Room");
        test(find(TextField.class).id("booking-booked-by-field")).setValue(NEW_BOOKED_BY);
        // BR-001: the end equals the start — not after it
        LocalDateTime start = LocalDateTime.of(2026, 12, 3, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 12, 3, 9, 0);
        test(find(DateTimePicker.class).id("booking-start-field")).setValue(start);
        test(find(DateTimePicker.class).id("booking-end-field")).setValue(end);
        test(find(Button.class).id("book-room-button")).click();

        DateTimePicker endField = find(DateTimePicker.class).id("booking-end-field");
        assertThat(endField.isInvalid()).isTrue();
        assertThat(endField.getErrorMessage()).isEqualTo("End must be after start.");
        assertThat(jdbcClient.sql("select count(*) from booking where booked_by = ?")
                .param(NEW_BOOKED_BY).query(Long.class).single()).isZero();
        assertThat(find(TextField.class).id("booking-booked-by-field").getValue()).isEqualTo(NEW_BOOKED_BY);
        assertThat(find(DateTimePicker.class).id("booking-start-field").getValue()).isEqualTo(start);
    }

    private void fillForm(String roomName, String bookedBy, LocalDateTime start, LocalDateTime end,
            String purpose) {
        test(find(ComboBox.class).id("booking-room-field")).selectItem(roomName);
        test(find(TextField.class).id("booking-booked-by-field")).setValue(bookedBy);
        test(find(DateTimePicker.class).id("booking-start-field")).setValue(start);
        test(find(DateTimePicker.class).id("booking-end-field")).setValue(end);
        test(find(TextField.class).id("booking-purpose-field")).setValue(purpose);
    }
}
