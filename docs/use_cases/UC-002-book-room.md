# Use Case: Book Room

## Overview

**Use Case ID:** UC-002  
**Use Case Name:** Book Room  
**Primary Actor:** Employee  
**Goal:** Reserve a meeting room for a time slot.  
**Trigger:** An employee needs a room for a meeting.  
**Status:** Tested  
**Requirements:** [FR-004, FR-005, NFR-001](../requirements.md)

## Preconditions

- At least one room is registered (UC-001)

## Main Success Scenario

1. Employee chooses "Book Room" in the main menu.
2. System displays the booking form with the fields "Room", "Booked by", "Start", "End", and "Purpose".
3. Employee selects a room, enters their name, the start and the end of the time slot, optionally the purpose, and
   chooses "Book".
4. System validates the entries against BR-001 to BR-003.
5. System stores the booking and shows the message "Room <room> booked from <start> to <end>."

## Alternative Flows

### A1: Room Already Booked

**Trigger:** The time slot overlaps another booking of the same room (BR-002) (step 4)  
**Flow:**

1. System shows "The room is already booked from <start> to <end>." at the fields "Start" and "End", with the start
   and end of the overlapping booking that starts first, and keeps every entry of the form.
2. Use case continues at step 3.

### A2: Invalid Entry

**Trigger:** An entry breaks BR-001 or BR-003 (step 4)  
**Flow:**

1. System shows the message of the broken rule at each offending field and keeps every entry of the form.
2. Use case continues at step 3.

## Postconditions

### Success Postconditions

- The booking is stored for the room and the time slot

### Failure Postconditions

- No booking is stored

## Business Rules

### BR-001: Time Slot

The start and the end are required, and the end is after the start. A missing start or end is rejected with
"Start is required." or "End is required.", an end not after the start with "End must be after start." at the
field "End".

### BR-002: No Double Booking

Two bookings of the same room never overlap. A booking that ends at the moment another one starts does not overlap
it.

### BR-003: Booking Details

The room is required; a missing room is rejected with "Room is required." The name in "Booked by" is required and
at most 100 characters long; leading and trailing spaces are removed before it is checked and stored. An empty name
is rejected with "Booked by is required.", a longer one with "Booked by must be at most 100 characters." The purpose
is optional and at most 200 characters long; a longer text is rejected with "Purpose must be at most 200
characters."

### BR-004: Date Format

Messages show a date and time as `yyyy-MM-dd HH:mm`, for example 2026-11-09 14:00.
