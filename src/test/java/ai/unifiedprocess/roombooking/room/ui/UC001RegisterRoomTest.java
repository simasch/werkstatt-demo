package ai.unifiedprocess.roombooking.room.ui;

import java.util.List;
import java.util.Map;

import ai.unifiedprocess.roombooking.TestcontainersConfiguration;
import ai.unifiedprocess.roombooking.booking.ui.BookRoomView;
import ai.unifiedprocess.roombooking.room.domain.RoomListItem;
import ai.unifiedprocess.roombooking.room.service.RegisterRoom;
import ai.unifiedprocess.roombooking.room.service.RoomNameTakenException;
import ai.unifiedprocess.roombooking.usecase.UseCase;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-001 "Register Room", the integration layer: the view runs against the real
 * service, repository, and PostgreSQL. Test data: {@code R__uc001_register_room.sql} (Lakeview, Atrium,
 * Harbour). Every room a test creates has a name starting with "UC001" and is removed afterwards.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UC001RegisterRoomTest extends SpringBrowserlessTest {

    private static final String NEW_NAME = "UC001 Boardroom";

    @Autowired
    private JdbcClient jdbcClient;

    @AfterEach
    void removeCreatedRooms() {
        jdbcClient.sql("delete from room where lower(name) like 'uc001%'").update();
    }

    // --- Main success scenario -------------------------------------------------------------

    @Test
    @UseCase(id = "UC-001")
    void overview_lists_every_room_with_name_capacity_and_equipment_ordered_by_name() {
        navigate(RegisterRoomView.class);

        Grid<RoomListItem> grid = grid();
        int atrium = rowOf(grid, "Atrium");
        int harbour = rowOf(grid, "Harbour");
        int lakeview = rowOf(grid, "Lakeview");

        // Step 2: every registered room, ordered by name
        assertThat(atrium).isNotNegative();
        assertThat(atrium).isLessThan(harbour);
        assertThat(harbour).isLessThan(lakeview);
        // with its name, capacity, and equipment (test data)
        assertThat(test(grid).getCellText(lakeview, 0)).isEqualTo("Lakeview");
        assertThat(test(grid).getCellText(lakeview, 1)).isEqualTo("8");
        assertThat(test(grid).getCellText(lakeview, 2)).isEqualTo("projector, whiteboard");
        assertThat(test(grid).getCellText(atrium, 1)).isEqualTo("20");
    }

    @Test
    @UseCase(id = "UC-001")
    void register_room_shows_an_empty_form() {
        navigate(RegisterRoomView.class);

        openForm();

        // Step 4: an empty room form
        assertThat(find(Dialog.class).exists()).isTrue();
        assertThat(nameField().getValue()).isEmpty();
        assertThat(capacityField().getValue()).isNull();
        assertThat(equipmentField().getValue()).isEmpty();
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-001", "BR-003", "BR-004", "BR-005"})
    void saving_a_valid_room_stores_it_and_shows_it_in_the_overview() {
        navigate(RegisterRoomView.class);

        fillForm(NEW_NAME, 12, "projector, whiteboard");
        save();

        // Step 8: the form closes and the new room is in the overview
        assertThat(find(Dialog.class).exists()).isFalse();
        Grid<RoomListItem> grid = grid();
        int row = rowOf(grid, NEW_NAME);
        assertThat(row).isNotNegative();
        assertThat(test(grid).getCellText(row, 1)).isEqualTo("12");
        assertThat(test(grid).getCellText(row, 2)).isEqualTo("projector, whiteboard");
        // Success postcondition, read back from the database
        assertThat(storedRooms(NEW_NAME)).containsExactly(Map.of(
                "name", NEW_NAME, "capacity", 12, "equipment", "projector, whiteboard"));
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-002"})
    void name_is_stored_without_leading_or_trailing_spaces() {
        navigate(RegisterRoomView.class);

        fillForm("   " + NEW_NAME + "  ", 6, "");
        save();

        assertThat(find(Dialog.class).exists()).isFalse();
        assertThat(jdbcClient.sql("select name from room where lower(name) like 'uc001%'")
                .query(String.class).list()).containsExactly(NEW_NAME);
        assertThat(rowOf(grid(), NEW_NAME)).isNotNegative();
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-003"})
    void equipment_is_optional() {
        navigate(RegisterRoomView.class);

        fillForm(NEW_NAME, 3, "");
        save();

        assertThat(find(Dialog.class).exists()).isFalse();
        assertThat(jdbcClient.sql("select count(*) from room where name = ?").param(NEW_NAME)
                .query(Integer.class).single()).isEqualTo(1);
        assertThat(rowOf(grid(), NEW_NAME)).isNotNegative();
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-004"})
    void capacity_of_1_and_of_50_is_accepted() {
        navigate(RegisterRoomView.class);
        fillForm("UC001 Booth", 1, "");
        save();
        assertThat(find(Dialog.class).exists()).isFalse();

        fillForm("UC001 Hall", 50, "");
        save();
        assertThat(find(Dialog.class).exists()).isFalse();

        assertThat(jdbcClient.sql("select capacity from room where name in ('UC001 Booth', 'UC001 Hall') order by capacity")
                .query(Integer.class).list()).containsExactly(1, 50);
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-005"})
    void name_of_50_and_equipment_of_200_characters_are_accepted() {
        String name = "UC001" + "x".repeat(45);
        String equipment = "e".repeat(200);
        navigate(RegisterRoomView.class);

        fillForm(name, 10, equipment);
        save();

        assertThat(find(Dialog.class).exists()).isFalse();
        assertThat(storedRooms(name)).containsExactly(Map.of(
                "name", name, "capacity", 10, "equipment", equipment));
    }

    @Test
    @UseCase(id = "UC-001")
    void registered_room_can_be_booked_by_employees() {
        navigate(RegisterRoomView.class);
        fillForm(NEW_NAME, 12, "projector, whiteboard");
        save();
        assertThat(find(Dialog.class).exists()).isFalse();

        // Success postcondition: employees can book the room (UC-002) — it is offered on the booking page
        navigate(BookRoomView.class);
        Grid<RoomListItem> rooms = grid();
        int row = rowOf(rooms, NEW_NAME);
        assertThat(row).isNotNegative();
        assertThat(test(rooms).getCellText(row, 1)).isEqualTo("12");
        assertThat(test(rooms).getCellText(row, 2)).isEqualTo("projector, whiteboard");

        // and selecting it opens the booking form for it
        rooms.select(test(rooms).getRow(row));
        assertThat(find(DateTimePicker.class).withCaption("Start time").single().isVisible()).isTrue();
        assertThat(find(DateTimePicker.class).withCaption("End time").single().isVisible()).isTrue();
    }

    // --- A1: No Room Registered Yet ---------------------------------------------------------

    @Test
    @Transactional
    @UseCase(id = "UC-001", scenario = "A1: No Room Registered Yet")
    void empty_overview_notes_that_no_room_is_registered_yet() {
        // Empty the table inside the test transaction; the rollback restores the shared test data
        jdbcClient.sql("delete from room").update();
        // The Browserless setup already opened the overview on its "" alias before the delete, and
        // navigate() would reuse that instance: start a fresh UI so the overview reads the empty table
        cleanVaadinEnvironment();
        initVaadinEnvironment();

        navigate(RegisterRoomView.class);

        Grid<RoomListItem> grid = grid();
        assertThat(test(grid).size()).isZero();
        // A1 step 1: the note says that no room is registered yet
        assertThat(grid.getEmptyStateText()).isEqualTo(RegisterRoomView.NO_ROOM_YET);
        // A1 step 2: the use case continues at step 3
        openForm();
        assertThat(find(Dialog.class).exists()).isTrue();
    }

    // --- A2: Invalid Entry ------------------------------------------------------------------

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-003"},
            requirements = {"NFR-001"})
    void missing_name_is_reported_at_the_name_field_and_entries_are_kept() {
        navigate(RegisterRoomView.class);

        fillForm("", 12, "projector");
        save();

        assertRejectedAt(nameField(), RegisterRoom.NAME_REQUIRED);
        assertThat(capacityField().isInvalid()).isFalse();
        assertThat(equipmentField().isInvalid()).isFalse();
        assertEntriesKept("", 12, "projector");
        assertNothingStored();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-002", "BR-003"},
            requirements = {"NFR-001"})
    void name_of_spaces_only_counts_as_missing() {
        navigate(RegisterRoomView.class);

        fillForm("    ", 12, "");
        save();

        assertRejectedAt(nameField(), RegisterRoom.NAME_REQUIRED);
        assertThat(capacityField().isInvalid()).isFalse();
        assertEntriesKept("    ", 12, "");
        assertNothingStored();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-003"},
            requirements = {"NFR-001"})
    void missing_capacity_is_reported_at_the_capacity_field_and_entries_are_kept() {
        navigate(RegisterRoomView.class);

        fillForm(NEW_NAME, null, "whiteboard");
        save();

        assertRejectedAt(capacityField(), RegisterRoom.CAPACITY_REQUIRED);
        assertThat(nameField().isInvalid()).isFalse();
        assertEntriesKept(NEW_NAME, null, "whiteboard");
        assertNothingStored();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-004"},
            requirements = {"NFR-001"})
    void capacity_of_0_is_rejected_at_the_capacity_field() {
        navigate(RegisterRoomView.class);

        fillForm(NEW_NAME, 0, "whiteboard");
        save();

        assertRejectedAt(capacityField(), RegisterRoom.CAPACITY_OUT_OF_RANGE);
        assertThat(nameField().isInvalid()).isFalse();
        assertEntriesKept(NEW_NAME, 0, "whiteboard");
        assertNothingStored();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-004"},
            requirements = {"NFR-001"})
    void capacity_of_51_is_rejected_at_the_capacity_field() {
        navigate(RegisterRoomView.class);

        fillForm(NEW_NAME, 51, "whiteboard");
        save();

        assertRejectedAt(capacityField(), RegisterRoom.CAPACITY_OUT_OF_RANGE);
        assertThat(nameField().isInvalid()).isFalse();
        assertEntriesKept(NEW_NAME, 51, "whiteboard");
        assertNothingStored();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-005"},
            requirements = {"NFR-001"})
    void name_of_51_characters_is_rejected_at_the_name_field() {
        String name = "UC001" + "x".repeat(46);
        navigate(RegisterRoomView.class);

        fillForm(name, 10, "whiteboard");
        save();

        assertRejectedAt(nameField(), RegisterRoom.NAME_TOO_LONG);
        assertThat(capacityField().isInvalid()).isFalse();
        assertEntriesKept(name, 10, "whiteboard");
        assertNothingStored();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-005"},
            requirements = {"NFR-001"})
    void equipment_of_201_characters_is_rejected_at_the_equipment_field() {
        String equipment = "e".repeat(201);
        navigate(RegisterRoomView.class);

        fillForm(NEW_NAME, 10, equipment);
        save();

        assertRejectedAt(equipmentField(), RegisterRoom.EQUIPMENT_TOO_LONG);
        assertThat(nameField().isInvalid()).isFalse();
        assertThat(capacityField().isInvalid()).isFalse();
        assertEntriesKept(NEW_NAME, 10, equipment);
        assertNothingStored();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-003", "BR-004"},
            requirements = {"NFR-001"})
    void each_offending_field_is_reported_at_once() {
        navigate(RegisterRoomView.class);

        fillForm("", 99, "projector");
        save();

        // A2 step 1: the system reports at each offending field
        assertRejectedAt(nameField(), RegisterRoom.NAME_REQUIRED);
        assertRejectedAt(capacityField(), RegisterRoom.CAPACITY_OUT_OF_RANGE);
        assertThat(equipmentField().isInvalid()).isFalse();
        assertEntriesKept("", 99, "projector");
        assertNothingStored();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-003"},
            requirements = {"NFR-001"})
    void corrected_entry_is_saved_after_a_rejection() {
        navigate(RegisterRoomView.class);
        fillForm("", 12, "projector");
        save();
        assertRejectedAt(nameField(), RegisterRoom.NAME_REQUIRED);

        // A2 step 3: the use case continues at step 5
        test(nameField()).setValue(NEW_NAME);
        save();

        assertThat(find(Dialog.class).exists()).isFalse();
        assertThat(storedRooms(NEW_NAME)).containsExactly(Map.of(
                "name", NEW_NAME, "capacity", 12, "equipment", "projector"));
    }

    // --- A3: Room Name Already Taken ------------------------------------------------------

    @Test
    @UseCase(id = "UC-001", scenario = "A3: Room Name Already Taken", businessRules = {"BR-001"},
            requirements = {"NFR-001"})
    void taken_name_is_reported_at_the_name_field_and_entries_are_kept() {
        navigate(RegisterRoomView.class);

        fillForm("Lakeview", 30, "UC001 beamer");
        save();

        assertRejectedAt(nameField(), RoomNameTakenException.MESSAGE);
        assertThat(capacityField().isInvalid()).isFalse();
        assertEntriesKept("Lakeview", 30, "UC001 beamer");
        // Failure postconditions: no room stored, the registered room unchanged, no duplicate name
        assertLakeviewUnchanged();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A3: Room Name Already Taken", businessRules = {"BR-001"},
            requirements = {"NFR-001"})
    void name_is_compared_without_outer_spaces_and_case() {
        navigate(RegisterRoomView.class);

        // BR-001: "Lakeview" and " lakeview " are the same name
        fillForm(" lakeview ", 30, "UC001 beamer");
        save();

        assertRejectedAt(nameField(), RoomNameTakenException.MESSAGE);
        assertEntriesKept(" lakeview ", 30, "UC001 beamer");
        assertLakeviewUnchanged();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A3: Room Name Already Taken", businessRules = {"BR-001"},
            requirements = {"NFR-001"})
    void name_taken_while_the_form_is_open_is_rejected() {
        navigate(RegisterRoomView.class);
        fillForm(NEW_NAME, 12, "projector");

        // Competing change: another office manager registers the name while this one enters the data
        jdbcClient.sql("insert into room (name, capacity, equipment) values (?, 40, null)")
                .param(NEW_NAME.toUpperCase()).update();
        save();

        assertRejectedAt(nameField(), RoomNameTakenException.MESSAGE);
        assertEntriesKept(NEW_NAME, 12, "projector");
        // No two rooms have the same name; the competitor's room is unchanged
        assertThat(jdbcClient.sql("select capacity from room where lower(name) = lower(?)").param(NEW_NAME)
                .query(Integer.class).list()).containsExactly(40);
    }

    // --- A4: Registration Cancelled -------------------------------------------------------

    @Test
    @UseCase(id = "UC-001", scenario = "A4: Registration Cancelled")
    void cancel_discards_the_entries_and_shows_the_overview() {
        navigate(RegisterRoomView.class);

        fillForm(NEW_NAME, 12, "projector");
        test(find(Button.class).withText("Cancel").single()).click();

        assertThat(find(Dialog.class).exists()).isFalse();
        assertThat(getCurrentView()).isInstanceOf(RegisterRoomView.class);
        assertThat(rowOf(grid(), NEW_NAME)).isNegative();
        assertNothingStored();

        // The entries are discarded: a new registration starts with an empty form
        openForm();
        assertThat(nameField().getValue()).isEmpty();
        assertThat(capacityField().getValue()).isNull();
        assertThat(equipmentField().getValue()).isEmpty();
    }

    // --- Helpers ----------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Grid<RoomListItem> grid() {
        return find(Grid.class).id("room-grid");
    }

    private int rowOf(Grid<RoomListItem> grid, String name) {
        var tester = test(grid);
        for (int i = 0; i < tester.size(); i++) {
            if (name.equals(tester.getCellText(i, 0))) {
                return i;
            }
        }
        return -1;
    }

    private void openForm() {
        test(find(Button.class).withText("Register Room").single()).click();
    }

    private void save() {
        test(find(Button.class).withText("Save").single()).click();
    }

    private void fillForm(String name, Integer capacity, String equipment) {
        openForm();
        test(nameField()).setValue(name);
        if (capacity != null) {
            test(capacityField()).setValue(capacity);
        }
        test(equipmentField()).setValue(equipment);
    }

    private TextField nameField() {
        return find(TextField.class).withCaption("Name").single();
    }

    private IntegerField capacityField() {
        return find(IntegerField.class).withCaption("Capacity").single();
    }

    private TextArea equipmentField() {
        return find(TextArea.class).withCaption("Equipment").single();
    }

    private static void assertRejectedAt(com.vaadin.flow.component.HasValidation field, String expectedMessage) {
        assertThat(field.isInvalid()).isTrue();
        // A2/A3 step 1: the field says what is wrong with it, the message of the violated rule
        // (OQ-002: the specification fixes no wording, so the rule's message constant is the expected text)
        assertThat(field.getErrorMessage()).isEqualTo(expectedMessage);
    }

    private void assertEntriesKept(String name, Integer capacity, String equipment) {
        // A2/A3 step 2, NFR-001: the form stays open with every entry, including the rejected ones
        assertThat(find(Dialog.class).exists()).isTrue();
        assertThat(nameField().getValue()).isEqualTo(name);
        assertThat(capacityField().getValue()).isEqualTo(capacity);
        assertThat(equipmentField().getValue()).isEqualTo(equipment);
    }

    private void assertNothingStored() {
        assertThat(jdbcClient.sql("select count(*) from room where lower(name) like 'uc001%'")
                .query(Integer.class).single()).isZero();
    }

    private void assertLakeviewUnchanged() {
        assertThat(storedRooms("Lakeview")).containsExactly(Map.of(
                "name", "Lakeview", "capacity", 8, "equipment", "projector, whiteboard"));
        assertThat(jdbcClient.sql("select count(*) from room where equipment = 'UC001 beamer'")
                .query(Integer.class).single()).isZero();
    }

    private List<Map<String, Object>> storedRooms(String name) {
        // Every room whose name equals this one in the sense of BR-001
        return jdbcClient.sql("select name, capacity, equipment from room where lower(name) = lower(?)")
                .param(name).query().listOfRows();
    }
}
