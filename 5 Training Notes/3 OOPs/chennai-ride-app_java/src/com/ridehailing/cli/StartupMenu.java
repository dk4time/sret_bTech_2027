package com.ridehailing.cli;

import com.ridehailing.app.ChennaiRideApp;
import com.ridehailing.check.SelfCheck;
import com.ridehailing.exception.RideHailingException;

/** The first screen: scripted day, interactive mode, or self-check. No world exists yet (session is null). */
public class StartupMenu extends Menu {

    public StartupMenu(ConsoleIO io) {
        super("Chennai Ride App · start-up menu", io, null);
    }

    @Override
    protected String[] options() {
        return new String[] {
                "Run full simulated day (demo)",
                "Interactive mode",
                "Run self-check"
        };
    }

    @Override
    protected String exitLabel() {
        return "Exit";
    }

    @Override
    protected void handle(int choice) throws RideHailingException {
        switch (choice) {
            case 1:
                new ChennaiRideApp().runDay();
                break;
            case 2:
                startInteractive(io, "Back to start-up menu");
                break;
            default:
                runSelfCheck();
                break;
        }
    }

    /** A fresh seeded world each time interactive mode starts. */
    public static void startInteractive(ConsoleIO io, String exitLabel) {
        CliSession session = new CliSession(io);
        session.printWelcome();
        Menu main = new MainMenu(io, session, exitLabel);
        main.run();
    }

    private void runSelfCheck() {
        try {
            SelfCheck.runAll();
        } catch (Exception e) {
            io.printError("SelfCheck stopped: " + e);
        }
    }
}
