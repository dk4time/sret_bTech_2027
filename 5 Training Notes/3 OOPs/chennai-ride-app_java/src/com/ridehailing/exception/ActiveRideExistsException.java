package com.ridehailing.exception;

/** The customer already has a ride that is not finished. Inheritance: it IS-A InvalidBookingException. */
public class ActiveRideExistsException extends InvalidBookingException {

    public ActiveRideExistsException(String message) {
        super(message);
    }
}
