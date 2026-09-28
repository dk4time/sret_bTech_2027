package com.ridehailing.cli;

import com.ridehailing.exception.RideHailingException;

/**
 * ABSTRACTION + TEMPLATE LOOP: every screen of the interactive mode is a Menu.
 *
 * Menu owns the loop that never changes (status line, numbered options, read a choice, handle
 * errors) in the concrete run() method. Each subclass only says WHAT its options are and HOW
 * to handle one choice. MainMenu keeps its sub-screens in plain Menu references and calls
 * run() on them: POLYMORPHISM picks the right subclass behaviour at runtime.
 *
 * Business errors (RideHailingException) and rule violations from the model
 * (IllegalArgumentException / IllegalStateException) are caught HERE, once, and shown as a
 * clean ✘ line - the app keeps running and nobody sees a stack trace.
 */
public abstract class Menu {

    protected final String title;
    protected final ConsoleIO io;
    protected final CliSession session;   // null only for the start-up menu (no world exists yet)

    protected Menu(String title, ConsoleIO io, CliSession session) {
        this.title = title;
        this.io = io;
        this.session = session;
    }

    /** The numbered options, shown as 1..n. Option 0 always leaves the menu. */
    protected abstract String[] options();

    /** Handles one chosen option (1..n). May throw any business exception: run() reports it. */
    protected abstract void handle(int choice) throws RideHailingException;

    /** Runs once before the loop, e.g. to pick a customer. Return false to leave straight away. */
    protected boolean onEnter() throws RideHailingException {
        return true;
    }

    /** Extra text after the title, e.g. the selected customer. */
    protected String subtitle() {
        return null;
    }

    protected String exitLabel() {
        return "Back";
    }

    /** The loop every menu shares. final: subclasses customise the steps, not the loop. */
    public final void run() {
        try {
            if (!onEnter()) {
                return;
            }
        } catch (GoBackException e) {
            return;
        } catch (RideHailingException | IllegalArgumentException | IllegalStateException e) {
            io.printError(e.getMessage());
            return;
        }
        while (true) {
            if (session != null) {
                io.printStatus(session.statusLine());
            }
            String sub = subtitle();
            io.printHeader(sub == null ? title : title + " · " + sub);
            String[] options = options();
            for (int i = 0; i < options.length; i++) {
                io.println(String.format("   %2d. %s", i + 1, options[i]));
            }
            io.println(String.format("   %2d. %s", 0, exitLabel()));

            int choice;
            try {
                choice = io.readInt("Choose", 0, options.length);
            } catch (GoBackException e) {
                return;
            }
            if (choice == 0) {
                return;
            }
            try {
                handle(choice);
            } catch (GoBackException e) {
                io.printInfo("(cancelled - back to " + title + ")");
            } catch (RideHailingException e) {
                io.printError(e.getMessage());
            } catch (IllegalArgumentException | IllegalStateException e) {
                io.printError(e.getMessage());
            }
        }
    }
}
