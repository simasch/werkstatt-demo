package ai.unifiedprocess.roombooking.booking.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import ai.unifiedprocess.roombooking.booking.domain.Booking;
import ai.unifiedprocess.roombooking.booking.domain.BookingRepository;
import jakarta.validation.Valid;

/**
 * Room booking.
 */
@Service
@Validated
@Transactional(readOnly = true)
public class BookingService {

    private final BookingRepository repository;

    public BookingService(BookingRepository repository) {
        this.repository = repository;
    }

    /**
     * UC-002 steps 4–5: books the room; A1 when the time slot overlaps another booking.
     */
    @Transactional
    public Booking book(@Valid BookRoom command) {
        // UC-002 BR-001: the end must be after the start.
        if (!command.endsAt().isAfter(command.startsAt())) {
            throw new EndNotAfterStartException();
        }
        // UC-002 BR-002: two bookings of the same room never overlap.
        repository.findOverlapping(command.roomId(), command.startsAt(), command.endsAt())
                .ifPresent(overlapping -> {
                    throw new RoomAlreadyBookedException(overlapping.startsAt(), overlapping.endsAt());
                });
        Booking booking = new Booking(null, command.roomId(), command.bookedBy(), command.startsAt(),
                command.endsAt(), command.purpose());
        long id = repository.insert(booking);
        return new Booking(id, booking.roomId(), booking.bookedBy(), booking.startsAt(), booking.endsAt(),
                booking.purpose());
    }
}
