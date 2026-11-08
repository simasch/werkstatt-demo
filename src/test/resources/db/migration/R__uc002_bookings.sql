-- UC-002 Book Room: shared test data, an existing booking of "Atrium" against which overlap tests run.
INSERT INTO booking (room_id, booked_by, starts_at, ends_at, purpose)
SELECT id, 'Existing Booker', '2030-06-01 09:00:00', '2030-06-01 10:00:00', 'Standup'
FROM room
WHERE name = 'Atrium';
