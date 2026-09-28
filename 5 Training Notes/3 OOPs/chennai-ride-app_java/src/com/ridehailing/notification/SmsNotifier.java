package com.ridehailing.notification;

import com.ridehailing.model.user.User;
import com.ridehailing.time.SimulatedClock;

/** SMS costs money, so it is sent only for important events (OTP, fare, payment, cancellation). */
public class SmsNotifier implements Notifier {

    private final SimulatedClock clock;
    private final NotificationLog log;   // optional: null when nobody keeps a history

    public SmsNotifier(SimulatedClock clock) {
        this(clock, null);
    }

    /** Overloaded: also records every message it sends into a NotificationLog (used by the interactive mode). */
    public SmsNotifier(SimulatedClock clock, NotificationLog log) {
        this.clock = clock;
        this.log = log;
    }

    @Override
    public boolean handles(RideEvent event) {
        return event.isImportant();
    }

    @Override
    public void notify(User recipient, RideEvent event, String message) {
        System.out.println(clock.stamp() + "      SMS  -> " + recipient.getPhone() + ": " + message);
        if (log != null) {
            log.record(clock.now(), "SMS", recipient, event, message);
        }
    }
}
