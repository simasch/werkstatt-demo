# Use Case: Book Room

## Overview

**Use Case ID:** UC-002  
**Use Case Name:** Book Room  
**Primary Actor:** Employee  
**Goal:** Reserve a meeting room for a time slot.  
**Trigger:** An employee needs a room for a meeting.  
**Status:** Approved  
**Requirements:** [FR-004, FR-005, NFR-001](../requirements.md)

## Preconditions

- At least one room is registered (UC-001)

## Main Success Scenario

1. Employee chooses "Book Room" in the main menu.
2. System displays the booking form with the fields "Room", "Booked by", "Start", "End", and "Purpose".
3. Employee selects a room, enters their name, the start and the end of the time slot, optionally the purpose, and
   chooses "Book".
4. System validates the entries against BR-001 and BR-002.
5. System stores the booking and shows the message "Room <room> booked from <start> to <end>."

## Alternative Flows

### A1: Room Already Booked

**Trigger:** The time slot overlaps another booking of the same room (BR-002) (step 4)  
**Flow:**

1. System shows "The room is already booked from <start> to <end>." with the start and end of the overlapping
   booking, and keeps every entry of the form.
2. Use case continues at step 3.

## Postconditions

### Success Postconditions

- The booking is stored for the room and the time slot

### Failure Postconditions

- No booking is stored

## Business Rules

### BR-001: Time Slot

The end of a booking is after its start.

### BR-002: No Double Booking

Two bookings of the same room never overlap. A booking that ends at the moment another one starts does not overlap
it.
