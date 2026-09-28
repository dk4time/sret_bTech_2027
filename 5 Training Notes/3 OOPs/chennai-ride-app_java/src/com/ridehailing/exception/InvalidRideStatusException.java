package com.ridehailing.exception;

/** A ride method was called in a status that does not allow it (e.g. starting a cancelled ride). */
public class InvalidRideStatusException extends RideHailingException {

    public InvalidRideStatusException(String message) {
        super(message);
    }
}
