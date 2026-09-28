package com.ridehailing.notification;

import com.ridehailing.model.user.User;
import com.ridehailing.time.SimulatedClock;

/** In-app push notification. Handles every event (uses the interface's default handles()). */
public class PushNotifier implements Notifier {

    private final SimulatedClock clock;
    private final NotificationLog log;   // optional: null when nobody keeps a history

    public PushNotifier(SimulatedClock clock) {
        this(clock, null);
    }

    /** Overloaded: also records every message it sends into a NotificationLog (used by the interactive mode). */
    public PushNotifier(SimulatedClock clock, NotificationLog log) {
        this.clock = clock;
        this.log = log;
    }

    @Override
    public void notify(User recipient, RideEvent event, String message) {
        System.out.println(clock.stamp() + "      PUSH -> " + recipient.getName() + " [" + event.getTitle() + "] " + message);
        if (log != null) {
            log.record(clock.now(), "PUSH", recipient, event, message);
        }
    }
}
