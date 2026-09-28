package com.ridehailing.notification;

import com.ridehailing.model.user.User;

/**
 * ABSTRACTION through an INTERFACE. RideService keeps a List<Notifier> and calls notify()
 * on each one without knowing if it is SMS or push: DYNAMIC DISPATCH picks the right code.
 * A new channel (WhatsApp, email) is just another class that implements Notifier.
 */
public interface Notifier {

    void notify(User recipient, RideEvent event, String message);

    /** Does this channel carry this kind of event? By default: every event. */
    default boolean handles(RideEvent event) {
        return true;
    }
}
