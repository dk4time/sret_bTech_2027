package com.ridehailing.exception;

/** The captain entered an OTP that does not match. Carries how many attempts are left. */
public class InvalidOtpException extends RideHailingException {

    private final int attemptsLeft;

    public InvalidOtpException(String message, int attemptsLeft) {
        super(message);
        this.attemptsLeft = attemptsLeft;
    }

    public int getAttemptsLeft() {
        return attemptsLeft;
    }
}
