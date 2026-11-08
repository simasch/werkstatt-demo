-- UC-002 Book Room: test data. Fixed negative id the booking_seq sequence never hands out,
-- so this repeatable migration can run again on a database that already holds this row.
-- Depends on room -101 ("Alpha Room") inserted by R__uc001_register_room.sql
-- (UC-002 precondition: at least one room is registered, UC-001).
INSERT INTO booking (id, room_id, booked_by, starts_at, ends_at, purpose)
VALUES (-201, -101, 'Existing Booking', '2026-12-01 10:00:00', '2026-12-01 11:00:00', NULL)
ON CONFLICT (id) DO NOTHING;
