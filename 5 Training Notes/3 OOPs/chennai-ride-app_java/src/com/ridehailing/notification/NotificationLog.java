package com.ridehailing.notification;

import com.ridehailing.model.user.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Keeps every SMS and push message that was sent, so the operations team can look back.
 * SmsNotifier and PushNotifier write into it (composition: they HAVE-A log).
 */
public class NotificationLog {

    /** One sent message. Immutable. */
    public static final class Entry {
        private final LocalDateTime at;
        private final String channel;
        private final String recipient;
        private final RideEvent event;
        private final String message;

        private Entry(LocalDateTime at, String channel, String recipient, RideEvent event, String message) {
            this.at = at;
            this.channel = channel;
            this.recipient = recipient;
            this.event = event;
            this.message = message;
        }

        public LocalDateTime getAt() {
            return at;
        }

        public String getChannel() {
            return channel;
        }

        public String getRecipient() {
            return recipient;
        }

        public RideEvent getEvent() {
            return event;
        }

        public String getMessage() {
            return message;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    public void record(LocalDateTime at, String channel, User recipient, RideEvent event, String message) {
        entries.add(new Entry(at, channel, recipient.getName(), event, message));
    }

    /** Newest message first. */
    public List<Entry> latestFirst() {
        List<Entry> result = new ArrayList<>();
        for (int i = entries.size() - 1; i >= 0; i--) {
            result.add(entries.get(i));
        }
        return result;
    }

    public int size() {
        return entries.size();
    }
}
