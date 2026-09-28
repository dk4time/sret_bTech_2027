package com.ridehailing.cli;

/**
 * Thrown when the user confirms "q", or when the input ends (EOF, e.g. a replayed session file).
 * Caught once, at the very top (Launcher), which says goodbye and exits cleanly.
 */
public class QuitException extends RuntimeException {

    public QuitException(String message) {
        super(message);
    }
}
