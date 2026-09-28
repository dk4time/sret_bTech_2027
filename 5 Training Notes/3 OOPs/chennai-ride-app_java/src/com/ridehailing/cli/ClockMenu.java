package com.ridehailing.cli;

import com.ridehailing.service.FareService;
import com.ridehailing.time.SimulatedClock;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Moving time by hand is how the grace period, waiting charges, no-shows and the night charge are
 * shown live. The SimulatedClock itself refuses to go backward - the menu just reports it.
 */
public class ClockMenu extends Menu {

    public ClockMenu(ConsoleIO io, CliSession session) {
        super("Clock controls", io, session);
    }

    @Override
    protected String[] options() {
        return new String[] {
                "Show current time",
                "Advance by N minutes",
                "Jump forward to a time (e.g. 23:30)",
                "Advance to the next ride event (Auto captain rides)"
        };
    }

    @Override
    protected void handle(int choice) {
        switch (choice) {
            case 1:
                showTime();
                break;
            case 2:
                int minutes = io.readInt("Minutes to advance", 1, 24 * 60);
                session.advanceClockByMinutes(minutes);
                showTime();
                break;
            case 3:
                LocalTime time = io.readTime("Jump to");
                LocalDateTime target = LocalDateTime.of(session.getClock().today(), time);
                session.advanceClockTo(target);   // earlier than now? the clock refuses with a clear message
                showTime();
                break;
            default:
                LocalDateTime next = session.getWorld().getAutopilot().nextEventTime();
                if (next == null) {
                    io.printInfo("No Auto-captain ride is waiting for its next step.");
                    return;
                }
                session.advanceClockTo(next);
                showTime();
                break;
        }
    }

    private void showTime() {
        LocalDateTime now = session.getClock().now();
        String period = FareService.isNightTime(now) ? "night (+20% night charge from 11 PM to 5 AM)"
                : session.getWorld().getServiceArea().isPeakHour(now) ? "peak hour (8-11 AM, 5-9 PM: slower traffic)"
                : "off-peak";
        io.printSuccess("It is " + SimulatedClock.format(now) + " on " + now.toLocalDate() + " - " + period + ".");
    }
}
