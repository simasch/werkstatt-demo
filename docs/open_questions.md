# Open Questions

Questions about the specification that the specification does not answer yet. A question is `Resolved` only when
the answer is in the specification itself; **Resolved in** names where. Resolved and withdrawn questions stay as
history.

## OQ-001: In which order does the room overview list the rooms?

**Element:** UC-001 step 2
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Open
**Assumed:** Ordered by name, alphabetically.

FR-003 asks for every room with its capacity and equipment but names no order. Readings: (a) by name, so a room is
found as at the door; (b) by capacity, so the room for a group is found quickly; (c) in the order of registration.

## OQ-002: Which wording do the field errors and the empty-overview note use?

**Element:** UC-001 A1, A2, A3
**Raised by:** /vaadin-flow-ui-test, 2026-11-08
**Blocking:** No
**Status:** Open
**Assumed:** No fixed wording; the tests assert that the error is shown at the offending field (and only there),
that a message is present, and that the empty overview shows a note.

A1 names "the note that no room is registered yet", A2 "what is wrong" with each field, and A3 "that another room
already has this name", but none gives the text. Readings: (a) the wording is free as long as it says what the
flow names; (b) the specification fixes each message, e.g. "Another room already has this name." for A3 and one
message per rule of BR-003 to BR-005 for A2, so that the tests can assert the full text.

## OQ-003: In which order does the booking page list the rooms?

**Element:** UC-002 step 2
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Open
**Assumed:** Ordered by name, alphabetically, the same as the room overview (UC-001 step 2).

FR-004 asks that an employee can book a room but names no order for the room list the booking page shows.
Readings: (a) by name, so a room is found as at the door, consistent with UC-001; (b) by capacity; (c) by how many
free time slots it still has.

## OQ-004: Which wording do the field errors, the overlap message, and the empty-room-list note use?

**Element:** UC-002 A1, A2, A3
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Open
**Assumed:** No fixed wording; the tests assert that the error is shown at the offending field (and only there) for
A2, that a message is present for A3, and that the empty room list shows a note for A1.

A1 names "the note that no room is registered yet", A2 "what is wrong" with each field, and A3 that the room "is
already booked for an overlapping time slot", but none gives the text, consistent with OQ-002 of UC-001.

## OQ-005: Does a booking that starts exactly when another one of the same room ends count as overlapping?

**Element:** UC-002 BR-002
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Open
**Assumed:** No — back-to-back bookings do not overlap; the time span of a booking is inclusive of its start time
and exclusive of its end time.

FR-005 asks that an overlapping booking of the same room is rejected but does not say whether two bookings that
touch at the boundary (one ends at 10:00, the next starts at 10:00) collide. Readings: (a) touching bookings do not
overlap, the common scheduling convention, allowing back-to-back meetings; (b) touching bookings do overlap, leaving
a gap between any two bookings of a room.

## OQ-006: May an employee book a room for a time slot that starts in the past?

**Element:** UC-002 (scope)
**Raised by:** /use-case-spec, 2026-11-08
**Blocking:** No
**Status:** Open
**Assumed:** No restriction; a booking may be made for a past, present, or future time slot, since neither
FR-004/FR-005 nor the entity model restricts it.

Neither the requirements nor the entity model say whether a booking's start time must be in the present or future.
Readings: (a) no restriction, keeping the use case as small as the sources describe; (b) the system rejects a start
time in the past, which would need a new business rule and alternative flow not grounded in any current source.
