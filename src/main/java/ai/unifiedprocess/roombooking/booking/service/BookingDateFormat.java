package ai.unifiedprocess.roombooking.booking.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** The format in which messages of the booking show a date and time. */
public final class BookingDateFormat {

    // UC-002 BR-004: Messages show a date and time as yyyy-MM-dd HH:mm.
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private BookingDateFormat() {
    }

    /** The date and time as {@code yyyy-MM-dd HH:mm}, for example 2026-11-09 14:00. */
    public static String format(LocalDateTime dateTime) {
        return FORMAT.format(dateTime);
    }
}
