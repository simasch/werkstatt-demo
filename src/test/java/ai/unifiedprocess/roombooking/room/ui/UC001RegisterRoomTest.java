package ai.unifiedprocess.roombooking.room.ui;

import ai.unifiedprocess.roombooking.TestcontainersConfiguration;
import ai.unifiedprocess.roombooking.usecase.UseCase;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Use case test for UC-001 "Register Room". The view gets its rooms from RoomService/RoomRepository; the test
 * drives the view only and seeds data through the Flyway test migration (R__uc001_rooms.sql: "Atrium" capacity 8
 * with equipment, "Boardroom" capacity 4 without), never through the service or the repository.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UC001RegisterRoomTest extends SpringBrowserlessTest {

    // Every room name a test may create or attempt to create; never a name of the shared test data
    private static final String[] CREATED_NAMES = {
            "Conference Room C", "Conference Room D", "Trimmed Room", "Cancelled Room", "ATRIUM",
            "Missing Capacity Room", "Oversized Room"
    };

    @Autowired
    private JdbcClient jdbcClient;

    @AfterEach
    void removeCreatedRooms() {
        for (String name : CREATED_NAMES) {
            jdbcClient.sql("delete from room where name = ?").param(name).update();
        }
    }

    @Test
    @UseCase(id = "UC-001")
    void grid_lists_the_rooms_sorted_by_name() {
        navigate(RoomsView.class);

        // Step 2: the list of rooms with name, capacity, and equipment, sorted by name
        Grid<?> grid = find(Grid.class).single();
        assertThat(test(grid).getCellText(0, 0)).isEqualTo("Atrium");
        assertThat(test(grid).getCellText(0, 1)).isEqualTo("8");
        assertThat(test(grid).getCellText(0, 2)).isEqualTo("Projector, Whiteboard");
        assertThat(test(grid).getCellText(1, 0)).isEqualTo("Boardroom");
        assertThat(test(grid).getCellText(1, 1)).isEqualTo("4");
    }

    @Test
    @UseCase(id = "UC-001")
    void registering_a_room_shows_the_message_and_lists_the_room() {
        navigate(RoomsView.class);

        openNewRoomDialog();
        test(find(TextField.class).withCaption("Name").single()).setValue("Conference Room C");
        test(find(IntegerField.class).withCaption("Capacity").single()).setValue(12);
        test(find(TextField.class).withCaption("Equipment").single()).setValue("Projector");
        test(find(Button.class).withText("Save").single()).click();

        // Step 7: the exact message the specification states
        Notification notification = find(Notification.class).single();
        assertThat(test(notification).getText()).isEqualTo("Room Conference Room C registered.");
        // Postcondition: the room is stored with its name, capacity, and equipment — read back from the database
        assertThat(jdbcClient.sql("select capacity, equipment from room where name = ?")
                .param("Conference Room C")
                .query((rs, rowNum) -> rs.getInt(1) + "/" + rs.getString(2))
                .list()).containsExactly("12/Projector");
        // Postcondition: the room is shown in the list of rooms
        assertThat(find(Dialog.class).exists()).isFalse();
        assertThat(rowOf(find(Grid.class).single(), "Conference Room C")).isNotNegative();
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-004"})
    void registering_a_room_without_equipment_succeeds() {
        navigate(RoomsView.class);

        openNewRoomDialog();
        test(find(TextField.class).withCaption("Name").single()).setValue("Conference Room D");
        test(find(IntegerField.class).withCaption("Capacity").single()).setValue(6);
        test(find(Button.class).withText("Save").single()).click();

        // BR-004: equipment is optional
        Notification notification = find(Notification.class).single();
        assertThat(test(notification).getText()).isEqualTo("Room Conference Room D registered.");
        assertThat(jdbcClient.sql("select capacity from room where name = ?")
                .param("Conference Room D").query(Integer.class).list()).containsExactly(6);
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-001"})
    void room_name_is_trimmed_before_it_is_stored() {
        navigate(RoomsView.class);

        openNewRoomDialog();
        test(find(TextField.class).withCaption("Name").single()).setValue("  Trimmed Room  ");
        test(find(IntegerField.class).withCaption("Capacity").single()).setValue(3);
        test(find(Button.class).withText("Save").single()).click();

        // BR-001: leading and trailing spaces are removed before the name is checked and stored
        Notification notification = find(Notification.class).single();
        assertThat(test(notification).getText()).isEqualTo("Room Trimmed Room registered.");
        assertThat(jdbcClient.sql("select name from room where name = ?")
                .param("Trimmed Room").query(String.class).list()).containsExactly("Trimmed Room");
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A3: Registration Cancelled")
    void cancelling_the_dialog_stores_nothing() {
        navigate(RoomsView.class);

        openNewRoomDialog();
        test(find(TextField.class).withCaption("Name").single()).setValue("Cancelled Room");
        test(find(IntegerField.class).withCaption("Capacity").single()).setValue(5);
        test(find(Button.class).withText("Cancel").single()).click();

        // Failure postconditions: no room is stored, the list of rooms is unchanged
        assertThat(find(Dialog.class).exists()).isFalse();
        assertThat(jdbcClient.sql("select count(*) from room where name = ?")
                .param("Cancelled Room").query(Long.class).single()).isZero();
        assertThat(rowOf(find(Grid.class).single(), "Cancelled Room")).isNegative();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-001"}, requirements = {"NFR-001"})
    void empty_name_is_rejected_and_keeps_the_other_entries() {
        navigate(RoomsView.class);

        openNewRoomDialog();
        IntegerField capacityField = find(IntegerField.class).withCaption("Capacity").single();
        test(capacityField).setValue(7);
        test(find(Button.class).withText("Save").single()).click();

        // BR-001: an empty name is rejected with this exact message
        TextField nameField = find(TextField.class).withCaption("Name").single();
        assertThat(nameField.isInvalid()).isTrue();
        assertThat(nameField.getErrorMessage()).isEqualTo("Name is required.");
        // NFR-001: every other entry of the form is kept
        assertThat(capacityField.getValue()).isEqualTo(7);
        assertThat(find(Dialog.class).exists()).isTrue();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A1: Room Name Already Used", businessRules = {"BR-002"}, requirements = {"NFR-001"})
    void name_already_used_case_insensitively_is_rejected_and_keeps_the_other_entries() {
        navigate(RoomsView.class);

        openNewRoomDialog();
        test(find(TextField.class).withCaption("Name").single()).setValue("ATRIUM");
        IntegerField capacityField = find(IntegerField.class).withCaption("Capacity").single();
        test(capacityField).setValue(9);
        test(find(Button.class).withText("Save").single()).click();

        // BR-002: "Atrium" already exists, compared without regard to upper and lower case
        TextField nameField = find(TextField.class).withCaption("Name").single();
        assertThat(nameField.isInvalid()).isTrue();
        assertThat(nameField.getErrorMessage()).isEqualTo("A room named ATRIUM already exists.");
        // NFR-001 / A1: every entry of the form is kept
        assertThat(capacityField.getValue()).isEqualTo(9);
        assertThat(jdbcClient.sql("select count(*) from room where name = ?")
                .param("ATRIUM").query(Long.class).single()).isZero();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-003"})
    void missing_capacity_is_rejected() {
        navigate(RoomsView.class);

        openNewRoomDialog();
        test(find(TextField.class).withCaption("Name").single()).setValue("Missing Capacity Room");
        test(find(Button.class).withText("Save").single()).click();

        // BR-003: a missing capacity is rejected with this exact message
        IntegerField capacityField = find(IntegerField.class).withCaption("Capacity").single();
        assertThat(capacityField.isInvalid()).isTrue();
        assertThat(capacityField.getErrorMessage()).isEqualTo("Capacity must be between 1 and 50.");
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-003"})
    void capacity_above_50_is_rejected() {
        navigate(RoomsView.class);

        openNewRoomDialog();
        test(find(TextField.class).withCaption("Name").single()).setValue("Oversized Room");
        test(find(IntegerField.class).withCaption("Capacity").single()).setValue(51);
        test(find(Button.class).withText("Save").single()).click();

        // BR-003: an out-of-range capacity is rejected with this exact message
        IntegerField capacityField = find(IntegerField.class).withCaption("Capacity").single();
        assertThat(capacityField.isInvalid()).isTrue();
        assertThat(capacityField.getErrorMessage()).isEqualTo("Capacity must be between 1 and 50.");
    }

    private void openNewRoomDialog() {
        test(find(Button.class).withText("New Room").single()).click();
    }

    /** Index of the row with this name, or -1 — reads every row. */
    private int rowOf(Grid<?> grid, String name) {
        int size = test(grid).size();
        for (int i = 0; i < size; i++) {
            if (name.equals(test(grid).getCellText(i, 0))) {
                return i;
            }
        }
        return -1;
    }
}
