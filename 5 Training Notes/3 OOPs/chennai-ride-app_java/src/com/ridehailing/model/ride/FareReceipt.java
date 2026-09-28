package com.ridehailing.model.ride;

import com.ridehailing.model.common.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The itemised bill for one ride. Built by FareService, immutable afterwards.
 *
 * The receipt derives its own subtotal, GST and total from the line items, so the
 * numbers on it can never disagree with each other.
 */
public final class FareReceipt {

    private final String rideId;
    private final String tripSummary;
    private final String distanceLabel;
    private final String timeLabel;
    private final Money baseFare;
    private final Money distanceCharge;
    private final Money timeCharge;
    private final Money minimumFareTopUp;
    private final Money waitingCharge;
    private final int chargedWaitingMinutes;
    private final BigDecimal surgeMultiplier;
    private final Money surgeCharge;
    private final Money nightCharge;
    private final List<OutstandingFee> previousFees;
    private final String couponCode;
    private final Money couponDiscount;
    private final int gstPercent;
    private final Money estimatedTotal;

    // derived amounts
    private final Money tripFare;
    private final Money rideAmount;
    private final Money previousFeesTotal;
    private final Money subtotal;
    private final Money gst;
    private final Money total;

    public FareReceipt(String rideId, String tripSummary, String distanceLabel, String timeLabel,
                       Money baseFare, Money distanceCharge, Money timeCharge, Money minimumFareTopUp,
                       Money waitingCharge, int chargedWaitingMinutes,
                       BigDecimal surgeMultiplier, Money surgeCharge, Money nightCharge,
                       List<OutstandingFee> previousFees, String couponCode, Money couponDiscount,
                       int gstPercent, Money estimatedTotal) {
        this.rideId = rideId;
        this.tripSummary = tripSummary;
        this.distanceLabel = distanceLabel;
        this.timeLabel = timeLabel;
        this.baseFare = baseFare;
        this.distanceCharge = distanceCharge;
        this.timeCharge = timeCharge;
        this.minimumFareTopUp = minimumFareTopUp;
        this.waitingCharge = waitingCharge;
        this.chargedWaitingMinutes = chargedWaitingMinutes;
        this.surgeMultiplier = surgeMultiplier;
        this.surgeCharge = surgeCharge;
        this.nightCharge = nightCharge;
        this.previousFees = new ArrayList<>(previousFees); // defensive copy
        this.couponCode = couponCode;
        this.couponDiscount = couponDiscount;
        this.gstPercent = gstPercent;
        this.estimatedTotal = estimatedTotal;

        this.tripFare = baseFare.plus(distanceCharge).plus(timeCharge).plus(minimumFareTopUp)
                .plus(waitingCharge).plus(surgeCharge).plus(nightCharge);
        this.rideAmount = tripFare.minus(couponDiscount);
        Money fees = Money.ZERO;
        for (OutstandingFee fee : previousFees) {
            fees = fees.plus(fee.getAmount());
        }
        this.previousFeesTotal = fees;
        this.subtotal = rideAmount.plus(previousFeesTotal);
        this.gst = subtotal.percent(gstPercent);
        this.total = subtotal.plus(gst);
    }

    public String getRideId() {
        return rideId;
    }

    /** Fare for THIS trip before coupon and GST. */
    public Money getTripFare() {
        return tripFare;
    }

    /** This trip's fare after the coupon: the part the ride's captain earns from (before commission). */
    public Money getRideAmount() {
        return rideAmount;
    }

    public List<OutstandingFee> getPreviousFees() {
        return Collections.unmodifiableList(previousFees);
    }

    public Money getPreviousFeesTotal() {
        return previousFeesTotal;
    }

    public Money getSubtotal() {
        return subtotal;
    }

    public Money getGst() {
        return gst;
    }

    public Money getTotal() {
        return total;
    }

    public Money getEstimatedTotal() {
        return estimatedTotal;
    }

    public Money getWaitingCharge() {
        return waitingCharge;
    }

    public Money getNightCharge() {
        return nightCharge;
    }

    public Money getSurgeCharge() {
        return surgeCharge;
    }

    public Money getCouponDiscount() {
        return couponDiscount;
    }

    public Money getMinimumFareTopUp() {
        return minimumFareTopUp;
    }

    public Money getBaseFare() {
        return baseFare;
    }

    public Money getDistanceCharge() {
        return distanceCharge;
    }

    public Money getTimeCharge() {
        return timeCharge;
    }

    private static String line(String label, String amount) {
        return String.format("  │ %-46s %12s │%n", label, amount);
    }

    private static String line(String label, Money amount) {
        return line(label, amount.toString());
    }

    @Override
    public String toString() {
        String rule = "  ├" + "─".repeat(61) + "┤\n";
        StringBuilder sb = new StringBuilder();
        sb.append("  ┌").append("─".repeat(61)).append("┐\n");
        sb.append(String.format("  │ %-59s │%n", "RECEIPT  " + rideId));
        for (String part : tripSummary.split("\n")) {
            sb.append(String.format("  │ %-59s │%n", part));
        }
        sb.append(rule);
        sb.append(line("1. Base fare", baseFare));
        sb.append(line("2. Distance charge (" + distanceLabel + ")", distanceCharge));
        sb.append(line("3. Time charge (" + timeLabel + ")", timeCharge));
        if (!minimumFareTopUp.isZero()) {
            sb.append(line("   Minimum fare top-up", minimumFareTopUp));
        }
        sb.append(line("4. Waiting charge (" + chargedWaitingMinutes + " min after 3 free)", waitingCharge));
        sb.append(line("5. Surge (" + surgeMultiplier + "x)", surgeCharge));
        sb.append(line("6. Night charge (+20%, 11 PM-5 AM)", nightCharge));
        if (previousFees.isEmpty()) {
            sb.append(line("7. Previous cancellation fee", Money.ZERO));
        } else {
            for (OutstandingFee fee : previousFees) {
                sb.append(line("7. Previous cancellation fee (" + fee.getFromRideId() + ")", fee.getAmount()));
            }
        }
        String couponLabel = couponCode == null ? "8. Coupon discount" : "8. Coupon " + couponCode;
        sb.append(line(couponLabel, couponDiscount.isZero() ? Money.ZERO.toString() : "- " + couponDiscount));
        sb.append(line("   Subtotal", subtotal));
        sb.append(line("9. GST " + gstPercent + "%", gst));
        sb.append(rule);
        sb.append(line("10. TOTAL", total));
        sb.append(line("    Estimated at booking", estimatedTotal));
        sb.append("  └").append("─".repeat(61)).append("┘");
        return sb.toString();
    }
}
