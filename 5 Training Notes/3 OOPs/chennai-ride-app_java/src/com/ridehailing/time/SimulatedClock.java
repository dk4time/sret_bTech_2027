package com.ridehailing.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * The ONLY source of time in the app. LocalDateTime.now() is never used, so the whole day
 * is reproducible and every event gets a believable timestamp.
 *
 * The clock only moves forward. Trying to go back is a programming error in the demo, so it
 * throws an unchecked IllegalArgumentException.
 */
public final class SimulatedClock {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private LocalDateTime now;

    public SimulatedClock(LocalDateTime start) {
        if (start == null) {
            throw new IllegalArgumentException("Start time is required");
        }
        this.now = start;
    }

    public LocalDateTime now() {
        return now;
    }

    public LocalDate today() {
        return now.toLocalDate();
    }

    public void advanceMinutes(long minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException("The clock never goes backward (" + minutes + " minutes)");
        }
        now = now.plusMinutes(minutes);
    }

    public void advanceTo(LocalDateTime target) {
        if (target.isBefore(now)) {
            throw new IllegalArgumentException("The clock never goes backward: it is " + format(now)
                    + ", cannot move to " + format(target));
        }
        now = target;
    }

    /** Advances only if the target is later; handy when several rides share one clock. */
    public void advanceToAtLeast(LocalDateTime target) {
        if (target.isAfter(now)) {
            now = target;
        }
    }

    /** "[08:15 AM]" - printed at the start of every console line. */
    public String stamp() {
        return "[" + format(now) + "]";
    }

    public static String format(LocalDateTime time) {
        return time.format(STAMP);
    }

    @Override
    public String toString() {
        return "SimulatedClock " + now;
    }
}
