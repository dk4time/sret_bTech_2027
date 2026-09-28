package com.ridehailing.cli;

import com.ridehailing.service.DispatchMode;

/**
 * The hub of the interactive mode. Each sub-screen is held as a plain Menu reference
 * (UPCASTING) and started with run() - DYNAMIC DISPATCH runs the right subclass.
 */
public class MainMenu extends Menu {

    private final String exitLabel;

    public MainMenu(ConsoleIO io, CliSession session, String exitLabel) {
        super("Main menu", io, session);
        this.exitLabel = exitLabel;
    }

    @Override
    protected String[] options() {
        return new String[] {
                "Customer app",
                "Captain app",
                "Operations dashboard",
                "Clock controls",
                "Settings (captain mode: Manual / Auto)"
        };
    }

    @Override
    protected String exitLabel() {
        return exitLabel;
    }

    @Override
    protected void handle(int choice) {
        Menu next;
        switch (choice) {
            case 1:
                next = new CustomerMenu(io, session);
                break;
            case 2:
                next = new CaptainMenu(io, session);
                break;
            case 3:
                next = new OperationsMenu(io, session);
                break;
            case 4:
                next = new ClockMenu(io, session);
                break;
            default:
                changeCaptainMode();
                return;
        }
        next.run();   // polymorphism: whichever Menu subclass 'next' really is
    }

    private void changeCaptainMode() {
        io.printInfo("Captain mode is now: " + session.getMode().getLabel());
        io.printInfo("  Manual - a booking waits for the captain: switch to the Captain app to accept/reject,");
        io.printInfo("           mark arrived, enter the OTP and complete the ride (both sides of the app, live).");
        io.printInfo("  Auto   - the captain accepts at once, drives over and starts with the OTP;");
        io.printInfo("           the trip ends when you move the clock past the drop time. You act only as the customer.");
        int pick = io.readChoice("Captain mode", new String[] {"Manual captain", "Auto captain"});
        session.setMode(pick == 0 ? DispatchMode.CAPTAIN_APP : DispatchMode.AUTOMATIC);
        io.printSuccess("Captain mode: " + session.getMode().getLabel());
    }
}
