package com.ridehailing.model.user;

import com.ridehailing.exception.InsufficientWalletBalanceException;
import com.ridehailing.model.common.Location;
import com.ridehailing.model.common.Money;
import com.ridehailing.model.ride.OutstandingFee;
import com.ridehailing.model.ride.Ride;
import com.ridehailing.model.ride.RideStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The person who books. Customer IS-A User.
 *
 * AGGREGATION: a customer keeps a list of Ride references (history). The rides are also
 * held by the RideService, and a Ride refers back to its Customer and Captain; neither side
 * owns the other's lifecycle.
 */
public class Customer extends User {

    private static int nextIdNumber = 1001;
    public static final Money MAX_TOP_UP = Money.of(10000);

    private final String upiId;
    private Money walletBalance;
    private final List<Ride> rideHistory = new ArrayList<>();
    private final List<OutstandingFee> outstandingFees = new ArrayList<>();
    private final Set<String> usedCoupons = new HashSet<>();

    public Customer(String name, String phone, Location home, String upiId) {
        this(name, phone, home, upiId, Money.ZERO);
    }

    public Customer(String name, String phone, Location home, String upiId, Money openingWalletBalance) {
        super("CUS-" + (nextIdNumber++), name, phone, home);
        if (upiId == null || !upiId.contains("@")) {
            throw new IllegalArgumentException("UPI id must look like name@bank, got: " + upiId);
        }
        this.upiId = upiId;
        this.walletBalance = openingWalletBalance;
    }

    @Override
    public String getRole() {
        return "Customer";
    }

    // ------------------------------------------------------------ wallet

    public void topUpWallet(Money amount) {
        if (amount.isZero()) {
            throw new IllegalArgumentException("Top-up amount must be positive");
        }
        if (amount.isGreaterThan(MAX_TOP_UP)) {
            throw new IllegalArgumentException("Top-up is limited to " + MAX_TOP_UP + " per transaction");
        }
        walletBalance = walletBalance.plus(amount);
    }

    /** All or nothing: the balance is never deducted partially. */
    public void debitWallet(Money amount) throws InsufficientWalletBalanceException {
        if (walletBalance.isLessThan(amount)) {
            throw new InsufficientWalletBalanceException(walletBalance, amount);
        }
        walletBalance = walletBalance.minus(amount);
    }

    // ------------------------------------------------------------ rides

    public void recordRide(Ride ride) {
        if (!rideHistory.isEmpty()) {
            Ride last = rideHistory.get(rideHistory.size() - 1);
            if (ride.getRequestedAt().isBefore(last.getRequestedAt())) {
                throw new IllegalStateException("Ride history must stay in chronological order");
            }
        }
        rideHistory.add(ride);
    }

    public Ride getActiveRide() {
        for (Ride ride : rideHistory) {
            if (ride.getStatus().isActive()) {
                return ride;
            }
        }
        return null;
    }

    public boolean hasActiveRide() {
        return getActiveRide() != null;
    }

    public Ride getPaymentPendingRide() {
        for (Ride ride : rideHistory) {
            if (ride.getStatus() == RideStatus.PAYMENT_PENDING) {
                return ride;
            }
        }
        return null;
    }

    /** Location changes when a ride ends: the customer is wherever they were dropped. */
    public void arriveAt(Location dropPoint) {
        moveTo(dropPoint);
    }

    /** The customer moved on their own (metro, walk, bus) - only possible when not in a ride. */
    public void travelOnOwnTo(Location place) {
        if (hasActiveRide()) {
            throw new IllegalStateException(getName() + " is in a ride and cannot travel on their own");
        }
        moveTo(place);
    }

    // ------------------------------------------------------------ fees & coupons

    public void addOutstandingFee(OutstandingFee fee) {
        outstandingFees.add(fee);
    }

    /** Hands over every unpaid fee exactly once: the list is emptied as it is taken. */
    public List<OutstandingFee> takeOutstandingFees() {
        List<OutstandingFee> taken = new ArrayList<>(outstandingFees);
        outstandingFees.clear();
        return taken;
    }

    public Money getOutstandingFeeTotal() {
        Money total = Money.ZERO;
        for (OutstandingFee fee : outstandingFees) {
            total = total.plus(fee.getAmount());
        }
        return total;
    }

    public boolean hasUsedCoupon(String code) {
        return usedCoupons.contains(code);
    }

    public void markCouponUsed(String code) {
        if (!usedCoupons.add(code)) {
            throw new IllegalStateException(code + " was already used by " + getName());
        }
    }

    public List<Ride> getRideHistory() {
        return Collections.unmodifiableList(rideHistory);
    }

    public Money getWalletBalance() {
        return walletBalance;
    }

    public String getUpiId() {
        return upiId;
    }
}
