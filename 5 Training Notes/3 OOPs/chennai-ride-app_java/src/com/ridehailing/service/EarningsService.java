package com.ridehailing.service;

import com.ridehailing.model.common.Money;
import com.ridehailing.model.payment.PaymentMethod;
import com.ridehailing.model.ride.FareReceipt;
import com.ridehailing.model.ride.OutstandingFee;
import com.ridehailing.model.ride.Ride;
import com.ridehailing.model.user.Captain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Splits every rupee a customer pays into: captain earnings + platform commission + GST.
 * Tracks each captain's day and the platform's totals, so at the end of the day the books
 * can be checked to the paisa.
 */
public class EarningsService {

    public static final int COMMISSION_PERCENT = 20;

    /** One captain's numbers for one day. A STATIC NESTED CLASS: it only makes sense inside EarningsService. */
    public static final class DayEarnings {
        private final Captain captain;
        private final LocalDate day;
        private int rides;
        private Money grossFare = Money.ZERO;      // fare before GST, incl. fees owed to this captain
        private Money commission = Money.ZERO;
        private Money netEarnings = Money.ZERO;
        private Money cashCollected = Money.ZERO;
        private Money duesAdded = Money.ZERO;

        private DayEarnings(Captain captain, LocalDate day) {
            this.captain = captain;
            this.day = day;
        }

        public Captain getCaptain() {
            return captain;
        }

        public LocalDate getDay() {
            return day;
        }

        public int getRides() {
            return rides;
        }

        public Money getGrossFare() {
            return grossFare;
        }

        public Money getCommission() {
            return commission;
        }

        public Money getNetEarnings() {
            return netEarnings;
        }

        public Money getCashCollected() {
            return cashCollected;
        }

        public Money getDuesAdded() {
            return duesAdded;
        }
    }

    private final Map<String, DayEarnings> earnings = new LinkedHashMap<>();
    private Money totalPaidByCustomers = Money.ZERO;
    private Money totalCaptainEarnings = Money.ZERO;
    private Money totalCommission = Money.ZERO;
    private Money totalGst = Money.ZERO;
    private Money totalCash = Money.ZERO;
    private Money totalDigital = Money.ZERO;
    private int paidRides;

    /** Called exactly once per ride, when its payment succeeds. */
    public void recordPaidRide(Ride ride, PaymentMethod method, LocalDate day) {
        FareReceipt receipt = ride.getReceipt();
        Captain rideCaptain = ride.getCaptain();
        DayEarnings mine = entryFor(rideCaptain, day);

        // 1) this trip's fare (after coupon) goes to the ride's captain, minus 20% commission
        Money rideCommission = receipt.getRideAmount().percent(COMMISSION_PERCENT);
        Money rideNet = receipt.getRideAmount().minus(rideCommission);
        mine.rides++;
        mine.grossFare = mine.grossFare.plus(receipt.getRideAmount());
        mine.commission = mine.commission.plus(rideCommission);
        mine.netEarnings = mine.netEarnings.plus(rideNet);
        totalCommission = totalCommission.plus(rideCommission);
        totalCaptainEarnings = totalCaptainEarnings.plus(rideNet);
        Money collectorsOwnShare = rideNet;

        // 2) carried-forward cancellation / no-show fees go to the captain who was let down
        for (OutstandingFee fee : receipt.getPreviousFees()) {
            DayEarnings owner = entryFor(fee.getOwedTo(), day);
            Money feeCommission = fee.getAmount().percent(COMMISSION_PERCENT);
            Money feeNet = fee.getAmount().minus(feeCommission);
            owner.grossFare = owner.grossFare.plus(fee.getAmount());
            owner.commission = owner.commission.plus(feeCommission);
            owner.netEarnings = owner.netEarnings.plus(feeNet);
            totalCommission = totalCommission.plus(feeCommission);
            totalCaptainEarnings = totalCaptainEarnings.plus(feeNet);
            if (fee.getOwedTo().equals(rideCaptain)) {
                collectorsOwnShare = collectorsOwnShare.plus(feeNet);
            }
        }

        // 3) GST belongs to the government, collected by the platform
        totalGst = totalGst.plus(receipt.getGst());
        totalPaidByCustomers = totalPaidByCustomers.plus(receipt.getTotal());
        paidRides++;

        // 4) cash stays with the captain, who now owes the platform everything that is not theirs
        if (method == PaymentMethod.CASH) {
            Money dues = receipt.getTotal().minus(collectorsOwnShare);
            mine.cashCollected = mine.cashCollected.plus(receipt.getTotal());
            mine.duesAdded = mine.duesAdded.plus(dues);
            rideCaptain.addDues(dues);
            totalCash = totalCash.plus(receipt.getTotal());
        } else {
            totalDigital = totalDigital.plus(receipt.getTotal());
        }
    }

    private DayEarnings entryFor(Captain captain, LocalDate day) {
        String key = captain.getId() + "|" + day;
        DayEarnings entry = earnings.get(key);
        if (entry == null) {
            entry = new DayEarnings(captain, day);
            earnings.put(key, entry);
        }
        return entry;
    }

    public DayEarnings getDayEarnings(Captain captain, LocalDate day) {
        DayEarnings entry = earnings.get(captain.getId() + "|" + day);
        return entry == null ? new DayEarnings(captain, day) : entry;
    }

    /** Net earnings over every day recorded (the demo day may run past midnight). */
    public Money getTotalNetEarnings(Captain captain) {
        Money total = Money.ZERO;
        for (DayEarnings entry : earnings.values()) {
            if (entry.captain.equals(captain)) {
                total = total.plus(entry.netEarnings);
            }
        }
        return total;
    }

    public List<DayEarnings> getAllDayEarnings() {
        return new ArrayList<>(earnings.values());
    }

    /** The books balance when: paid by customers = captain earnings + commission + GST (to the paisa). */
    public boolean isBalanced() {
        Money rightSide = totalCaptainEarnings.plus(totalCommission).plus(totalGst);
        boolean splitMatches = totalPaidByCustomers.equals(rightSide);
        boolean collectionMatches = totalPaidByCustomers.equals(totalCash.plus(totalDigital));
        return splitMatches && collectionMatches;
    }

    public Money getTotalPaidByCustomers() {
        return totalPaidByCustomers;
    }

    public Money getTotalCaptainEarnings() {
        return totalCaptainEarnings;
    }

    public Money getTotalCommission() {
        return totalCommission;
    }

    public Money getTotalGst() {
        return totalGst;
    }

    public Money getTotalCash() {
        return totalCash;
    }

    public Money getTotalDigital() {
        return totalDigital;
    }

    public int getPaidRides() {
        return paidRides;
    }
}
