package com.ridehailing.model.ride;

import java.time.LocalDateTime;

/** One line in a ride's timeline: when it happened, the status after it, and a note. */
public final class TimelineEntry {

    private final LocalDateTime at;
    private final RideStatus status;
    private final String note;

    public TimelineEntry(LocalDateTime at, RideStatus status, String note) {
        this.at = at;
        this.status = status;
        this.note = note;
    }

    public LocalDateTime getAt() {
        return at;
    }

    public RideStatus getStatus() {
        return status;
    }

    public String getNote() {
        return note;
    }
}
