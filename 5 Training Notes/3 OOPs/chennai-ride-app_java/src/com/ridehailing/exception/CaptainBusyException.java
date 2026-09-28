package com.ridehailing.exception;

/** The captain is on a ride and cannot do this now (e.g. go offline, take a second ride). */
public class CaptainBusyException extends RideHailingException {

    public CaptainBusyException(String message) {
        super(message);
    }
}
