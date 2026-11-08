package ai.unifiedprocess.roombooking.booking.ui;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import ai.unifiedprocess.roombooking.booking.domain.Booking;
import ai.unifiedprocess.roombooking.booking.service.BookRoom;
import ai.unifiedprocess.roombooking.booking.service.BookingDateFormat;
import ai.unifiedprocess.roombooking.booking.service.BookingService;
import ai.unifiedprocess.roombooking.booking.service.EndNotAfterStartException;
import ai.unifiedprocess.roombooking.booking.service.RoomAlreadyBookedException;
import ai.unifiedprocess.roombooking.room.domain.Room;
import ai.unifiedprocess.roombooking.room.service.RoomService;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC-002 Book Room. Shows the booking form with the fields Room, Booked by, Start, End, and Purpose, books the room
 * for the time slot, and shows the alternative flows A1 and A2 at the offending fields with every entry kept.
 */
@Route("book-room")
@PageTitle("Book Room")
@Menu(title = "Book Room", order = 2)
public class BookRoomView extends VerticalLayout {

    private final transient BookingService bookingService;

    private final ComboBox<Room> room = new ComboBox<>("Room");

    private final TextField bookedBy = new TextField("Booked by");

    private final DateTimePicker start = new DateTimePicker("Start");

    private final DateTimePicker end = new DateTimePicker("End");

    private final TextField purpose = new TextField("Purpose");

    private final Binder<BookRoom> binder = new Binder<>(BookRoom.class);

    public BookRoomView(BookingService bookingService, RoomService roomService) {
        this.bookingService = bookingService;

        room.setItems(roomService.findAll());
        room.setItemLabelGenerator(Room::name);
        room.setRequiredIndicatorVisible(true);
        bookedBy.setRequiredIndicatorVisible(true);
        start.setRequiredIndicatorVisible(true);
        end.setRequiredIndicatorVisible(true);
        start.setI18n(new DateTimePicker.DateTimePickerI18n().setRequiredErrorMessage(BookRoom.START_REQUIRED)
            .setBadInputErrorMessage(BookRoom.START_REQUIRED)
            .setIncompleteInputErrorMessage(BookRoom.START_REQUIRED));
        end.setI18n(new DateTimePicker.DateTimePickerI18n().setRequiredErrorMessage(BookRoom.END_REQUIRED)
            .setBadInputErrorMessage(BookRoom.END_REQUIRED)
            .setIncompleteInputErrorMessage(BookRoom.END_REQUIRED));

        // UC-002 BR-003: The room is required.
        binder.forField(room).asRequired(BookRoom.ROOM_REQUIRED).withConverter(new RoomToId()).bind("roomId");
        // UC-002 BR-003: Booked by required, at most 100 characters, spaces at both ends removed.
        binder.forField(bookedBy)
            .withValidator(value -> value != null && !value.strip().isEmpty(), BookRoom.BOOKED_BY_REQUIRED)
            .withValidator(value -> value.strip().length() <= 100, BookRoom.BOOKED_BY_TOO_LONG)
            .bind("bookedBy");
        // UC-002 BR-001: The start is required.
        binder.forField(start).asRequired(BookRoom.START_REQUIRED).bind("startsAt");
        // UC-002 BR-001: The end is required and after the start.
        Binder.Binding<BookRoom, ?> endBinding = binder.forField(end)
            .asRequired(BookRoom.END_REQUIRED)
            .withValidator(value -> start.getValue() == null || value.isAfter(start.getValue()),
                    BookRoom.END_NOT_AFTER_START)
            .bind("endsAt");
        start.addValueChangeListener(event -> {
            if (!end.isEmpty()) {
                endBinding.validate();
            }
        });
        // UC-002 BR-003: The purpose is optional and at most 200 characters long.
        binder.forField(purpose)
            .withValidator(value -> value == null || value.length() <= 200, BookRoom.PURPOSE_TOO_LONG)
            .bind("purpose");

        FormLayout form = new FormLayout(room, bookedBy, start, end, purpose);

        Button book = new Button("Book", event -> book());
        book.addThemeVariants(ButtonVariant.PRIMARY);

        add(form, book);
    }

    private void book() {
        BookRoom command;
        try {
            command = binder.writeRecord();
        }
        catch (ValidationException ex) {
            // A2: the binder shows the message of each broken rule at its field
            return;
        }
        try {
            Booking booking = bookingService.book(command);
            Notification notification = Notification.show("Room " + room.getValue().name() + " booked from "
                    + BookingDateFormat.format(booking.startsAt()) + " to "
                    + BookingDateFormat.format(booking.endsAt()) + ".");
            notification.addThemeVariants(NotificationVariant.SUCCESS);
        }
        catch (RoomAlreadyBookedException ex) {
            // A1: the message is shown at the fields Start and End
            showError(start, ex.getMessage());
            showError(end, ex.getMessage());
        }
        catch (EndNotAfterStartException ex) {
            showError(end, ex.getMessage());
        }
        catch (ConstraintViolationException ex) {
            for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
                HasValidation field = fieldOf(violation.getPropertyPath());
                if (field != null) {
                    showError(field, violation.getMessage());
                }
            }
        }
    }

    private HasValidation fieldOf(Path path) {
        String property = null;
        for (Path.Node node : path) {
            property = node.getName();
        }
        return switch (property == null ? "" : property) {
            case "roomId" -> room;
            case "bookedBy" -> bookedBy;
            case "startsAt" -> start;
            case "endsAt" -> end;
            case "purpose" -> purpose;
            default -> null;
        };
    }

    private static void showError(HasValidation field, String message) {
        field.setErrorMessage(message);
        field.setInvalid(true);
    }

    /** Converts the selected room into the room id of the booking. */
    private final class RoomToId implements Converter<Room, Long> {

        @Override
        public Result<Long> convertToModel(Room value, ValueContext context) {
            return Result.ok(value == null ? null : value.id());
        }

        @Override
        public Room convertToPresentation(Long value, ValueContext context) {
            return room.getListDataView()
                .getItems()
                .filter(candidate -> candidate.id().equals(value))
                .findFirst()
                .orElse(null);
        }
    }
}
