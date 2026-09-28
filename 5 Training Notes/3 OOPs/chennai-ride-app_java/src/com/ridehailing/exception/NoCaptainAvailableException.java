package com.ridehailing.exception;

/** No eligible captain accepted the request (none nearby, or 3 offers rejected). */
public class NoCaptainAvailableException extends RideHailingException {

    public NoCaptainAvailableException(String message) {
        super(message);
    }
}
