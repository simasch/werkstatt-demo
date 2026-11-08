package ai.unifiedprocess.roombooking.booking.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import ai.unifiedprocess.roombooking.TestcontainersConfiguration;
import ai.unifiedprocess.roombooking.room.domain.Room;
import ai.unifiedprocess.roombooking.usecase.UseCase;
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

/**
 * Use case test for UC-002 "Book Room", the integration layer of the use case: the view runs against the real
 * service, repository, and PostgreSQL. Rooms and bookings come from {@code R__uc002_book_room.sql}: the room
 * Weisshorn is booked on 2026-11-09 from 10:00 to 11:00 (Anna Keller) and from 13:00 to 14:00 (Ben Huber); the room
 * Dom has no booking. Every expected value comes from the specification or that test data.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UC002BookRoomTest extends SpringBrowserlessTest {

    private static final long WEISSHORN = 900011;

    private static final long DOM = 900012;

    private static final String ROOM_REQUIRED = "Room is required.";

    private static final String BOOKED_BY_REQUIRED = "Booked by is required.";

    private static final String BOOKED_BY_TOO_LONG = "Booked by must be at most 100 characters.";

    private static final String START_REQUIRED = "Start is required.";

    private static final String END_REQUIRED = "End is required.";

    private static final String END_NOT_AFTER_START = "End must be after start.";

    private static final String PURPOSE_TOO_LONG = "Purpose must be at most 200 characters.";

    /** The booking of Anna Keller in Weisshorn, as A1 names it (BR-004 format). */
    private static final String ALREADY_BOOKED_10_TO_11 =
            "The room is already booked from 2026-11-09 10:00 to 2026-11-09 11:00.";

    private static final LocalDateTime NOV_9_0900 = LocalDateTime.of(2026, 11, 9, 9, 0);

    private static final LocalDateTime NOV_9_1030 = LocalDateTime.of(2026, 11, 9, 10, 30);

    private static final LocalDateTime NOV_9_1100 = LocalDateTime.of(2026, 11, 9, 11, 0);

    private static final LocalDateTime NOV_9_1130 = LocalDateTime.of(2026, 11, 9, 11, 30);

    private static final LocalDateTime NOV_9_1300 = LocalDateTime.of(2026, 11, 9, 13, 0);

    private static final LocalDateTime NOV_9_1500 = LocalDateTime.of(2026, 11, 9, 15, 0);

    private static final LocalDateTime NOV_10_0900 = LocalDateTime.of(2026, 11, 10, 9, 0);

    private static final LocalDateTime NOV_10_1000 = LocalDateTime.of(2026, 11, 10, 10, 0);

    @Autowired
    private JdbcClient jdbcClient;

    @AfterEach
    void removeCreatedBookings() {
        // Targeted and idempotent: only bookings of the UC-002 rooms a test created, never the shared test data
        jdbcClient.sql("delete from booking where room_id in (?, ?) and id < 900000").params(WEISSHORN, DOM).update();
    }

    // ---------------------------------------------------------------- Main Success Scenario

    @Test
    @UseCase(id = "UC-002")
    void main_menu_offers_book_room_and_leads_to_the_booking_form() {
        navigate(BookRoomView.class);

        // Step 1: "Book Room" in the main menu
        test(find(SideNav.class).single()).clickItem("Book Room");

        assertThat(getCurrentView()).isInstanceOf(BookRoomView.class);
    }

    @Test
    @UseCase(id = "UC-002")
    void booking_form_shows_room_booked_by_start_end_and_purpose() {
        navigate(BookRoomView.class);

        // Step 2: the fields "Room", "Booked by", "Start", "End", and "Purpose"
        assertThat(roomField().isVisible()).isTrue();
        assertThat(bookedByField().isVisible()).isTrue();
        assertThat(startField().isVisible()).isTrue();
        assertThat(endField().isVisible()).isTrue();
        assertThat(purposeField().isVisible()).isTrue();
        assertThat(find(Button.class).withText("Book").exists()).isTrue();
        // Precondition: the registered rooms can be selected
        assertThat(test(roomField()).getSuggestionItems()).extracting(Room::name).contains("Weisshorn", "Dom");
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-004"})
    void booking_a_free_room_stores_the_booking_and_shows_the_message() {
        navigate(BookRoomView.class);

        // Step 3
        fill("Dom", "Clara Meier", NOV_10_0900, NOV_10_1000, "Team meeting");
        click("Book");

        // Step 5: message with dates as yyyy-MM-dd HH:mm (BR-004)
        assertThat(test(find(Notification.class).single()).getText())
                .isEqualTo("Room Dom booked from 2026-11-10 09:00 to 2026-11-10 10:00.");
        // Success postcondition: the booking is stored for the room and the time slot
        assertThat(storedBookings(DOM)).containsExactly(
                booking("Clara Meier", NOV_10_0900, NOV_10_1000, "Team meeting"));
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-003"})
    void the_purpose_is_optional() {
        navigate(BookRoomView.class);

        fill("Dom", "Clara Meier", NOV_10_0900, NOV_10_1000, null);
        click("Book");

        assertThat(test(find(Notification.class).single()).getText())
                .isEqualTo("Room Dom booked from 2026-11-10 09:00 to 2026-11-10 10:00.");
        assertThat(storedBookings(DOM)).containsExactly(booking("Clara Meier", NOV_10_0900, NOV_10_1000, null));
    }

    // ---------------------------------------------------------------- BR-002 allowed cases

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-002"})
    void a_booking_that_touches_other_bookings_at_both_ends_does_not_overlap() {
        navigate(BookRoomView.class);

        // Starts as Anna Keller's booking ends, ends as Ben Huber's starts
        fill("Weisshorn", "Clara Meier", NOV_9_1100, NOV_9_1300, null);
        click("Book");

        assertThat(test(find(Notification.class).single()).getText())
                .isEqualTo("Room Weisshorn booked from 2026-11-09 11:00 to 2026-11-09 13:00.");
        assertThat(storedBookings(WEISSHORN)).contains(booking("Clara Meier", NOV_9_1100, NOV_9_1300, null));
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-002"})
    void a_booking_of_another_room_at_the_same_time_does_not_overlap() {
        navigate(BookRoomView.class);

        // Same time slot as Anna Keller's booking of Weisshorn, but in Dom
        fill("Dom", "Clara Meier", LocalDateTime.of(2026, 11, 9, 10, 0), NOV_9_1100, null);
        click("Book");

        assertThat(test(find(Notification.class).single()).getText())
                .isEqualTo("Room Dom booked from 2026-11-09 10:00 to 2026-11-09 11:00.");
        assertThat(storedBookings(DOM)).containsExactly(
                booking("Clara Meier", LocalDateTime.of(2026, 11, 9, 10, 0), NOV_9_1100, null));
    }

    // ---------------------------------------------------------------- A1 / BR-002

    @Test
    @UseCase(id = "UC-002", scenario = "A1: Room Already Booked", businessRules = {"BR-002", "BR-004"},
            requirements = {"NFR-001"})
    void an_overlapping_time_slot_is_rejected_at_start_and_end() {
        navigate(BookRoomView.class);

        fill("Weisshorn", "Clara Meier", NOV_9_1030, NOV_9_1130, "Workshop");
        click("Book");

        assertThat(startField().isInvalid()).isTrue();
        assertThat(startField().getErrorMessage()).isEqualTo(ALREADY_BOOKED_10_TO_11);
        assertThat(endField().isInvalid()).isTrue();
        assertThat(endField().getErrorMessage()).isEqualTo(ALREADY_BOOKED_10_TO_11);
        // Every entry of the form is kept (NFR-001)
        assertFormKeeps("Weisshorn", "Clara Meier", NOV_9_1030, NOV_9_1130, "Workshop");
        // Failure postcondition: no booking is stored
        assertThat(find(Notification.class).exists()).isFalse();
        assertThat(storedBookings(WEISSHORN)).containsExactly(
                booking("Anna Keller", LocalDateTime.of(2026, 11, 9, 10, 0), NOV_9_1100, "Sprint review"),
                booking("Ben Huber", NOV_9_1300, LocalDateTime.of(2026, 11, 9, 14, 0), null));
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A1: Room Already Booked", businessRules = {"BR-002"})
    void a_time_slot_overlapping_several_bookings_names_the_one_that_starts_first() {
        navigate(BookRoomView.class);

        // 09:00–15:00 overlaps Anna Keller's (10:00) and Ben Huber's (13:00) booking
        fill("Weisshorn", "Clara Meier", NOV_9_0900, NOV_9_1500, null);
        click("Book");

        assertThat(startField().getErrorMessage()).isEqualTo(ALREADY_BOOKED_10_TO_11);
        assertThat(endField().getErrorMessage()).isEqualTo(ALREADY_BOOKED_10_TO_11);
        assertThat(storedBookings(WEISSHORN)).hasSize(2);
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A1: Room Already Booked", businessRules = {"BR-002"})
    void a_time_slot_inside_another_booking_is_rejected() {
        navigate(BookRoomView.class);

        fill("Weisshorn", "Clara Meier", LocalDateTime.of(2026, 11, 9, 13, 15), LocalDateTime.of(2026, 11, 9, 13, 45),
                null);
        click("Book");

        String expected = "The room is already booked from 2026-11-09 13:00 to 2026-11-09 14:00.";
        assertThat(startField().getErrorMessage()).isEqualTo(expected);
        assertThat(endField().getErrorMessage()).isEqualTo(expected);
        assertThat(storedBookings(WEISSHORN)).hasSize(2);
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A1: Room Already Booked", businessRules = {"BR-002"})
    void after_an_overlap_the_actor_changes_the_time_slot_and_books() {
        navigate(BookRoomView.class);

        fill("Weisshorn", "Clara Meier", NOV_9_1030, NOV_9_1130, null);
        click("Book");
        assertThat(startField().isInvalid()).isTrue();

        // A1 step 2: continues at step 3
        test(startField()).setValue(NOV_9_1100);
        test(endField()).setValue(NOV_9_1300);
        click("Book");

        assertThat(test(find(Notification.class).single()).getText())
                .isEqualTo("Room Weisshorn booked from 2026-11-09 11:00 to 2026-11-09 13:00.");
        assertThat(storedBookings(WEISSHORN)).contains(booking("Clara Meier", NOV_9_1100, NOV_9_1300, null));
    }

    // ---------------------------------------------------------------- A2: BR-001

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-001"}, requirements = {"NFR-001"})
    void a_missing_start_is_rejected() {
        navigate(BookRoomView.class);

        fill("Dom", "Clara Meier", null, NOV_10_1000, "Team meeting");
        click("Book");

        assertThat(startField().isInvalid()).isTrue();
        assertThat(startField().getErrorMessage()).isEqualTo(START_REQUIRED);
        assertFormKeeps("Dom", "Clara Meier", null, NOV_10_1000, "Team meeting");
        assertThat(storedBookings(DOM)).isEmpty();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-001"}, requirements = {"NFR-001"})
    void a_missing_end_is_rejected() {
        navigate(BookRoomView.class);

        fill("Dom", "Clara Meier", NOV_10_0900, null, "Team meeting");
        click("Book");

        assertThat(endField().isInvalid()).isTrue();
        assertThat(endField().getErrorMessage()).isEqualTo(END_REQUIRED);
        assertFormKeeps("Dom", "Clara Meier", NOV_10_0900, null, "Team meeting");
        assertThat(storedBookings(DOM)).isEmpty();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-001"}, requirements = {"NFR-001"})
    void an_end_equal_to_the_start_is_rejected_at_the_end() {
        navigate(BookRoomView.class);

        fill("Dom", "Clara Meier", NOV_10_0900, NOV_10_0900, "Team meeting");
        click("Book");

        assertThat(endField().isInvalid()).isTrue();
        assertThat(endField().getErrorMessage()).isEqualTo(END_NOT_AFTER_START);
        assertFormKeeps("Dom", "Clara Meier", NOV_10_0900, NOV_10_0900, "Team meeting");
        assertThat(storedBookings(DOM)).isEmpty();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-001"})
    void an_end_before_the_start_is_rejected_at_the_end() {
        navigate(BookRoomView.class);

        fill("Dom", "Clara Meier", NOV_10_1000, NOV_10_0900, null);
        click("Book");

        assertThat(endField().isInvalid()).isTrue();
        assertThat(endField().getErrorMessage()).isEqualTo(END_NOT_AFTER_START);
        assertThat(storedBookings(DOM)).isEmpty();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-001"})
    void an_end_one_minute_after_the_start_is_accepted() {
        navigate(BookRoomView.class);

        fill("Dom", "Clara Meier", NOV_10_0900, NOV_10_0900.plusMinutes(1), null);
        click("Book");

        assertThat(test(find(Notification.class).single()).getText())
                .isEqualTo("Room Dom booked from 2026-11-10 09:00 to 2026-11-10 09:01.");
        assertThat(storedBookings(DOM)).containsExactly(
                booking("Clara Meier", NOV_10_0900, NOV_10_0900.plusMinutes(1), null));
    }

    // ---------------------------------------------------------------- A2: BR-003

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-003"}, requirements = {"NFR-001"})
    void a_missing_room_is_rejected() {
        navigate(BookRoomView.class);

        fill(null, "Clara Meier", NOV_10_0900, NOV_10_1000, "Team meeting");
        click("Book");

        assertThat(roomField().isInvalid()).isTrue();
        assertThat(roomField().getErrorMessage()).isEqualTo(ROOM_REQUIRED);
        assertFormKeeps(null, "Clara Meier", NOV_10_0900, NOV_10_1000, "Team meeting");
        assertThat(bookingsBy("Clara Meier")).isZero();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-003"}, requirements = {"NFR-001"})
    void an_empty_name_is_rejected() {
        navigate(BookRoomView.class);

        fill("Dom", null, NOV_10_0900, NOV_10_1000, "Team meeting");
        click("Book");

        assertThat(bookedByField().isInvalid()).isTrue();
        assertThat(bookedByField().getErrorMessage()).isEqualTo(BOOKED_BY_REQUIRED);
        assertFormKeeps("Dom", "", NOV_10_0900, NOV_10_1000, "Team meeting");
        assertThat(storedBookings(DOM)).isEmpty();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-003"})
    void a_name_of_only_spaces_is_rejected_as_empty() {
        navigate(BookRoomView.class);

        fill("Dom", "   ", NOV_10_0900, NOV_10_1000, null);
        click("Book");

        assertThat(bookedByField().isInvalid()).isTrue();
        assertThat(bookedByField().getErrorMessage()).isEqualTo(BOOKED_BY_REQUIRED);
        assertThat(storedBookings(DOM)).isEmpty();
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-003"})
    void leading_and_trailing_spaces_are_removed_from_the_name() {
        navigate(BookRoomView.class);

        fill("Dom", "  Clara Meier  ", NOV_10_0900, NOV_10_1000, null);
        click("Book");

        assertThat(test(find(Notification.class).single()).getText())
                .isEqualTo("Room Dom booked from 2026-11-10 09:00 to 2026-11-10 10:00.");
        assertThat(storedBookings(DOM)).containsExactly(booking("Clara Meier", NOV_10_0900, NOV_10_1000, null));
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-003"})
    void a_name_of_100_characters_is_accepted() {
        String name100 = "N".repeat(100);
        navigate(BookRoomView.class);

        fill("Dom", name100, NOV_10_0900, NOV_10_1000, null);
        click("Book");

        assertThat(test(find(Notification.class).single()).getText())
                .isEqualTo("Room Dom booked from 2026-11-10 09:00 to 2026-11-10 10:00.");
        assertThat(storedBookings(DOM)).containsExactly(booking(name100, NOV_10_0900, NOV_10_1000, null));
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-003"})
    void spaces_around_a_100_character_name_do_not_count_towards_the_limit() {
        String name100 = "N".repeat(100);
        navigate(BookRoomView.class);

        fill("Dom", " " + name100 + " ", NOV_10_0900, NOV_10_1000, null);
        click("Book");

        assertThat(test(find(Notification.class).single()).getText())
                .isEqualTo("Room Dom booked from 2026-11-10 09:00 to 2026-11-10 10:00.");
        assertThat(storedBookings(DOM)).containsExactly(booking(name100, NOV_10_0900, NOV_10_1000, null));
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-003"}, requirements = {"NFR-001"})
    void a_name_of_101_characters_is_rejected() {
        String name101 = "N".repeat(101);
        navigate(BookRoomView.class);

        fill("Dom", name101, NOV_10_0900, NOV_10_1000, "Team meeting");
        click("Book");

        assertThat(bookedByField().isInvalid()).isTrue();
        assertThat(bookedByField().getErrorMessage()).isEqualTo(BOOKED_BY_TOO_LONG);
        assertFormKeeps("Dom", name101, NOV_10_0900, NOV_10_1000, "Team meeting");
        assertThat(storedBookings(DOM)).isEmpty();
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-003"})
    void a_purpose_of_200_characters_is_accepted() {
        String purpose200 = "P".repeat(200);
        navigate(BookRoomView.class);

        fill("Dom", "Clara Meier", NOV_10_0900, NOV_10_1000, purpose200);
        click("Book");

        assertThat(test(find(Notification.class).single()).getText())
                .isEqualTo("Room Dom booked from 2026-11-10 09:00 to 2026-11-10 10:00.");
        assertThat(storedBookings(DOM)).containsExactly(booking("Clara Meier", NOV_10_0900, NOV_10_1000, purpose200));
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-003"}, requirements = {"NFR-001"})
    void a_purpose_of_201_characters_is_rejected() {
        String purpose201 = "P".repeat(201);
        navigate(BookRoomView.class);

        fill("Dom", "Clara Meier", NOV_10_0900, NOV_10_1000, purpose201);
        click("Book");

        assertThat(purposeField().isInvalid()).isTrue();
        assertThat(purposeField().getErrorMessage()).isEqualTo(PURPOSE_TOO_LONG);
        assertFormKeeps("Dom", "Clara Meier", NOV_10_0900, NOV_10_1000, purpose201);
        assertThat(storedBookings(DOM)).isEmpty();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-001", "BR-003"},
            requirements = {"NFR-001"})
    void every_offending_field_shows_the_message_of_its_rule() {
        String purpose201 = "P".repeat(201);
        navigate(BookRoomView.class);

        fill(null, null, null, null, purpose201);
        click("Book");

        assertThat(roomField().getErrorMessage()).isEqualTo(ROOM_REQUIRED);
        assertThat(roomField().isInvalid()).isTrue();
        assertThat(bookedByField().isInvalid()).isTrue();
        assertThat(bookedByField().getErrorMessage()).isEqualTo(BOOKED_BY_REQUIRED);
        assertThat(startField().isInvalid()).isTrue();
        assertThat(startField().getErrorMessage()).isEqualTo(START_REQUIRED);
        assertThat(endField().isInvalid()).isTrue();
        assertThat(endField().getErrorMessage()).isEqualTo(END_REQUIRED);
        assertThat(purposeField().isInvalid()).isTrue();
        assertThat(purposeField().getErrorMessage()).isEqualTo(PURPOSE_TOO_LONG);
        assertThat(purposeField().getValue()).isEqualTo(purpose201);
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-003"})
    void after_an_invalid_entry_the_actor_corrects_it_and_books() {
        navigate(BookRoomView.class);

        fill("Dom", null, NOV_10_0900, NOV_10_1000, null);
        click("Book");
        assertThat(bookedByField().isInvalid()).isTrue();

        // A2 step 2: continues at step 3
        test(bookedByField()).setValue("Clara Meier");
        click("Book");

        assertThat(test(find(Notification.class).single()).getText())
                .isEqualTo("Room Dom booked from 2026-11-10 09:00 to 2026-11-10 10:00.");
        assertThat(storedBookings(DOM)).containsExactly(booking("Clara Meier", NOV_10_0900, NOV_10_1000, null));
    }

    // ---------------------------------------------------------------- helpers

    private void fill(String room, String bookedBy, LocalDateTime start, LocalDateTime end, String purpose) {
        if (room != null) {
            test(roomField()).selectItem(room);
        }
        if (bookedBy != null) {
            test(bookedByField()).setValue(bookedBy);
        }
        if (start != null) {
            test(startField()).setValue(start);
        }
        if (end != null) {
            test(endField()).setValue(end);
        }
        if (purpose != null) {
            test(purposeField()).setValue(purpose);
        }
    }

    private void assertFormKeeps(String room, String bookedBy, LocalDateTime start, LocalDateTime end,
            String purpose) {
        if (room == null) {
            assertThat(roomField().getValue()).isNull();
        }
        else {
            assertThat(roomField().getValue().name()).isEqualTo(room);
        }
        assertThat(bookedByField().getValue()).isEqualTo(bookedBy);
        assertThat(startField().getValue()).isEqualTo(start);
        assertThat(endField().getValue()).isEqualTo(end);
        assertThat(purposeField().getValue()).isEqualTo(purpose);
    }

    private static Map<String, Object> booking(String bookedBy, LocalDateTime start, LocalDateTime end,
            String purpose) {
        return purpose == null
                ? Map.of("bookedBy", bookedBy, "start", start, "end", end)
                : Map.of("bookedBy", bookedBy, "start", start, "end", end, "purpose", purpose);
    }

    /** The stored bookings of the room ordered by start; purpose only when one is stored. */
    private List<Map<String, Object>> storedBookings(long roomId) {
        return jdbcClient.sql("select booked_by, starts_at, ends_at, purpose from booking where room_id = ? "
                + "order by starts_at")
                .param(roomId)
                .query((rs, i) -> booking(rs.getString("booked_by"), rs.getTimestamp("starts_at").toLocalDateTime(),
                        rs.getTimestamp("ends_at").toLocalDateTime(), rs.getString("purpose")))
                .list();
    }

    private int bookingsBy(String bookedBy) {
        return jdbcClient.sql("select count(*) from booking where booked_by = ?").param(bookedBy)
                .query(Integer.class).single();
    }

    private void click(String text) {
        test(find(Button.class).withText(text).single()).click();
    }

    @SuppressWarnings("unchecked")
    private ComboBox<Room> roomField() {
        return find(ComboBox.class).withCaption("Room").single();
    }

    private TextField bookedByField() {
        return find(TextField.class).withCaption("Booked by").single();
    }

    private DateTimePicker startField() {
        return find(DateTimePicker.class).withCaption("Start").single();
    }

    private DateTimePicker endField() {
        return find(DateTimePicker.class).withCaption("End").single();
    }

    private TextField purposeField() {
        return find(TextField.class).withCaption("Purpose").single();
    }
}
