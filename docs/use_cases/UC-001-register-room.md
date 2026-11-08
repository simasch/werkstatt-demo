# Use Case: Register Room

## Overview

**Use Case ID:** UC-001  
**Use Case Name:** Register Room  
**Primary Actor:** Office Manager  
**Goal:** Make a new meeting room known to the system so that employees can book it.  
**Trigger:** A meeting room has been set up in the office.  
**Status:** Approved  
**Requirements:** [FR-001, FR-002, FR-003, NFR-001](../requirements.md)

## Preconditions

_None — registering a room relies on no prior state._

## Main Success Scenario

1. Office Manager chooses "Rooms" in the main menu.
2. System displays the list of rooms with name, capacity, and equipment, sorted by name.
3. Office Manager chooses "New Room".
4. System displays an empty room form with the fields "Name", "Capacity", and "Equipment".
5. Office Manager enters the name and the capacity of the room, optionally its equipment, and chooses "Save".
6. System validates the entries against BR-001 to BR-004.
7. System stores the room, closes the form, shows the message "Room <name> registered.", and displays the new room
   in the list.

## Alternative Flows

### A1: Room Name Already Used

**Trigger:** Another room already has the name (BR-002) (step 6)  
**Flow:**

1. System shows "A room named <name> already exists." at the field "Name" and keeps every entry of the form.
2. Use case continues at step 5.

### A2: Invalid Entry

**Trigger:** An entry breaks BR-001, BR-003, or BR-004 (step 6)  
**Flow:**

1. System shows the message of the broken rule at each offending field and keeps every entry of the form.
2. Use case continues at step 5.

### A3: Registration Cancelled

**Trigger:** Office Manager chooses "Cancel" instead of "Save" (step 5)  
**Flow:**

1. System closes the form without storing anything.
2. Use case ends.

## Postconditions

### Success Postconditions

- The room is stored with its name, capacity, and equipment
- The room is shown in the list of rooms

### Failure Postconditions

- No room is stored
- The list of rooms is unchanged

## Business Rules

### BR-001: Room Name

The name is required and at most 50 characters long; leading and trailing spaces are removed before it is checked
and stored. An empty name is rejected with "Name is required.", a longer one with "Name must be at most 50
characters."

### BR-002: Unique Room Name

No two rooms have the same name, compared without regard to upper and lower case.

### BR-003: Capacity

The capacity is required and a whole number from 1 to 50. A missing or out-of-range capacity is rejected with
"Capacity must be between 1 and 50."

### BR-004: Equipment

The equipment is optional and at most 200 characters long. A longer text is rejected with "Equipment must be at most
200 characters."
