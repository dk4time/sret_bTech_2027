package com.ridehailing.model.user;

/** Where a captain is in their working day. Enum with a field and behaviour. */
public enum CaptainStatus {

    KYC_PENDING("KYC pending"),
    OFFLINE("Offline"),
    AVAILABLE("Online · free"),
    ON_RIDE("On a ride");

    private final String label;

    CaptainStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Only a free, online captain may be offered a ride. */
    public boolean canReceiveOffers() {
        return this == AVAILABLE;
    }
}
