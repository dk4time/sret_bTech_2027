package com.ridehailing.exception;

/** Pickup or drop lies outside the Chennai metro service area. */
public class OutOfServiceAreaException extends InvalidBookingException {

    public OutOfServiceAreaException(String message) {
        super(message);
    }
}
