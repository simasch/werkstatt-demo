-- Test data of UC-001 Register Room: rooms already registered before the Office Manager opens the list.
-- Fixed ids far above what room_seq hands out; ON CONFLICT keeps the repeatable migration re-runnable.
INSERT INTO room (id, name, capacity, equipment)
VALUES (900001, 'Matterhorn', 12, 'Projector, whiteboard'),
       (900002, 'Eiger', 6, NULL),
       (900003, 'Jungfrau', 20, 'Video conferencing')
ON CONFLICT DO NOTHING;
