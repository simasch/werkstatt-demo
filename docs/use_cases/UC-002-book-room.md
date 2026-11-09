# Use Case: Book Room

## Overview

**Use Case ID:** UC-002  
**Use Case Name:** Book Room  
**Primary Actor:** Employee  
**Goal:** Book a room for a time slot so that the employee has a place for the meeting.  
**Trigger:** Employee wants to book a room for a meeting.  
**Status:** Draft

**Requirements:** [FR-004, FR-005, NFR-001](../requirements.md)  
**Layers:** UI

## Preconditions

_None — access control is out of scope for the first release, and the use case handles the case that no room is
registered yet (A1)._

## Main Success Scenario

1. Employee opens the room booking page.
2. System displays every registered room with its name, capacity, and equipment.
3. Employee selects a room to book.
4. System displays the booking form for the selected room together with its already booked time slots.
5. Employee enters their name, the start and end time of the meeting, and, optionally, its purpose.
6. Employee saves the booking.
7. System checks the entries against BR-001 to BR-004 and stores the booking in one step, so that no other booking
   of the room can take an overlapping time slot in between.
8. System displays the confirmed booking among the room's booked time slots.

## Alternative Flows

### A1: No Room Registered Yet

**Trigger:** No room is registered yet (step 2)  
**Flow:**

1. System displays an empty room list with the note that no room is registered yet.
2. Use case ends.

### A2: Invalid Entry

**Trigger:** The employee's name, the start time, or the end time is missing, the end time is not after the start
time, or an entry is too long (step 7)  
**Flow:**

1. System reports at each offending field what is wrong with it.
2. System keeps every entry of the form, including the rejected ones.
3. Use case continues at step 5.

### A3: Overlapping Booking

**Trigger:** Another booking of the same room overlaps the entered time slot, also one saved while the employee was
entering the data (step 7)  
**Flow:**

1. System reports that the room is already booked for an overlapping time slot.
2. System keeps every entry of the form.
3. Use case continues at step 5.

### A4: Booking Cancelled

**Trigger:** Employee cancels the booking instead of saving (step 6)  
**Flow:**

1. System discards the entries and displays the room list.
2. Use case ends.

## Postconditions

### Success Postconditions

- A new booking is stored for the selected room with the entered name, start and end time, and purpose.
- The booking appears among the room's booked time slots.
- No other booking of the same room overlaps it.

### Failure Postconditions

- No booking is stored.
- The bookings made before remain unchanged.
- No two bookings of the same room overlap.

## Business Rules

### BR-001: Booking Duration

A booking's end time must be after its start time; a booking of zero or negative duration is not allowed.

### BR-002: No Double Booking

Two bookings of the same room must not overlap: the time span from one booking's start time (inclusive) to its end
time (exclusive) must not intersect the time span of another booking of the same room. A booking that starts
exactly when another one of the same room ends does not overlap it.

### BR-003: Required Entries

The room, the employee's name, the start time, and the end time are required. The purpose is optional free text,
such as "Weekly sync". An employee's name that consists of spaces only counts as missing.

### BR-004: Maximum Lengths

The employee's name has at most 100 characters, the purpose at most 200 characters.
