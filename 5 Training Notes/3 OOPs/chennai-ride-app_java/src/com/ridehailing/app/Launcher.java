package com.ridehailing.app;

import com.ridehailing.check.SelfCheck;
import com.ridehailing.cli.ConsoleIO;
import com.ridehailing.cli.QuitException;
import com.ridehailing.cli.StartupMenu;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Single entry point.
 *
 *   (no argument)  start-up menu
 *   --demo         the scripted simulated day
 *   --check        SelfCheck (exit code 1 on any failure)
 *   --cli          interactive mode
 *   --echo / --no-echo   force echoing of typed input on or off (default: on when input is redirected)
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) throws Exception {
        // Always print UTF-8, so ₹ ✔ ✘ show correctly (Windows: build.bat also switches the console to UTF-8).
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));

        String mode = "";
        Boolean echo = null;
        for (String arg : args) {
            if (arg.equals("--echo")) {
                echo = Boolean.TRUE;
            } else if (arg.equals("--no-echo")) {
                echo = Boolean.FALSE;
            } else {
                mode = arg;
            }
        }
        if (echo == null) {
            echo = inputLooksRedirected();
        }

        switch (mode) {
            case "--demo":
                new ChennaiRideApp().runDay();
                return;
            case "--check":
                if (!SelfCheck.runAll()) {
                    System.exit(1);
                }
                return;
            case "--cli":
            case "":
                break;
            default:
                System.out.println("Unknown option " + mode + ". Use --demo, --check, --cli or no option for the menu.");
                System.exit(2);
        }

        ConsoleIO io = new ConsoleIO(System.in, System.out, echo);
        try {
            if (mode.equals("--cli")) {
                StartupMenu.startInteractive(io, "Exit");
            } else {
                new StartupMenu(io).run();
            }
            System.out.println("  Bye!");
        } catch (QuitException e) {
            System.out.println("  " + e.getMessage());
        }
    }

    /** Typed input is echoed only when it comes from a file or pipe (a replayed session). */
    private static boolean inputLooksRedirected() {
        if (System.console() == null) {
            return true;
        }
        try {
            return System.in.available() > 0;
        } catch (IOException e) {
            return false;
        }
    }
}
