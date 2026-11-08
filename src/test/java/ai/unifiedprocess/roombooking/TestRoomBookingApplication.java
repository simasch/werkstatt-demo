package ai.unifiedprocess.roombooking;

import org.springframework.boot.SpringApplication;

/** Local development entry point for {@code ./mvnw spring-boot:test-run}: the application on a Testcontainers PostgreSQL. */
public class TestRoomBookingApplication {

    public static void main(String[] args) {
        SpringApplication.from(RoomBookingApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }

}
