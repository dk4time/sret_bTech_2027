package com.ridehailing.service;

/** How a ride offer reaches a captain. */
public enum DispatchMode {

    /** Each offered captain decides instantly (by their trip-length preference). Used by the scripted demo. */
    AUTOMATIC("Auto captain"),

    /** The offer waits on the captain's phone until they tap Accept or Reject in the Captain app. */
    CAPTAIN_APP("Manual captain");

    private final String label;

    DispatchMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
