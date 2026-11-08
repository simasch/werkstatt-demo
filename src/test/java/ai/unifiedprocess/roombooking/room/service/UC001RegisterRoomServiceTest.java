package ai.unifiedprocess.roombooking.room.service;

import java.sql.Connection;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import ai.unifiedprocess.roombooking.TestcontainersConfiguration;
import ai.unifiedprocess.roombooking.usecase.UseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Service tests for UC-001 "Register Room" — only what the integration layer (the Browserless test
 * {@code UC001RegisterRoomTest}) cannot reach: a room registered by a competing transaction after
 * {@link RoomService#register} has checked the name and before it stores the room (step 7, BR-001).
 * Not {@code @Transactional}; every room a test creates has a name starting with "UC001" and is removed
 * afterwards.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UC001RegisterRoomServiceTest {

    private static final String NAME = "UC001 Skyline";

    @Autowired
    private RoomService roomService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcClient jdbcClient;

    @AfterEach
    void removeCreatedRooms() {
        jdbcClient.sql("delete from room where lower(name) like 'uc001%'").update();
    }

    @Test
    @UseCase(id = "UC-001", scenario = "A3: Room Name Already Taken", businessRules = {"BR-001"})
    void name_taken_by_a_competing_registration_between_check_and_insert_is_rejected() throws Exception {
        CompletableFuture<Long> registration;
        try (Connection competitor = dataSource.getConnection()) {
            // Competing office manager: registers the name (other case) but has not committed yet, so the
            // name check of the registration under test does not see it
            competitor.setAutoCommit(false);
            try (var insert = competitor.prepareStatement(
                    "insert into room (name, capacity, equipment) values (?, 40, null)")) {
                insert.setString(1, NAME.toUpperCase());
                insert.executeUpdate();
            }

            registration = CompletableFuture.supplyAsync(
                    () -> roomService.register(new RegisterRoom(NAME, 12, "projector")));

            // The registration has passed its name check and waits at the unique index for the competitor
            awaitInsertBlockedOnLock(registration);
            competitor.commit();
        }

        // Step 7 / A3 step 1: the registration is rejected because another room already has this name
        try {
            Long id = registration.get(30, TimeUnit.SECONDS);
            fail("Registration stored a second room with the same name, id " + id);
        }
        catch (ExecutionException ex) {
            assertThat(ex.getCause())
                    .isInstanceOf(RoomNameTakenException.class)
                    .hasMessage(RoomNameTakenException.MESSAGE);
        }

        // Failure postconditions: no room stored by the rejected registration, no two rooms with the same
        // name, the competitor's room unchanged
        assertThat(jdbcClient.sql("select name, capacity from room where lower(name) = lower(?)")
                .param(NAME).query().listOfRows())
                .containsExactly(java.util.Map.of("name", NAME.toUpperCase(), "capacity", 40));
    }

    private void awaitInsertBlockedOnLock(CompletableFuture<Long> registration) throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
        while (System.nanoTime() < deadline) {
            if (registration.isDone()) {
                fail("Registration finished before the competitor committed: " + registration.join());
            }
            Integer waiting = jdbcClient.sql("""
                            select count(*) from pg_stat_activity
                            where datname = current_database() and wait_event_type = 'Lock'
                              and query ilike 'insert into%room%'""")
                    .query(Integer.class).single();
            if (waiting > 0) {
                return;
            }
            Thread.sleep(50);
        }
        fail("Registration never reached the insert");
    }
}
