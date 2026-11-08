package ai.unifiedprocess.roombooking.room.ui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ai.unifiedprocess.roombooking.TestcontainersConfiguration;
import ai.unifiedprocess.roombooking.room.domain.Room;
import ai.unifiedprocess.roombooking.usecase.UseCase;
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

/**
 * Use case test for UC-001 "Register Room", the integration layer of the use case: the view runs against the real
 * service, repository, and PostgreSQL. Existing rooms come from {@code R__uc001_register_room.sql} (Eiger,
 * Jungfrau, Matterhorn); every expected value comes from the specification or that test data.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UC001RegisterRoomTest extends SpringBrowserlessTest {

    private static final String NAME_REQUIRED = "Name is required.";

    private static final String NAME_TOO_LONG = "Name must be at most 50 characters.";

    private static final String CAPACITY_RANGE = "Capacity must be between 1 and 50.";

    private static final String EQUIPMENT_TOO_LONG = "Equipment must be at most 200 characters.";

    /** A name of exactly 50 characters (BR-001 boundary). */
    private static final String NAME_50 = "Conference Room " + "X".repeat(34);

    /** Names of rooms a test may create; cleaned up after every test. */
    private final List<String> createdNames = new ArrayList<>();

    @Autowired
    private JdbcClient jdbcClient;

    @AfterEach
    void removeCreatedRooms() {
        // Targeted and idempotent: only the rooms this test may have created, never the shared test data
        for (String name : createdNames) {
            jdbcClient.sql("delete from room where lower(name) = lower(?) and id < 900000").param(name).update();
        }
    }

    // ---------------------------------------------------------------- Main Success Scenario

    @Test
    @UseCase(id = "UC-001")
    void main_menu_offers_rooms_and_leads_to_the_room_list() {
        navigate(RegisterRoomView.class);

        // Step 1: "Rooms" in the main menu
        test(find(SideNav.class).single()).clickItem("Rooms");

        assertThat(getCurrentView()).isInstanceOf(RegisterRoomView.class);
    }

    @Test
    @UseCase(id = "UC-001")
    void room_list_shows_name_capacity_and_equipment_sorted_by_name() {
        navigate(RegisterRoomView.class);

        // Step 2: the columns name, capacity, and equipment
        Grid<Room> grid = roomGrid();
        assertThat(grid.getColumns()).extracting(Grid.Column::getHeaderText)
                .containsExactly("Name", "Capacity", "Equipment");

        int eiger = rowOf("Eiger");
        int jungfrau = rowOf("Jungfrau");
        int matterhorn = rowOf("Matterhorn");
        assertThat(eiger).isNotNegative();
        // Sorted by name
        assertThat(eiger).isLessThan(jungfrau);
        assertThat(jungfrau).isLessThan(matterhorn);

        assertThat(test(grid).getCellText(matterhorn, 1)).isEqualTo("12");
        assertThat(test(grid).getCellText(matterhorn, 2)).isEqualTo("Projector, whiteboard");
        assertThat(test(grid).getCellText(jungfrau, 1)).isEqualTo("20");
        assertThat(test(grid).getCellText(jungfrau, 2)).isEqualTo("Video conferencing");
        assertThat(test(grid).getCellText(eiger, 1)).isEqualTo("6");
    }

    @Test
    @UseCase(id = "UC-001")
    void new_room_opens_an_empty_form_with_name_capacity_and_equipment() {
        navigate(RegisterRoomView.class);

        // Step 3 and 4
        click("New Room");

        assertThat(find(Dialog.class).single().isOpened()).isTrue();
        assertThat(nameField().getValue()).isEmpty();
        assertThat(capacityField().getValue()).isNull();
        assertThat(equipmentField().getValue()).isEmpty();
    }

    @Test
    @UseCase(id = "UC-001")
    void registering_a_room_stores_it_and_shows_it_in_the_list() {
        createdNames.add("Pilatus");
        navigate(RegisterRoomView.class);

        // Step 5
        click("New Room");
        test(nameField()).setValue("Pilatus");
        test(capacityField()).setValue(8);
        test(equipmentField()).setValue("Whiteboard");
        click("Save");

        // Step 7: form closed, message, room in the list
        assertThat(find(Dialog.class).exists()).isFalse();
        assertThat(test(find(Notification.class).single()).getText()).isEqualTo("Room Pilatus registered.");
        int row = rowOf("Pilatus");
        assertThat(row).isNotNegative();
        assertThat(test(roomGrid()).getCellText(row, 1)).isEqualTo("8");
        assertThat(test(roomGrid()).getCellText(row, 2)).isEqualTo("Whiteboard");
        // Success postcondition: stored with its name, capacity, and equipment
        assertThat(storedRooms("Pilatus"))
                .containsExactly(Map.of("name", "Pilatus", "capacity", 8, "equipment", "Whiteboard"));
        // Sorted by name: Matterhorn < Pilatus
        assertThat(rowOf("Matterhorn")).isLessThan(row);
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-004"})
    void registering_a_room_without_equipment_is_allowed() {
        createdNames.add("Rigi");
        navigate(RegisterRoomView.class);

        click("New Room");
        test(nameField()).setValue("Rigi");
        test(capacityField()).setValue(4);
        click("Save");

        assertThat(test(find(Notification.class).single()).getText()).isEqualTo("Room Rigi registered.");
        assertThat(rowOf("Rigi")).isNotNegative();
        assertThat(jdbcClient.sql("select capacity from room where name = 'Rigi'").query(Integer.class).list())
                .containsExactly(4);
    }

    // ---------------------------------------------------------------- BR-001

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-001"})
    void leading_and_trailing_spaces_are_removed_from_the_name() {
        createdNames.add("Titlis");
        navigate(RegisterRoomView.class);

        click("New Room");
        test(nameField()).setValue("  Titlis  ");
        test(capacityField()).setValue(10);
        click("Save");

        assertThat(test(find(Notification.class).single()).getText()).isEqualTo("Room Titlis registered.");
        assertThat(jdbcClient.sql("select name from room where lower(name) like '%titlis%'")
                .query(String.class).list()).containsExactly("Titlis");
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-001"})
    void a_name_of_50_characters_is_accepted() {
        createdNames.add(NAME_50);
        navigate(RegisterRoomView.class);

        click("New Room");
        test(nameField()).setValue(NAME_50);
        test(capacityField()).setValue(10);
        click("Save");

        assertThat(test(find(Notification.class).single()).getText()).isEqualTo("Room " + NAME_50 + " registered.");
        assertThat(storedRooms(NAME_50)).hasSize(1);
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-001"})
    void spaces_around_a_50_character_name_do_not_count_towards_the_limit() {
        createdNames.add(NAME_50);
        navigate(RegisterRoomView.class);

        click("New Room");
        test(nameField()).setValue(" " + NAME_50 + " ");
        test(capacityField()).setValue(10);
        click("Save");

        assertThat(test(find(Notification.class).single()).getText()).isEqualTo("Room " + NAME_50 + " registered.");
        assertThat(storedRooms(NAME_50)).hasSize(1);
    }

    // ---------------------------------------------------------------- A1 / BR-002

    @Test
    @UseCase(id = "UC-001", scenario = "A1: Room Name Already Used", businessRules = {"BR-002"},
            requirements = {"NFR-001"})
    void a_name_another_room_already_has_is_rejected_at_the_name_field() {
        navigate(RegisterRoomView.class);

        click("New Room");
        test(nameField()).setValue("Matterhorn");
        test(capacityField()).setValue(5);
        test(equipmentField()).setValue("Flip chart");
        click("Save");

        assertThat(nameField().isInvalid()).isTrue();
        assertThat(nameField().getErrorMessage()).isEqualTo("A room named Matterhorn already exists.");
        // Every entry of the form is kept (NFR-001), the form stays open
        assertThat(find(Dialog.class).single().isOpened()).isTrue();
        assertThat(nameField().getValue()).isEqualTo("Matterhorn");
        assertThat(capacityField().getValue()).isEqualTo(5);
        assertThat(equipmentField().getValue()).isEqualTo("Flip chart");
        // Failure postcondition: no room stored, the existing room unchanged
        assertThat(storedRooms("Matterhorn"))
                .containsExactly(Map.of("name", "Matterhorn", "capacity", 12, "equipment", "Projector, whiteboard"));
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A1: Room Name Already Used", businessRules = {"BR-002"})
    void names_are_compared_without_regard_to_upper_and_lower_case() {
        createdNames.add("EIGER");
        navigate(RegisterRoomView.class);

        click("New Room");
        test(nameField()).setValue("EIGER");
        test(capacityField()).setValue(3);
        click("Save");

        assertThat(nameField().isInvalid()).isTrue();
        assertThat(nameField().getErrorMessage()).isEqualTo("A room named EIGER already exists.");
        assertThat(jdbcClient.sql("select count(*) from room where lower(name) = 'eiger'")
                .query(Integer.class).single()).isEqualTo(1);
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A1: Room Name Already Used", businessRules = {"BR-002"})
    void after_a_rejected_name_the_actor_corrects_it_and_saves() {
        createdNames.add("Eiger North");
        navigate(RegisterRoomView.class);

        click("New Room");
        test(nameField()).setValue("Eiger");
        test(capacityField()).setValue(6);
        click("Save");
        assertThat(nameField().isInvalid()).isTrue();

        // A1 step 2: continues at step 5
        test(nameField()).setValue("Eiger North");
        click("Save");

        assertThat(find(Dialog.class).exists()).isFalse();
        assertThat(test(find(Notification.class).single()).getText()).isEqualTo("Room Eiger North registered.");
        assertThat(storedRooms("Eiger North")).hasSize(1);
    }

    // ---------------------------------------------------------------- A2: BR-001, BR-003, BR-004

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-001"}, requirements = {"NFR-001"})
    void an_empty_name_is_rejected() {
        navigate(RegisterRoomView.class);

        click("New Room");
        test(capacityField()).setValue(7);
        test(equipmentField()).setValue("Beamer");
        click("Save");

        assertThat(nameField().isInvalid()).isTrue();
        assertThat(nameField().getErrorMessage()).isEqualTo(NAME_REQUIRED);
        assertThat(capacityField().getValue()).isEqualTo(7);
        assertThat(equipmentField().getValue()).isEqualTo("Beamer");
        assertThat(find(Dialog.class).single().isOpened()).isTrue();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-001"})
    void a_name_of_only_spaces_is_rejected_as_empty() {
        navigate(RegisterRoomView.class);

        click("New Room");
        test(nameField()).setValue("   ");
        test(capacityField()).setValue(7);
        click("Save");

        assertThat(nameField().isInvalid()).isTrue();
        assertThat(nameField().getErrorMessage()).isEqualTo(NAME_REQUIRED);
        assertThat(jdbcClient.sql("select count(*) from room where btrim(name) = ''")
                .query(Integer.class).single()).isZero();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-001"}, requirements = {"NFR-001"})
    void a_name_of_51_characters_is_rejected() {
        String name51 = NAME_50 + "Y";
        createdNames.add(name51);
        navigate(RegisterRoomView.class);

        click("New Room");
        test(nameField()).setValue(name51);
        test(capacityField()).setValue(7);
        click("Save");

        assertThat(nameField().isInvalid()).isTrue();
        assertThat(nameField().getErrorMessage()).isEqualTo(NAME_TOO_LONG);
        assertThat(nameField().getValue()).isEqualTo(name51);
        assertThat(capacityField().getValue()).isEqualTo(7);
        assertThat(storedRooms(name51)).isEmpty();
        assertThat(rowOf(name51)).isNegative();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-003"}, requirements = {"NFR-001"})
    void a_missing_capacity_is_rejected() {
        createdNames.add("Pilatus");
        navigate(RegisterRoomView.class);

        click("New Room");
        test(nameField()).setValue("Pilatus");
        test(equipmentField()).setValue("Whiteboard");
        click("Save");

        assertThat(capacityField().isInvalid()).isTrue();
        assertThat(capacityField().getErrorMessage()).isEqualTo(CAPACITY_RANGE);
        assertThat(nameField().getValue()).isEqualTo("Pilatus");
        assertThat(equipmentField().getValue()).isEqualTo("Whiteboard");
        assertThat(storedRooms("Pilatus")).isEmpty();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-003"})
    void a_capacity_of_0_is_rejected() {
        assertCapacityRejected(0);
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-003"})
    void a_capacity_of_51_is_rejected() {
        assertCapacityRejected(51);
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-003"})
    void a_capacity_of_1_is_accepted() {
        assertCapacityAccepted("Pilatus", 1);
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-003"})
    void a_capacity_of_50_is_accepted() {
        assertCapacityAccepted("Rigi", 50);
    }

    @Test
    @UseCase(id = "UC-001", businessRules = {"BR-004"})
    void equipment_of_200_characters_is_accepted() {
        String equipment200 = "E".repeat(200);
        createdNames.add("Pilatus");
        navigate(RegisterRoomView.class);

        click("New Room");
        test(nameField()).setValue("Pilatus");
        test(capacityField()).setValue(8);
        test(equipmentField()).setValue(equipment200);
        click("Save");

        assertThat(test(find(Notification.class).single()).getText()).isEqualTo("Room Pilatus registered.");
        assertThat(storedRooms("Pilatus"))
                .containsExactly(Map.of("name", "Pilatus", "capacity", 8, "equipment", equipment200));
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-004"}, requirements = {"NFR-001"})
    void equipment_of_201_characters_is_rejected() {
        String equipment201 = "E".repeat(201);
        createdNames.add("Pilatus");
        navigate(RegisterRoomView.class);

        click("New Room");
        test(nameField()).setValue("Pilatus");
        test(capacityField()).setValue(8);
        test(equipmentField()).setValue(equipment201);
        click("Save");

        assertThat(equipmentField().isInvalid()).isTrue();
        assertThat(equipmentField().getErrorMessage()).isEqualTo(EQUIPMENT_TOO_LONG);
        assertThat(nameField().getValue()).isEqualTo("Pilatus");
        assertThat(capacityField().getValue()).isEqualTo(8);
        assertThat(equipmentField().getValue()).isEqualTo(equipment201);
        assertThat(storedRooms("Pilatus")).isEmpty();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-001", "BR-003", "BR-004"},
            requirements = {"NFR-001"})
    void every_offending_field_shows_the_message_of_its_rule() {
        String equipment201 = "E".repeat(201);
        navigate(RegisterRoomView.class);

        click("New Room");
        test(capacityField()).setValue(51);
        test(equipmentField()).setValue(equipment201);
        click("Save");

        assertThat(nameField().isInvalid()).isTrue();
        assertThat(nameField().getErrorMessage()).isEqualTo(NAME_REQUIRED);
        assertThat(capacityField().isInvalid()).isTrue();
        assertThat(capacityField().getErrorMessage()).isEqualTo(CAPACITY_RANGE);
        assertThat(equipmentField().isInvalid()).isTrue();
        assertThat(equipmentField().getErrorMessage()).isEqualTo(EQUIPMENT_TOO_LONG);
        assertThat(capacityField().getValue()).isEqualTo(51);
        assertThat(equipmentField().getValue()).isEqualTo(equipment201);
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A2: Invalid Entry", businessRules = {"BR-003"})
    void after_an_invalid_entry_the_actor_corrects_it_and_saves() {
        createdNames.add("Pilatus");
        navigate(RegisterRoomView.class);

        click("New Room");
        test(nameField()).setValue("Pilatus");
        test(capacityField()).setValue(0);
        click("Save");
        assertThat(capacityField().isInvalid()).isTrue();

        // A2 step 2: continues at step 5
        test(capacityField()).setValue(9);
        click("Save");

        assertThat(find(Dialog.class).exists()).isFalse();
        assertThat(test(find(Notification.class).single()).getText()).isEqualTo("Room Pilatus registered.");
        assertThat(storedRooms("Pilatus")).containsExactly(Map.of("name", "Pilatus", "capacity", 9));
    }

    // ---------------------------------------------------------------- A3

    @Test
    @UseCase(id = "UC-001", scenario = "A3: Registration Cancelled")
    void cancelling_closes_the_form_without_storing_anything() {
        createdNames.add("Pilatus");
        navigate(RegisterRoomView.class);
        int rowsBefore = test(roomGrid()).size();

        click("New Room");
        test(nameField()).setValue("Pilatus");
        test(capacityField()).setValue(8);
        test(equipmentField()).setValue("Whiteboard");
        click("Cancel");

        assertThat(find(Dialog.class).exists()).isFalse();
        assertThat(storedRooms("Pilatus")).isEmpty();
        // Failure postcondition: the list of rooms is unchanged
        assertThat(rowOf("Pilatus")).isNegative();
        assertThat(test(roomGrid()).size()).isEqualTo(rowsBefore);
    }

    // ---------------------------------------------------------------- helpers

    private void assertCapacityRejected(int capacity) {
        createdNames.add("Pilatus");
        navigate(RegisterRoomView.class);

        click("New Room");
        test(nameField()).setValue("Pilatus");
        test(capacityField()).setValue(capacity);
        click("Save");

        assertThat(capacityField().isInvalid()).isTrue();
        assertThat(capacityField().getErrorMessage()).isEqualTo(CAPACITY_RANGE);
        assertThat(nameField().getValue()).isEqualTo("Pilatus");
        assertThat(capacityField().getValue()).isEqualTo(capacity);
        assertThat(storedRooms("Pilatus")).isEmpty();
        assertThat(rowOf("Pilatus")).isNegative();
    }

    private void assertCapacityAccepted(String name, int capacity) {
        createdNames.add(name);
        navigate(RegisterRoomView.class);

        click("New Room");
        test(nameField()).setValue(name);
        test(capacityField()).setValue(capacity);
        click("Save");

        assertThat(test(find(Notification.class).single()).getText()).isEqualTo("Room " + name + " registered.");
        assertThat(storedRooms(name)).containsExactly(Map.of("name", name, "capacity", capacity));
    }

    /** The stored rooms with exactly this name; equipment only when it is not empty. */
    private List<Map<String, Object>> storedRooms(String name) {
        return jdbcClient.sql("select name, capacity, equipment from room where name = ?")
                .param(name)
                .query((rs, i) -> {
                    String equipment = rs.getString("equipment");
                    return equipment == null || equipment.isEmpty()
                            ? Map.<String, Object>of("name", rs.getString("name"), "capacity", rs.getInt("capacity"))
                            : Map.<String, Object>of("name", rs.getString("name"), "capacity", rs.getInt("capacity"),
                                    "equipment", equipment);
                })
                .list();
    }

    @SuppressWarnings("unchecked")
    private Grid<Room> roomGrid() {
        return find(Grid.class).id("room-grid");
    }

    private int rowOf(String name) {
        Grid<Room> grid = roomGrid();
        for (int i = 0; i < test(grid).size(); i++) {
            if (test(grid).getRow(i).name().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private void click(String text) {
        test(find(Button.class).withText(text).single()).click();
    }

    private TextField nameField() {
        return find(TextField.class).withCaption("Name").single();
    }

    private IntegerField capacityField() {
        return find(IntegerField.class).withCaption("Capacity").single();
    }

    private TextField equipmentField() {
        return find(TextField.class).withCaption("Equipment").single();
    }
}
