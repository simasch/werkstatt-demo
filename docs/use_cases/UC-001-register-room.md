# Use Case: Register Room

## Overview

**Use Case ID:** UC-001  
**Use Case Name:** Register Room  
**Primary Actor:** Office Manager  
**Goal:** Register a meeting room with its name, capacity, and equipment so that employees can see and book it.  
**Trigger:** The office manager wants to add a meeting room to the room list.  
**Status:** Tested  

**Requirements:** [FR-001, FR-002, FR-003, NFR-001](../requirements.md)  
**Layers:** UI

## Preconditions

_None — access control is out of scope for the first release, and no other use case must run first._

## Main Success Scenario

1. Office Manager opens the room overview.
2. System displays every registered room with its name, capacity, and equipment, ordered by name.
3. Office Manager requests to register a new room.
4. System displays an empty room form.
5. Office Manager enters the name and the capacity of the room and, optionally, its equipment.
6. Office Manager saves the room.
7. System checks the entries against BR-001 to BR-005 and stores the room in one step, so that no other room can
   take the name in between.
8. System displays the new room in the room overview.

## Alternative Flows

### A1: No Room Registered Yet

**Trigger:** No room is registered yet (step 2)  
**Flow:**

1. System displays an empty room overview with the note that no room is registered yet.
2. Use case continues at step 3.

### A2: Invalid Entry

**Trigger:** The name or the capacity is missing, the capacity is outside its range, or an entry is too long (step 7)  
**Flow:**

1. System reports at each offending field what is wrong with it.
2. System keeps every entry of the form, including the rejected ones.
3. Use case continues at step 5.

### A3: Room Name Already Taken

**Trigger:** Another room already has the entered name, also one registered while the office manager was entering the data (step 7)  
**Flow:**

1. System reports at the name field that another room already has this name.
2. System keeps every entry of the form.
3. Use case continues at step 5.

### A4: Registration Cancelled

**Trigger:** Office Manager cancels the registration instead of saving (step 6)  
**Flow:**

1. System discards the entries and displays the room overview.
2. Use case ends.

## Postconditions

### Success Postconditions

- A new room is stored with the entered name (without leading or trailing spaces), capacity, and equipment.
- The room appears in the room overview with its name, capacity, and equipment.
- Employees can book the room (UC-002).

### Failure Postconditions

- No room is stored.
- The rooms registered before remain unchanged.
- No two rooms have the same name.

## Business Rules

### BR-001: Distinct Room Names

A room name must differ from the name of every other room. Names are compared without leading or trailing spaces and
without regard to upper and lower case: "Lakeview" and " lakeview " are the same name.

### BR-002: Trimmed Room Name

The room name is stored without leading or trailing spaces. A name that consists of spaces only counts as missing.

### BR-003: Required Entries

Name and capacity are required. The equipment is optional free text, such as "projector, whiteboard".

### BR-004: Capacity Range

The capacity is a whole number of people from 1 to 50.

### BR-005: Maximum Lengths

The name has at most 50 characters, the equipment at most 200 characters.
