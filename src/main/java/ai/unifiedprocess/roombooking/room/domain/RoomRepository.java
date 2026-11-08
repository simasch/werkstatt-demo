package ai.unifiedprocess.roombooking.room.domain;

import static ai.unifiedprocess.roombooking.database.tables.Room.ROOM;

import java.util.List;

import org.jooq.DSLContext;
import org.jooq.Records;
import org.springframework.stereotype.Repository;

@Repository
public class RoomRepository {

    private final DSLContext ctx;

    public RoomRepository(DSLContext ctx) {
        this.ctx = ctx;
    }

    public List<Room> findAll() {
        return ctx.select(ROOM.ID, ROOM.NAME, ROOM.CAPACITY, ROOM.EQUIPMENT)
                .from(ROOM)
                .orderBy(ROOM.NAME)
                .fetch(Records.mapping(Room::new));
    }

    // UC-001 BR-002: No two rooms have the same name, compared without regard to case.
    public boolean existsByName(String name) {
        return ctx.fetchExists(ctx.selectOne()
                .from(ROOM)
                .where(ROOM.NAME.equalIgnoreCase(name)));
    }

    public long insert(Room room) {
        return ctx.insertInto(ROOM)
                .set(ROOM.NAME, room.name())
                .set(ROOM.CAPACITY, room.capacity())
                .set(ROOM.EQUIPMENT, room.equipment())
                .returning(ROOM.ID)
                .fetchOne(ROOM.ID);
    }
}
