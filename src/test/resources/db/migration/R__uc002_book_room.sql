-- Test data of UC-002 Book Room: a room registered before the employee opens the booking page.
-- The rooms of R__uc001_register_room.sql (Atrium, Harbour, Lakeview) are registered as well.
-- The bookings made before are inserted by UC002BookRoomTest and removed afterwards: a booking row here
-- would block the "delete from room" of UC-001 A1 (foreign key booking.room_id).
INSERT INTO room (name, capacity, equipment) VALUES ('Studio', 6, 'screen') ON CONFLICT DO NOTHING;
