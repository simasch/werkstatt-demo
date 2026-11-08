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
