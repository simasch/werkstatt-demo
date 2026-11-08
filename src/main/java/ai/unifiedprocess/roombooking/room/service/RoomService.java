package ai.unifiedprocess.roombooking.room.service;

import java.util.List;

import ai.unifiedprocess.roombooking.room.domain.NewRoom;
import ai.unifiedprocess.roombooking.room.domain.RoomListItem;
import ai.unifiedprocess.roombooking.room.domain.RoomRepository;
import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/** Registration and overview of the meeting rooms. */
@Service
@Validated
@Transactional(readOnly = true)
public class RoomService {

    private static final String NAME_CONSTRAINT = "room_name_uk";

    private final RoomRepository repository;

    public RoomService(RoomRepository repository) {
        this.repository = repository;
    }

    /** UC-001 step 2: every registered room, ordered by name; empty for A1. */
    public List<RoomListItem> findAll() {
        return repository.findAll();
    }

    /**
     * UC-001 steps 6–7: checks the entries and stores the room in one step; returns the new id.
     * Throws {@code ConstraintViolationException} for A2 and {@link RoomNameTakenException} for A3.
     */
    @Transactional
    public long register(@Valid RegisterRoom command) {
        // UC-001 BR-001: A room name differs from every other, ignoring case.
        if (repository.existsByName(command.name())) {
            throw new RoomNameTakenException();
        }
        try {
            return repository.insert(new NewRoom(command.name(), command.capacity(), command.equipment()));
        }
        catch (DuplicateKeyException ex) {
            // UC-001 BR-001: a room registered meanwhile took the name; the unique index rejects it.
            if (String.valueOf(ex.getMostSpecificCause().getMessage()).contains(NAME_CONSTRAINT)) {
                throw new RoomNameTakenException();
            }
            throw ex;
        }
    }
}
