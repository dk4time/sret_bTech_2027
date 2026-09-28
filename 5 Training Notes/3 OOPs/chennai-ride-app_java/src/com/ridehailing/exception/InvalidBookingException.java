package com.ridehailing.exception;

/** A booking request breaks a booking rule. Parent of the more specific booking exceptions below. */
public class InvalidBookingException extends RideHailingException {

    public InvalidBookingException(String message) {
        super(message);
    }
}
