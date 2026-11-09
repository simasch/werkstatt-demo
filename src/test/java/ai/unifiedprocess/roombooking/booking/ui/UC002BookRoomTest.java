package ai.unifiedprocess.roombooking.booking.ui;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ai.unifiedprocess.roombooking.TestcontainersConfiguration;
import ai.unifiedprocess.roombooking.booking.domain.BookedTimeSlot;
import ai.unifiedprocess.roombooking.room.domain.RoomListItem;
import ai.unifiedprocess.roombooking.usecase.UseCase;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-002 "Book Room", the integration layer: the view runs against the real service,
 * repository, and PostgreSQL. Test data: {@code R__uc001_register_room.sql} (Lakeview, Atrium, Harbour) and
 * {@code R__uc002_book_room.sql} (Studio). The bookings made before (Studio 09:00–10:00 and 13:00–14:00) are
 * inserted before each test; every booking of this test has a name starting with "UC002" and is removed
 * afterwards.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UC002BookRoomTest extends SpringBrowserlessTest {

    private static final String ROOM = "Studio";

    private static final LocalDateTime DAY = LocalDateTime.of(2030, 3, 4, 0, 0);

    private static final String NAME = "UC002 Dana";

    @Autowired
    private JdbcClient jdbcClient;

    private long studioId;

    @BeforeEach
    void insertBookingsMadeBefore() {
        studioId = roomId(ROOM);
        insertBooking(studioId, "UC002 Alice", at(9, 0), at(10, 0), "Weekly sync");
        insertBooking(studioId, "UC002 Bob", at(13, 0), at(14, 0), null);
    }

    @AfterEach
    void removeCreatedBookings() {
        jdbcClient.sql("delete from booking where booked_by like 'UC002%'").update();
    }

    // --- Main success scenario -------------------------------------------------------------

    @Test
    @UseCase(id = "UC-002")
    void booking_page_lists_every_room_with_name_capacity_and_equipment_ordered_by_name() {
        navigate(BookRoomView.class);

        Grid<RoomListItem> grid = roomGrid();
        assertThat(isShown(grid)).isTrue();
        int atrium = rowOf(grid, "Atrium");
        int harbour = rowOf(grid, "Harbour");
        int lakeview = rowOf(grid, "Lakeview");
        int studio = rowOf(grid, ROOM);

        // Step 2: every registered room, ordered by name (OQ-003)
        assertThat(atrium).isNotNegative();
        assertThat(atrium).isLessThan(harbour);
        assertThat(harbour).isLessThan(lakeview);
        assertThat(lakeview).isLessThan(studio);
        // with its name, capacity, and equipment (test data)
        RoomListItem studioRow = test(grid).getRow(studio);
        assertThat(studioRow.name()).isEqualTo(ROOM);
        assertThat(studioRow.capacity()).isEqualTo(6);
        assertThat(studioRow.equipment()).isEqualTo("screen");
        RoomListItem lakeviewRow = test(grid).getRow(lakeview);
        assertThat(lakeviewRow.capacity()).isEqualTo(8);
        assertThat(lakeviewRow.equipment()).isEqualTo("projector, whiteboard");
    }

    @Test
    @UseCase(id = "UC-002")
    void selecting_a_room_shows_the_booking_form_with_its_booked_time_slots() {
        navigate(BookRoomView.class);

        selectRoom(ROOM);

        // Step 4: the booking form of the selected room with its already booked time slots
        assertThat(isShown(nameField())).isTrue();
        assertThat(isShown(startField())).isTrue();
        assertThat(isShown(endField())).isTrue();
        assertThat(isShown(purposeField())).isTrue();
        // find() matches visible components only: the room list is hidden
        assertThat(find(Grid.class).withId("room-grid").exists()).isFalse();
        assertThat(shownSlots()).containsExactly(
                slot("UC002 Alice", at(9, 0), at(10, 0), "Weekly sync"),
                slot("UC002 Bob", at(13, 0), at(14, 0), null));
        assertThat(nameField().getValue()).isEmpty();
        assertThat(startField().getValue()).isNull();
        assertThat(endField().getValue()).isNull();
        assertThat(purposeField().getValue()).isEmpty();
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-001", "BR-002", "BR-003", "BR-004"})
    void saving_a_valid_booking_stores_it_and_shows_it_among_the_booked_time_slots() {
        navigate(BookRoomView.class);
        selectRoom(ROOM);

        fillForm(NAME, at(10, 30), at(11, 30), "Planning");
        save();

        // Step 8: the confirmed booking among the room's booked time slots
        assertNoErrors();
        assertThat(shownSlots()).contains(slot(NAME, at(10, 30), at(11, 30), "Planning"));
        // Success postconditions, read back from the database
        assertThat(storedBookings(NAME)).containsExactly(Map.of(
                "room_id", studioId, "booked_by", NAME, "starts_at", at(10, 30), "ends_at", at(11, 30),
                "purpose", "Planning"));
        assertBookingsMadeBeforeUnchanged();
        assertNoOverlapInStudio();
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-003"})
    void purpose_is_optional() {
        navigate(BookRoomView.class);
        selectRoom(ROOM);

        fillForm(NAME, at(15, 0), at(16, 0), "");
        save();

        assertNoErrors();
        List<Map<String, Object>> stored = storedBookings(NAME);
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0)).containsEntry("starts_at", at(15, 0)).containsEntry("ends_at", at(16, 0));
        // BR-003: no purpose was entered; the specification fixes no representation of "no purpose"
        assertThat((String) stored.get(0).get("purpose")).isNullOrEmpty();
        assertThat(shownSlots()).extracting(BookedTimeSlot::bookedBy).contains(NAME);
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-002"})
    void booking_that_starts_when_another_ends_and_ends_when_another_starts_is_accepted() {
        navigate(BookRoomView.class);
        selectRoom(ROOM);

        // BR-002: touching bookings do not overlap (OQ-005): 10:00–13:00 between Alice and Bob
        fillForm(NAME, at(10, 0), at(13, 0), "Workshop");
        save();

        assertNoErrors();
        assertThat(storedBookings(NAME)).hasSize(1);
        assertThat(shownSlots()).containsExactly(
                slot("UC002 Alice", at(9, 0), at(10, 0), "Weekly sync"),
                slot(NAME, at(10, 0), at(13, 0), "Workshop"),
                slot("UC002 Bob", at(13, 0), at(14, 0), null));
        assertNoOverlapInStudio();
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-002"})
    void booking_of_another_room_does_not_block_the_same_time_slot() {
        navigate(BookRoomView.class);
        selectRoom("Harbour");

        // BR-002 applies to bookings of the same room only: Studio is booked 09:00–10:00
        fillForm(NAME, at(9, 0), at(10, 0), "Interview");
        save();

        assertNoErrors();
        assertThat(storedBookings(NAME)).containsExactly(Map.of(
                "room_id", roomId("Harbour"), "booked_by", NAME, "starts_at", at(9, 0), "ends_at", at(10, 0),
                "purpose", "Interview"));
        assertThat(shownSlots()).containsExactly(slot(NAME, at(9, 0), at(10, 0), "Interview"));
    }

    @Test
    @UseCase(id = "UC-002", businessRules = {"BR-004"})
    void name_of_100_and_purpose_of_200_characters_are_accepted() {
        String name = "UC002" + "x".repeat(95);
        String purpose = "p".repeat(200);
        navigate(BookRoomView.class);
        selectRoom(ROOM);

        fillForm(name, at(15, 0), at(16, 0), purpose);
        save();

        assertNoErrors();
        assertThat(storedBookings(name)).containsExactly(Map.of(
                "room_id", studioId, "booked_by", name, "starts_at", at(15, 0), "ends_at", at(16, 0),
                "purpose", purpose));
    }

    // --- A1: No Room Registered Yet ---------------------------------------------------------

    @Test
    @Transactional
    @UseCase(id = "UC-002", scenario = "A1: No Room Registered Yet")
    void empty_room_list_notes_that_no_room_is_registered_yet() {
        // Empty the tables inside the test transaction; the rollback restores the shared test data
        jdbcClient.sql("delete from booking").update();
        jdbcClient.sql("delete from room").update();
        // Start a fresh UI so that no view instance created before the delete is reused
        cleanVaadinEnvironment();
        initVaadinEnvironment();

        navigate(BookRoomView.class);

        Grid<RoomListItem> grid = roomGrid();
        // A1 step 1: an empty room list with the note that no room is registered yet (OQ-004: no fixed wording)
        assertThat(isShown(grid)).isTrue();
        assertThat(test(grid).size()).isZero();
        assertThat(grid.getEmptyStateText()).isNotBlank();
    }

    // --- A2: Invalid Entry ------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-003"}, requirements = {"NFR-001"})
    void missing_name_is_reported_at_the_name_field_and_entries_are_kept() {
        navigate(BookRoomView.class);
        selectRoom(ROOM);

        fillForm("", at(15, 0), at(16, 0), "Planning");
        save();

        assertRejectedOnlyAt(nameField());
        assertEntriesKept("", at(15, 0), at(16, 0), "Planning");
        assertNothingStored();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-003"}, requirements = {"NFR-001"})
    void name_of_spaces_only_counts_as_missing() {
        navigate(BookRoomView.class);
        selectRoom(ROOM);

        fillForm("   ", at(15, 0), at(16, 0), "Planning");
        save();

        assertRejectedOnlyAt(nameField());
        assertEntriesKept("   ", at(15, 0), at(16, 0), "Planning");
        assertNothingStoredFor("   ");
        assertNothingStored();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-003"}, requirements = {"NFR-001"})
    void missing_start_time_is_reported_at_the_start_time_field_and_entries_are_kept() {
        navigate(BookRoomView.class);
        selectRoom(ROOM);

        fillForm(NAME, null, at(16, 0), "Planning");
        save();

        assertRejectedOnlyAt(startField());
        assertEntriesKept(NAME, null, at(16, 0), "Planning");
        assertNothingStored();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-003"}, requirements = {"NFR-001"})
    void missing_end_time_is_reported_at_the_end_time_field_and_entries_are_kept() {
        navigate(BookRoomView.class);
        selectRoom(ROOM);

        fillForm(NAME, at(15, 0), null, "Planning");
        save();

        assertRejectedOnlyAt(endField());
        assertEntriesKept(NAME, at(15, 0), null, "Planning");
        assertNothingStored();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-001"}, requirements = {"NFR-001"})
    void booking_of_zero_duration_is_rejected() {
        navigate(BookRoomView.class);
        selectRoom(ROOM);

        fillForm(NAME, at(15, 0), at(15, 0), "Planning");
        save();

        // BR-001: the end time is not after the start time — the end time is the offending field
        assertRejectedAt(endField());
        assertThat(nameField().isInvalid()).isFalse();
        assertThat(purposeField().isInvalid()).isFalse();
        assertEntriesKept(NAME, at(15, 0), at(15, 0), "Planning");
        assertNothingStored();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-001"}, requirements = {"NFR-001"})
    void booking_of_negative_duration_is_rejected() {
        navigate(BookRoomView.class);
        selectRoom(ROOM);

        fillForm(NAME, at(16, 0), at(15, 0), "Planning");
        save();

        assertRejectedAt(endField());
        assertThat(nameField().isInvalid()).isFalse();
        assertThat(purposeField().isInvalid()).isFalse();
        assertEntriesKept(NAME, at(16, 0), at(15, 0), "Planning");
        assertNothingStored();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-004"}, requirements = {"NFR-001"})
    void name_of_101_characters_is_rejected_at_the_name_field() {
        String name = "UC002" + "x".repeat(96);
        navigate(BookRoomView.class);
        selectRoom(ROOM);

        fillForm(name, at(15, 0), at(16, 0), "Planning");
        save();

        assertRejectedOnlyAt(nameField());
        assertEntriesKept(name, at(15, 0), at(16, 0), "Planning");
        assertNothingStored();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-004"}, requirements = {"NFR-001"})
    void purpose_of_201_characters_is_rejected_at_the_purpose_field() {
        String purpose = "p".repeat(201);
        navigate(BookRoomView.class);
        selectRoom(ROOM);

        fillForm(NAME, at(15, 0), at(16, 0), purpose);
        save();

        assertRejectedOnlyAt(purposeField());
        assertEntriesKept(NAME, at(15, 0), at(16, 0), purpose);
        assertNothingStored();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-003", "BR-004"},
            requirements = {"NFR-001"})
    void each_offending_field_is_reported_at_once() {
        String purpose = "p".repeat(201);
        navigate(BookRoomView.class);
        selectRoom(ROOM);

        fillForm("", null, at(16, 0), purpose);
        save();

        // A2 step 1: the system reports at each offending field
        assertRejectedAt(nameField());
        assertRejectedAt(startField());
        assertRejectedAt(purposeField());
        assertThat(endField().isInvalid()).isFalse();
        assertEntriesKept("", null, at(16, 0), purpose);
        assertNothingStored();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A2: Invalid Entry", businessRules = {"BR-003"}, requirements = {"NFR-001"})
    void corrected_entry_is_saved_after_a_rejection() {
        navigate(BookRoomView.class);
        selectRoom(ROOM);
        fillForm("", at(15, 0), at(16, 0), "Planning");
        save();
        assertRejectedAt(nameField());

        // A2 step 3: the use case continues at step 5
        test(nameField()).setValue(NAME);
        save();

        assertNoErrors();
        assertThat(storedBookings(NAME)).containsExactly(Map.of(
                "room_id", studioId, "booked_by", NAME, "starts_at", at(15, 0), "ends_at", at(16, 0),
                "purpose", "Planning"));
        assertThat(shownSlots()).contains(slot(NAME, at(15, 0), at(16, 0), "Planning"));
    }

    // --- A3: Overlapping Booking ------------------------------------------------------------

    @Test
    @UseCase(id = "UC-002", scenario = "A3: Overlapping Booking", businessRules = {"BR-002"},
            requirements = {"NFR-001"})
    void booking_that_overlaps_another_booking_of_the_room_is_rejected() {
        navigate(BookRoomView.class);
        selectRoom(ROOM);

        // Overlaps Alice's booking 09:00–10:00 by half an hour
        fillForm(NAME, at(9, 30), at(10, 30), "Planning");
        save();

        assertOverlapReported();
        assertEntriesKept(NAME, at(9, 30), at(10, 30), "Planning");
        assertOverlapFailurePostconditions();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A3: Overlapping Booking", businessRules = {"BR-002"},
            requirements = {"NFR-001"})
    void booking_that_contains_another_booking_of_the_room_is_rejected() {
        navigate(BookRoomView.class);
        selectRoom(ROOM);

        // Contains Bob's booking 13:00–14:00
        fillForm(NAME, at(12, 0), at(15, 0), "Planning");
        save();

        assertOverlapReported();
        assertEntriesKept(NAME, at(12, 0), at(15, 0), "Planning");
        assertOverlapFailurePostconditions();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A3: Overlapping Booking", businessRules = {"BR-002"},
            requirements = {"NFR-001"})
    void booking_of_exactly_the_same_time_slot_is_rejected() {
        navigate(BookRoomView.class);
        selectRoom(ROOM);

        fillForm(NAME, at(9, 0), at(10, 0), "Planning");
        save();

        assertOverlapReported();
        assertEntriesKept(NAME, at(9, 0), at(10, 0), "Planning");
        assertOverlapFailurePostconditions();
    }

    @Test
    @UseCase(id = "UC-002", scenario = "A3: Overlapping Booking", businessRules = {"BR-002"},
            requirements = {"NFR-001"})
    void booking_saved_by_someone_else_while_entering_the_data_is_rejected() {
        navigate(BookRoomView.class);
        selectRoom(ROOM);
        fillForm(NAME, at(16, 0), at(17, 0), "Planning");

        // Competing change: another employee books 16:30–17:30 while this one enters the data
        insertBooking(studioId, "UC002 Erin", at(16, 30), at(17, 30), "Review");
        save();

        assertOverlapReported();
        assertEntriesKept(NAME, at(16, 0), at(17, 0), "Planning");
        assertNothingStored();
        assertThat(storedBookings("UC002 Erin")).containsExactly(Map.of(
                "room_id", studioId, "booked_by", "UC002 Erin", "starts_at", at(16, 30), "ends_at", at(17, 30),
                "purpose", "Review"));
        assertNoOverlapInStudio();
    }

    // --- A4: Booking Cancelled --------------------------------------------------------------

    @Test
    @UseCase(id = "UC-002", scenario = "A4: Booking Cancelled")
    void cancel_discards_the_entries_and_shows_the_room_list() {
        navigate(BookRoomView.class);
        selectRoom(ROOM);
        fillForm(NAME, at(15, 0), at(16, 0), "Planning");

        test(find(Button.class).withText("Cancel").single()).click();

        // A4 step 1: the entries are discarded and the room list is displayed
        assertThat(isShown(roomGrid())).isTrue();
        // find() matches visible components only: the booking form is hidden
        assertThat(find(TextField.class).withCaption("Name").exists()).isFalse();
        assertNothingStored();
        assertBookingsMadeBeforeUnchanged();

        // The entries are discarded: booking the room again starts with an empty form
        selectRoom(ROOM);
        assertThat(nameField().getValue()).isEmpty();
        assertThat(startField().getValue()).isNull();
        assertThat(endField().getValue()).isNull();
        assertThat(purposeField().getValue()).isEmpty();
    }

    // --- Helpers ----------------------------------------------------------------------------

    private static LocalDateTime at(int hour, int minute) {
        return DAY.withHour(hour).withMinute(minute);
    }

    private static BookedTimeSlot slot(String bookedBy, LocalDateTime startsAt, LocalDateTime endsAt,
            String purpose) {
        // The id is not compared (see shownSlots)
        return new BookedTimeSlot(0, bookedBy, startsAt, endsAt, purpose);
    }

    private long roomId(String name) {
        return jdbcClient.sql("select id from room where name = ?").param(name).query(Long.class).single();
    }

    private void insertBooking(long roomId, String bookedBy, LocalDateTime startsAt, LocalDateTime endsAt,
            String purpose) {
        jdbcClient.sql("insert into booking (room_id, booked_by, starts_at, ends_at, purpose) values (?, ?, ?, ?, ?)")
                .params(roomId, bookedBy, startsAt, endsAt, purpose)
                .update();
    }

    @SuppressWarnings("unchecked")
    private Grid<RoomListItem> roomGrid() {
        return find(Grid.class).id("room-grid");
    }

    @SuppressWarnings("unchecked")
    private Grid<BookedTimeSlot> slotGrid() {
        return find(Grid.class).id("booked-time-slot-grid");
    }

    private int rowOf(Grid<RoomListItem> grid, String name) {
        var tester = test(grid);
        for (int i = 0; i < tester.size(); i++) {
            if (name.equals(tester.getRow(i).name())) {
                return i;
            }
        }
        return -1;
    }

    private void selectRoom(String name) {
        Grid<RoomListItem> grid = roomGrid();
        int row = rowOf(grid, name);
        assertThat(row).as("room %s in the room list", name).isNotNegative();
        test(grid).select(row);
    }

    /** The booked time slots the view shows, with the id set to 0 so that they compare by value. */
    private List<BookedTimeSlot> shownSlots() {
        Grid<BookedTimeSlot> grid = slotGrid();
        assertThat(isShown(grid)).isTrue();
        var tester = test(grid);
        List<BookedTimeSlot> slots = new ArrayList<>();
        for (int i = 0; i < tester.size(); i++) {
            BookedTimeSlot row = tester.getRow(i);
            slots.add(slot(row.bookedBy(), row.startsAt(), row.endsAt(), row.purpose()));
        }
        return slots;
    }

    private void fillForm(String name, LocalDateTime startsAt, LocalDateTime endsAt, String purpose) {
        test(nameField()).setValue(name);
        if (startsAt != null) {
            test(startField()).setValue(startsAt);
        }
        if (endsAt != null) {
            test(endField()).setValue(endsAt);
        }
        test(purposeField()).setValue(purpose);
    }

    private void save() {
        test(find(Button.class).withText("Save").single()).click();
    }

    private TextField nameField() {
        return find(TextField.class).withCaption("Name").single();
    }

    private DateTimePicker startField() {
        return find(DateTimePicker.class).withCaption("Start time").single();
    }

    private DateTimePicker endField() {
        return find(DateTimePicker.class).withCaption("End time").single();
    }

    private TextArea purposeField() {
        return find(TextArea.class).withCaption("Purpose").single();
    }

    private List<HasValidation> fields() {
        return List.of(nameField(), startField(), endField(), purposeField());
    }

    /** Whether the component and every parent of it are visible. */
    private static boolean isShown(Component component) {
        Component current = component;
        while (current != null) {
            if (!current.isVisible()) {
                return false;
            }
            current = current.getParent().orElse(null);
        }
        return true;
    }

    private static void assertRejectedAt(HasValidation field) {
        // A2 step 1: the field says what is wrong with it (OQ-004: no fixed wording)
        assertThat(field.isInvalid()).isTrue();
        assertThat(field.getErrorMessage()).isNotBlank();
    }

    private void assertRejectedOnlyAt(HasValidation offending) {
        assertRejectedAt(offending);
        for (HasValidation field : fields()) {
            if (field != offending) {
                assertThat(field.isInvalid()).as("no error at %s", field).isFalse();
            }
        }
    }

    private void assertNoErrors() {
        for (HasValidation field : fields()) {
            assertThat(field.isInvalid()).as("no error at %s", field).isFalse();
        }
    }

    private void assertOverlapReported() {
        // A3 step 1: the system reports that the room is already booked for an overlapping time slot
        // (OQ-004: no fixed wording, no fixed place) — at a field of the form or in a notification
        boolean atField = fields().stream()
                .anyMatch(field -> field.isInvalid() && field.getErrorMessage() != null
                        && !field.getErrorMessage().isBlank());
        boolean notified = find(Notification.class).all().stream()
                .anyMatch(notification -> !test(notification).getText().isBlank());
        assertThat(atField || notified).as("overlap reported").isTrue();
    }

    private void assertEntriesKept(String name, LocalDateTime startsAt, LocalDateTime endsAt, String purpose) {
        // A2/A3 step 2, NFR-001: the form stays with every entry, including the rejected ones
        assertThat(isShown(nameField())).isTrue();
        assertThat(nameField().getValue()).isEqualTo(name);
        assertThat(startField().getValue()).isEqualTo(startsAt);
        assertThat(endField().getValue()).isEqualTo(endsAt);
        assertThat(purposeField().getValue()).isEqualTo(purpose);
    }

    private void assertNothingStored() {
        assertNothingStoredFor(NAME);
        assertThat(jdbcClient.sql("select count(*) from booking where booked_by like 'UC002%' "
                + "and booked_by not in ('UC002 Alice', 'UC002 Bob', 'UC002 Erin')")
                .query(Integer.class).single()).isZero();
    }

    private void assertNothingStoredFor(String name) {
        assertThat(storedBookings(name)).isEmpty();
    }

    private void assertOverlapFailurePostconditions() {
        assertNothingStored();
        assertBookingsMadeBeforeUnchanged();
        assertNoOverlapInStudio();
    }

    private void assertBookingsMadeBeforeUnchanged() {
        assertThat(storedBookings("UC002 Alice")).containsExactly(Map.of(
                "room_id", studioId, "booked_by", "UC002 Alice", "starts_at", at(9, 0), "ends_at", at(10, 0),
                "purpose", "Weekly sync"));
        List<Map<String, Object>> bob = storedBookings("UC002 Bob");
        assertThat(bob).hasSize(1);
        assertThat(bob.get(0)).containsEntry("room_id", studioId)
                .containsEntry("starts_at", at(13, 0))
                .containsEntry("ends_at", at(14, 0))
                .containsEntry("purpose", null);
    }

    private void assertNoOverlapInStudio() {
        // BR-002: no two bookings of the same room overlap ([start, end))
        assertThat(jdbcClient.sql("select count(*) from booking a join booking b on a.room_id = b.room_id "
                + "and a.id < b.id and a.starts_at < b.ends_at and b.starts_at < a.ends_at where a.room_id = ?")
                .param(studioId).query(Integer.class).single()).isZero();
    }

    private List<Map<String, Object>> storedBookings(String bookedBy) {
        return jdbcClient.sql("select room_id, booked_by, starts_at, ends_at, purpose from booking where booked_by = ?")
                .param(bookedBy)
                .query((rs, n) -> {
                    Map<String, Object> row = new java.util.HashMap<>();
                    row.put("room_id", rs.getLong("room_id"));
                    row.put("booked_by", rs.getString("booked_by"));
                    row.put("starts_at", rs.getTimestamp("starts_at").toLocalDateTime());
                    row.put("ends_at", rs.getTimestamp("ends_at").toLocalDateTime());
                    row.put("purpose", rs.getString("purpose"));
                    return row;
                })
                .list();
    }
}
