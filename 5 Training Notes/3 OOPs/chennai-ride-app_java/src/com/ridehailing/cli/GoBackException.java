package com.ridehailing.cli;

/**
 * Thrown by ConsoleIO when the user types "b" at any prompt. The current menu catches it and
 * abandons the action in progress. UNCHECKED on purpose: it is navigation, not a business failure,
 * and it has to pass through every prompt helper without cluttering their signatures.
 */
public class GoBackException extends RuntimeException {

    public GoBackException() {
        super("back");
    }
}
