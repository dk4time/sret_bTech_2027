package com.ridehailing.exception;

/**
 * Base class of every business-rule failure in the app.
 *
 * CHECKED vs UNCHECKED (deliberate choice):
 *   RideHailingException extends Exception, so it is CHECKED. Every subclass describes
 *   something that WILL happen in a normal day: no captain nearby, a wrong OTP, a low
 *   wallet, a UPI failure. The caller (the app screen) must decide what to show the user,
 *   so the compiler forces the caller to handle it or declare it.
 *
 *   Programming mistakes (a negative Money amount, a null location, a malformed number
 *   plate) are NOT business outcomes. For those we throw the JDK's UNCHECKED exceptions
 *   (IllegalArgumentException / IllegalStateException): they point to a bug to fix, not
 *   a situation to handle.
 */
public class RideHailingException extends Exception {

    public RideHailingException(String message) {
        super(message);
    }
}
