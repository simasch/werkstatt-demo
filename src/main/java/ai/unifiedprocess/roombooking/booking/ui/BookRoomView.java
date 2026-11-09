package ai.unifiedprocess.roombooking.booking.ui;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import ai.unifiedprocess.roombooking.booking.domain.BookedTimeSlot;
import ai.unifiedprocess.roombooking.booking.service.BookRoom;
import ai.unifiedprocess.roombooking.booking.service.BookingEndNotAfterStartException;
import ai.unifiedprocess.roombooking.booking.service.BookingService;
import ai.unifiedprocess.roombooking.booking.service.RoomAlreadyBookedException;
import ai.unifiedprocess.roombooking.room.domain.RoomListItem;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

/** UC-002 Book Room: the room list, the booking form, and the room's booked time slots. */
@Route("booking")
@PageTitle("Book Room")
@Menu(title = "Book Room", order = 2)
public class BookRoomView extends VerticalLayout {

    public static final String NO_ROOM_YET = "No room is registered yet.";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BookingService bookingService;

    private final VerticalLayout roomSection = new VerticalLayout();

    private final Grid<RoomListItem> roomGrid = new Grid<>();

    private final VerticalLayout bookingSection = new VerticalLayout();

    private final H3 bookingTitle = new H3();

    private final Grid<BookedTimeSlot> slotGrid = new Grid<>();

    private final TextField bookedBy = new TextField("Name");

    private final DateTimePicker startsAt = new DateTimePicker("Start time");

    private final DateTimePicker endsAt = new DateTimePicker("End time");

    private final TextArea purpose = new TextArea("Purpose");

    private final Binder<BookingEntries> binder = new Binder<>(BookingEntries.class);

    private RoomListItem selectedRoom;

    public BookRoomView(BookingService bookingService) {
        this.bookingService = bookingService;
        setSizeFull();

        buildRoomSection();
        buildBookingSection();
        add(new H2("Book Room"), roomSection, bookingSection);
        showRoomList();
    }

    private void buildRoomSection() {
        roomGrid.setId("room-grid");
        roomGrid.addColumn(RoomListItem::name).setHeader("Name").setKey("name");
        roomGrid.addColumn(RoomListItem::capacity).setHeader("Capacity").setKey("capacity");
        roomGrid.addColumn(RoomListItem::equipment).setHeader("Equipment").setKey("equipment");
        // UC-002 A1: an empty room list notes that no room is registered yet.
        roomGrid.setEmptyStateText(NO_ROOM_YET);
        // UC-002 step 3: the employee selects a room to book.
        roomGrid.asSingleSelect().addValueChangeListener(event -> {
            if (event.getValue() != null) {
                showBookingForm(event.getValue());
            }
        });
        roomSection.setPadding(false);
        roomSection.setSizeFull();
        roomSection.add(new Paragraph("Select a room to book."), roomGrid);
    }

    private void buildBookingSection() {
        slotGrid.setId("booked-time-slot-grid");
        slotGrid.addColumn(slot -> format(slot.startsAt())).setHeader("Start time").setKey("startsAt");
        slotGrid.addColumn(slot -> format(slot.endsAt())).setHeader("End time").setKey("endsAt");
        slotGrid.addColumn(BookedTimeSlot::bookedBy).setHeader("Name").setKey("bookedBy");
        slotGrid.addColumn(BookedTimeSlot::purpose).setHeader("Purpose").setKey("purpose");
        slotGrid.setEmptyStateText("The room has no booked time slots yet.");
        slotGrid.setAllRowsVisible(true);

        bookedBy.setRequiredIndicatorVisible(true);
        startsAt.setRequiredIndicatorVisible(true);
        endsAt.setRequiredIndicatorVisible(true);
        purpose.setHelperText("For example: Weekly sync");

        binder.forField(bookedBy)
            // UC-002 BR-003: The employee's name is required; spaces only count as missing.
            .withValidator(value -> value != null && !value.isBlank(), BookRoom.NAME_REQUIRED)
            // UC-002 BR-004: The employee's name has at most 100 characters.
            .withValidator(value -> value.length() <= 100, BookRoom.NAME_TOO_LONG)
            .bind(BookingEntries::getBookedBy, BookingEntries::setBookedBy);
        binder.forField(startsAt)
            // UC-002 BR-003: The start time is required.
            .asRequired(BookRoom.START_REQUIRED)
            .bind(BookingEntries::getStartsAt, BookingEntries::setStartsAt);
        Binder.Binding<BookingEntries, LocalDateTime> endsAtBinding = binder.forField(endsAt)
            // UC-002 BR-003: The end time is required.
            .asRequired(BookRoom.END_REQUIRED)
            // UC-002 BR-001: A booking's end time must be after its start time.
            .withValidator(value -> startsAt.getValue() == null || value.isAfter(startsAt.getValue()),
                    BookingEndNotAfterStartException.MESSAGE)
            .bind(BookingEntries::getEndsAt, BookingEntries::setEndsAt);
        binder.forField(purpose)
            // UC-002 BR-004: The purpose has at most 200 characters.
            .withValidator(value -> value == null || value.length() <= 200, BookRoom.PURPOSE_TOO_LONG)
            .bind(BookingEntries::getPurpose, BookingEntries::setPurpose);
        startsAt.addValueChangeListener(event -> {
            if (endsAt.getValue() != null) {
                endsAtBinding.validate();
            }
        });

        FormLayout form = new FormLayout(bookedBy, startsAt, endsAt, purpose);
        form.setColspan(purpose, 2);

        Button save = new Button("Save", event -> save());
        save.addThemeVariants(ButtonVariant.PRIMARY);
        Button cancel = new Button("Cancel", event -> cancel());

        bookingSection.setPadding(false);
        bookingSection.add(bookingTitle, new H3("Booked time slots"), slotGrid, new H3("New booking"),
                form, new HorizontalLayout(cancel, save));
    }

    /** UC-002 step 2 and A4: every registered room; the booking form is hidden. */
    private void showRoomList() {
        selectedRoom = null;
        bookingSection.setVisible(false);
        roomSection.setVisible(true);
        // UC-002 step 2: every registered room, ordered by name; a short, bounded list.
        roomGrid.setItems(bookingService.findRooms());
    }

    /** UC-002 step 4: the booking form for the selected room with its booked time slots. */
    private void showBookingForm(RoomListItem room) {
        selectedRoom = room;
        bookingTitle.setText("Book " + room.name());
        clearForm();
        refreshSlots();
        roomSection.setVisible(false);
        bookingSection.setVisible(true);
    }

    private void refreshSlots() {
        slotGrid.setItems(bookingService.findBookedTimeSlots(selectedRoom.id()));
    }

    private void clearForm() {
        binder.readBean(new BookingEntries());
    }

    /** UC-002 A4: the entries are discarded and the room list is shown. */
    private void cancel() {
        clearForm();
        showRoomList();
    }

    /** UC-002 steps 6–8, A2, and A3. */
    private void save() {
        BookingEntries entries = new BookingEntries();
        try {
            binder.writeBean(entries);
        }
        catch (ValidationException ex) {
            // UC-002 A2: the binder marks each offending field; every entry stays in the form.
            return;
        }
        long bookingId;
        try {
            bookingId = bookingService.book(new BookRoom(selectedRoom.id(), entries.getBookedBy(),
                    entries.getStartsAt(), entries.getEndsAt(), entries.getPurpose()));
        }
        catch (BookingEndNotAfterStartException ex) {
            // UC-002 A2: reported at the end time; every entry stays in the form.
            showError(endsAt, ex.getMessage());
            return;
        }
        catch (RoomAlreadyBookedException ex) {
            // UC-002 A3: reported at the time slot's fields; every entry stays in the form.
            refreshSlots();
            showError(startsAt, ex.getMessage());
            showError(endsAt, ex.getMessage());
            return;
        }
        catch (ConstraintViolationException ex) {
            // UC-002 A2: the service rejected an entry the form let through.
            Map<String, HasValidation> fields = Map.of("bookedBy", bookedBy, "startsAt", startsAt,
                    "endsAt", endsAt, "purpose", purpose);
            for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
                HasValidation field = fields.get(lastNode(violation.getPropertyPath()));
                if (field == null) {
                    throw ex;
                }
                showError(field, violation.getMessage());
            }
            return;
        }
        // UC-002 step 8: the confirmed booking appears among the room's booked time slots.
        clearForm();
        refreshSlots();
        slotGrid.getListDataView()
            .getItems()
            .filter(slot -> slot.id() == bookingId)
            .findFirst()
            .ifPresent(slotGrid::select);
    }

    private static String format(LocalDateTime time) {
        return time.format(TIME_FORMAT);
    }

    private static String lastNode(Path path) {
        String last = null;
        for (Path.Node node : path) {
            last = node.getName();
        }
        return last;
    }

    private static void showError(HasValidation field, String message) {
        field.setErrorMessage(message);
        field.setInvalid(true);
    }

    /** The entries of the booking form while the employee edits them. */
    public static final class BookingEntries {

        private String bookedBy = "";

        private LocalDateTime startsAt;

        private LocalDateTime endsAt;

        private String purpose = "";

        public String getBookedBy() {
            return bookedBy;
        }

        public void setBookedBy(String bookedBy) {
            this.bookedBy = bookedBy;
        }

        public LocalDateTime getStartsAt() {
            return startsAt;
        }

        public void setStartsAt(LocalDateTime startsAt) {
            this.startsAt = startsAt;
        }

        public LocalDateTime getEndsAt() {
            return endsAt;
        }

        public void setEndsAt(LocalDateTime endsAt) {
            this.endsAt = endsAt;
        }

        public String getPurpose() {
            return purpose;
        }

        public void setPurpose(String purpose) {
            this.purpose = purpose;
        }
    }
}
