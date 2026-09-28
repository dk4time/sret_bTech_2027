package com.ridehailing.exception;

/** The captain cannot do this yet (e.g. going online while KYC is pending). */
public class CaptainNotEligibleException extends RideHailingException {

    public CaptainNotEligibleException(String message) {
        super(message);
    }
}
