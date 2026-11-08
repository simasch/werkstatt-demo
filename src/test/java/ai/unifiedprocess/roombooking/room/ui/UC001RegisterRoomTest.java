package ai.unifiedprocess.roombooking.room.ui;

import java.util.stream.IntStream;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import ai.unifiedprocess.roombooking.TestcontainersConfiguration;
import ai.unifiedprocess.roombooking.room.domain.Room;
import ai.unifiedprocess.roombooking.usecase.UseCase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-001 "Register Room". The view gets its rooms from the service layer
 * (RoomService, real RoomRepository and PostgreSQL); the test drives the view only and seeds
 * data through the Flyway test migration R__uc001_register_room.sql ("Alpha Room", "Zulu Room"),
 * never through the service. Messages come from the specification.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UC001RegisterRoomTest extends SpringBrowserlessTest {

    // The one room name these tests create; the test data holds no room with this name
    private static final String NEW_ROOM_NAME = "Boardroom One";

    @Autowired
    private JdbcClient jdbcClient;

    @AfterEach
    void removeCreatedRoom() {
        // Idempotent and targeted: a test may have failed before or after creating the room
        jdbcClient.sql("delete from room where lower(name) = lower(?)").param(NEW_ROOM_NAME).update();
    }

    @Test
    @UseCase(id = "UC-001")
    void choosing_rooms_in_the_main_menu_opens_the_room_list() {
        navigate(RoomsView.class);

        // Step 1: Office Manager chooses "Rooms" in the main menu — the @Menu(title = "Rooms")
        // entry on RoomsView, rendered as a SideNavItem by MainLayout's SideNav
        test(find(SideNav.class).single()).clickItem("Rooms");

        assertThat(getCurrentView()).isInstanceOf(RoomsView.class);
        assertThat(find(Grid.class).withId("room-grid").exists()).isTrue();
    }

    @Test
    @UseCase(id = "UC-001")
    void grid_lists_rooms_sorted_by_name() {
        navigate(RoomsView.class);

        // Expected values come from the Flyway test data, sorted by name ("Alpha Room" before
        // "Zulu Room"), never from a run of the code. Assert rows, not the total: other data may share the table.
        Grid<Room> grid = find(Grid.class).id("room-grid");
        int alpha = rowOf(grid, "Alpha Room");
        int zulu = rowOf(grid, "Zulu Room");
        assertThat(alpha).isNotNegative();
        assertThat(zulu).isNotNegative();
        assertThat(alpha).isLessThan(zulu);
        assertThat(test(grid).getCellText(alpha, 1)).isEqualTo("4");
        assertThat(test(grid).getCellText(alpha, 2)).isEqualTo("Whiteboard");
    }

    @Test
    @UseCase(id = "UC-001")
    void new_room_dialog_shows_an_empty_form() {
        navigate(RoomsView.class);

        test(find(Button.class).id("new-room-button")).click();

        assertThat(find(Dialog.class).exists()).isTrue();
        assertThat(find(TextField.class).id("room-name-field").getValue()).isEmpty();
        assertThat(find(IntegerField.class).id("room-capacity-field").getValue()).isNull();
        assertThat(find(TextField.class).id("room-equipment-field").getValue()).isEmpty();
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-001"})
    void registering_a_room_trims_the_name_stores_shows_the_message_and_lists_the_room() {
        navigate(RoomsView.class);

        openNewRoomDialog();
        // BR-001: leading and trailing spaces are removed before the name is checked and stored
        fillForm("  " + NEW_ROOM_NAME + "  ", 8, "Projector");
        test(find(Button.class).id("save-room-button")).click();

        // Main success scenario step 7: the exact message, with the trimmed name
        Notification notification = find(Notification.class).single();
        assertThat(test(notification).getText()).isEqualTo("Room " + NEW_ROOM_NAME + " registered.");
        // Postcondition: the room is stored with its name (trimmed), capacity, and equipment
        assertThat(jdbcClient.sql("select name, capacity, equipment from room where name = ?")
                .param(NEW_ROOM_NAME)
                .query((rs, rowNum) -> rs.getString("name") + "/" + rs.getInt("capacity") + "/" + rs.getString("equipment"))
                .single())
                .isEqualTo(NEW_ROOM_NAME + "/8/Projector");
        // Postcondition: the room is shown in the list of rooms; the dialog is closed
        assertThat(find(Dialog.class).exists()).isFalse();
        assertThat(rowOf(find(Grid.class).id("room-grid"), NEW_ROOM_NAME)).isNotNegative();
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-004"})
    void registering_a_room_without_equipment_is_allowed() {
        navigate(RoomsView.class);

        openNewRoomDialog();
        fillForm(NEW_ROOM_NAME, 6, "");
        test(find(Button.class).id("save-room-button")).click();

        Notification notification = find(Notification.class).single();
        assertThat(test(notification).getText()).isEqualTo("Room " + NEW_ROOM_NAME + " registered.");
        assertThat(jdbcClient.sql("select capacity from room where name = ?")
                .param(NEW_ROOM_NAME).query(Integer.class).single()).isEqualTo(6);
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A1: Room Name Already Used", businessRules = {"BR-002"})
    void registering_a_room_with_a_name_already_used_shows_error_and_keeps_entries() {
        navigate(RoomsView.class);

        openNewRoomDialog();
        // BR-002: "Alpha Room" is registered by the test data
        fillForm("Alpha Room", 9, "Flipchart");
        test(find(Button.class).id("save-room-button")).click();

        TextField nameField = find(TextField.class).id("room-name-field");
        assertThat(nameField.isInvalid()).isTrue();
        assertThat(nameField.getErrorMessage()).isEqualTo("A room named Alpha Room already exists.");
        // The form keeps every entry
        assertThat(nameField.getValue()).isEqualTo("Alpha Room");
        assertThat(find(IntegerField.class).id("room-capacity-field").getValue()).isEqualTo(9);
        assertThat(find(TextField.class).id("room-equipment-field").getValue()).isEqualTo("Flipchart");
        // Failure postcondition: no room is stored, the dialog stays open
        assertThat(find(Dialog.class).exists()).isTrue();
        assertThat(jdbcClient.sql("select count(*) from room where equipment = ?")
                .param("Flipchart").query(Long.class).single()).isZero();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A1: Room Name Already Used", businessRules = {"BR-002"})
    void registering_a_room_with_a_name_used_in_another_case_is_rejected() {
        navigate(RoomsView.class);

        openNewRoomDialog();
        // BR-002: the comparison ignores upper and lower case; "ZULU ROOM" collides with "Zulu Room"
        fillForm("ZULU ROOM", 5, "");
        test(find(Button.class).id("save-room-button")).click();

        TextField nameField = find(TextField.class).id("room-name-field");
        assertThat(nameField.isInvalid()).isTrue();
        assertThat(nameField.getErrorMessage()).isEqualTo("A room named ZULU ROOM already exists.");
        assertThat(jdbcClient.sql("select count(*) from room where name = ?")
                .param("ZULU ROOM").query(Long.class).single()).isZero();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-001"})
    void registering_a_room_with_an_empty_name_is_rejected() {
        navigate(RoomsView.class);

        openNewRoomDialog();
        fillForm("", 5, "Projector");
        test(find(Button.class).id("save-room-button")).click();

        TextField nameField = find(TextField.class).id("room-name-field");
        assertThat(nameField.isInvalid()).isTrue();
        assertThat(nameField.getErrorMessage()).isEqualTo("Name is required.");
        assertThat(find(Dialog.class).exists()).isTrue();
        // A2 / NFR-001: every other entry of the form is kept
        assertThat(find(IntegerField.class).id("room-capacity-field").getValue()).isEqualTo(5);
        assertThat(find(TextField.class).id("room-equipment-field").getValue()).isEqualTo("Projector");
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-001"})
    void registering_a_room_with_a_name_longer_than_50_characters_is_rejected() {
        navigate(RoomsView.class);

        openNewRoomDialog();
        test(find(IntegerField.class).id("room-capacity-field")).setValue(5);
        test(find(TextField.class).id("room-equipment-field")).setValue("Projector");
        // A browser drops keystrokes past the field's maxLength (50 — the tester throws for this
        // reason): set the 51-character name through the component's Java API instead
        find(TextField.class).id("room-name-field").setValue("A".repeat(51));
        test(find(Button.class).id("save-room-button")).click();

        TextField nameField = find(TextField.class).id("room-name-field");
        assertThat(nameField.isInvalid()).isTrue();
        assertThat(nameField.getErrorMessage()).isEqualTo("Name must be at most 50 characters.");
        assertThat(find(Dialog.class).exists()).isTrue();
        // A2 / NFR-001: every other entry of the form is kept
        assertThat(find(IntegerField.class).id("room-capacity-field").getValue()).isEqualTo(5);
        assertThat(find(TextField.class).id("room-equipment-field").getValue()).isEqualTo("Projector");
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-003"})
    void registering_a_room_with_a_missing_capacity_is_rejected() {
        navigate(RoomsView.class);

        openNewRoomDialog();
        test(find(TextField.class).id("room-name-field")).setValue(NEW_ROOM_NAME);
        test(find(TextField.class).id("room-equipment-field")).setValue("Projector");
        test(find(Button.class).id("save-room-button")).click();

        IntegerField capacityField = find(IntegerField.class).id("room-capacity-field");
        assertThat(capacityField.isInvalid()).isTrue();
        assertThat(capacityField.getErrorMessage()).isEqualTo("Capacity must be between 1 and 50.");
        assertThat(find(Dialog.class).exists()).isTrue();
        assertThat(jdbcClient.sql("select count(*) from room where name = ?")
                .param(NEW_ROOM_NAME).query(Long.class).single()).isZero();
        // A2 / NFR-001: every other entry of the form is kept
        assertThat(find(TextField.class).id("room-name-field").getValue()).isEqualTo(NEW_ROOM_NAME);
        assertThat(find(TextField.class).id("room-equipment-field").getValue()).isEqualTo("Projector");
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-003"})
    void registering_a_room_with_an_out_of_range_capacity_is_rejected() {
        navigate(RoomsView.class);

        openNewRoomDialog();
        test(find(TextField.class).id("room-name-field")).setValue(NEW_ROOM_NAME);
        test(find(TextField.class).id("room-equipment-field")).setValue("Projector");
        // A browser commits a value outside min/max and marks the field invalid rather than blocking
        // the keystrokes, so the tester does the same
        test(find(IntegerField.class).id("room-capacity-field")).setValue(51);
        test(find(Button.class).id("save-room-button")).click();

        IntegerField capacityField = find(IntegerField.class).id("room-capacity-field");
        assertThat(capacityField.isInvalid()).isTrue();
        assertThat(capacityField.getErrorMessage()).isEqualTo("Capacity must be between 1 and 50.");
        assertThat(find(Dialog.class).exists()).isTrue();
        assertThat(jdbcClient.sql("select count(*) from room where name = ?")
                .param(NEW_ROOM_NAME).query(Long.class).single()).isZero();
        // A2 / NFR-001: every other entry of the form is kept
        assertThat(find(TextField.class).id("room-name-field").getValue()).isEqualTo(NEW_ROOM_NAME);
        assertThat(find(TextField.class).id("room-equipment-field").getValue()).isEqualTo("Projector");
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-004"})
    void registering_a_room_with_equipment_longer_than_200_characters_is_rejected() {
        navigate(RoomsView.class);

        openNewRoomDialog();
        test(find(TextField.class).id("room-name-field")).setValue(NEW_ROOM_NAME);
        test(find(IntegerField.class).id("room-capacity-field")).setValue(5);
        // A browser drops keystrokes past the field's maxLength (200): set the 201-character
        // equipment text through the component's Java API instead of the tester
        find(TextField.class).id("room-equipment-field").setValue("E".repeat(201));
        test(find(Button.class).id("save-room-button")).click();

        TextField equipmentField = find(TextField.class).id("room-equipment-field");
        assertThat(equipmentField.isInvalid()).isTrue();
        assertThat(equipmentField.getErrorMessage()).isEqualTo("Equipment must be at most 200 characters.");
        assertThat(find(Dialog.class).exists()).isTrue();
        assertThat(jdbcClient.sql("select count(*) from room where name = ?")
                .param(NEW_ROOM_NAME).query(Long.class).single()).isZero();
        // A2 / NFR-001: every other entry of the form is kept
        assertThat(find(TextField.class).id("room-name-field").getValue()).isEqualTo(NEW_ROOM_NAME);
        assertThat(find(IntegerField.class).id("room-capacity-field").getValue()).isEqualTo(5);
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A3: Registration Cancelled")
    void cancelling_the_registration_closes_the_form_without_storing() {
        navigate(RoomsView.class);

        openNewRoomDialog();
        fillForm(NEW_ROOM_NAME, 5, "");
        test(find(Button.class).id("cancel-room-button")).click();

        assertThat(find(Dialog.class).exists()).isFalse();
        assertThat(jdbcClient.sql("select count(*) from room where name = ?")
                .param(NEW_ROOM_NAME).query(Long.class).single()).isZero();
        assertThat(rowOf(find(Grid.class).id("room-grid"), NEW_ROOM_NAME)).isNegative();
    }

    private void openNewRoomDialog() {
        test(find(Button.class).id("new-room-button")).click();
    }

    private void fillForm(String name, int capacity, String equipment) {
        test(find(TextField.class).id("room-name-field")).setValue(name);
        test(find(IntegerField.class).id("room-capacity-field")).setValue(capacity);
        test(find(TextField.class).id("room-equipment-field")).setValue(equipment);
    }

    /** Index of the row with this room name, or -1 — reads every row, so it works with lazy data providers. */
    private int rowOf(Grid<Room> grid, String name) {
        return IntStream.range(0, test(grid).size())
                .filter(i -> name.equals(test(grid).getRow(i).name()))
                .findFirst()
                .orElse(-1);
    }
}
