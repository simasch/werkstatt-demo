package ai.unifiedprocess.roombooking.room.service;

import ai.unifiedprocess.roombooking.room.domain.Room;
import ai.unifiedprocess.roombooking.room.domain.RoomRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * Registration and overview of the office's meeting rooms.
 */
@Service
@Validated
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository repository;

    public RoomService(RoomRepository repository) {
        this.repository = repository;
    }

    /**
     * UC-001 step 2: lists the rooms, sorted by name.
     */
    public List<Room> list() {
        return repository.findAll();
    }

    /**
     * UC-001 steps 5-7: registers a room; A1 when the name is already used.
     */
    @Transactional
    public Room register(@Valid RegisterRoom command) {
        // UC-001 BR-002: No two rooms have the same name, compared case-insensitively.
        if (repository.existsByNameIgnoreCase(command.name())) {
            throw new RoomNameAlreadyExistsException(command.name());
        }
        Room room = new Room(null, command.name(), command.capacity(), command.equipment());
        try {
            long id = repository.insert(room);
            return new Room(id, room.name(), room.capacity(), room.equipment());
        } catch (DuplicateKeyException e) {
            // a concurrent registration of the same name passed the guard clause above
            throw new RoomNameAlreadyExistsException(command.name());
        }
    }
}
