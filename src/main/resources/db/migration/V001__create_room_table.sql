CREATE SEQUENCE room_seq START WITH 1 INCREMENT BY 1 CACHE 50;

CREATE TABLE room
(
    id        BIGINT DEFAULT nextval('room_seq') PRIMARY KEY,
    -- UC-001 BR-002: The name is stored without leading or trailing spaces and is not blank.
    name      VARCHAR(50)  NOT NULL CHECK (name = btrim(name) AND name <> ''),
    -- UC-001 BR-004: The capacity is a whole number of people from 1 to 50.
    capacity  INTEGER      NOT NULL CHECK (capacity BETWEEN 1 AND 50),
    equipment VARCHAR(200)
);

-- UC-001 BR-001: Room names are unique, compared without regard to upper and lower case.
CREATE UNIQUE INDEX room_name_uk ON room (lower(name));
