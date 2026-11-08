package ai.unifiedprocess.roombooking.room.domain;

import static ai.unifiedprocess.roombooking.database.Tables.ROOM;

import java.util.List;
import java.util.Optional;

import org.jooq.DSLContext;
import org.jooq.Records;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

/** Data access for the {@link Room} aggregate. */
@Repository
public class RoomRepository {

    private final DSLContext ctx;

    public RoomRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    /** Every room, sorted by name without regard to upper and lower case. */
    public List<Room> findAll() {
        return ctx.select(ROOM.ID, ROOM.NAME, ROOM.CAPACITY, ROOM.EQUIPMENT)
                .from(ROOM)
                .orderBy(ROOM.NAME.lower(), ROOM.NAME, ROOM.ID)
                .fetch(Records.mapping(Room::new));
    }

    /**
     * The room with the id, locked until the end of the transaction so that writes concerning the room are
     * serialized; empty when no such room exists.
     */
    public Optional<Room> findByIdForUpdate(long id) {
        return ctx.select(ROOM.ID, ROOM.NAME, ROOM.CAPACITY, ROOM.EQUIPMENT)
                .from(ROOM)
                .where(ROOM.ID.eq(id))
                .forUpdate()
                .fetchOptional(Records.mapping(Room::new));
    }

    // UC-001 BR-002: Room names are compared without regard to upper and lower case.
    public boolean existsByName(String name) {
        return ctx.fetchExists(ROOM, ROOM.NAME.lower().eq(DSL.lower(DSL.val(name))));
    }

    /** Stores a new room and returns its generated id. */
    public long insert(Room room) {
        return ctx.insertInto(ROOM)
                .set(ROOM.NAME, room.name())
                .set(ROOM.CAPACITY, room.capacity())
                .set(ROOM.EQUIPMENT, room.equipment())
                .returning(ROOM.ID)
                .fetchSingle(ROOM.ID);
    }
}
