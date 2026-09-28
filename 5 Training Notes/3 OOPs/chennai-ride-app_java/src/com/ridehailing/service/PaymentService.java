package com.ridehailing.service;

import com.ridehailing.exception.InsufficientWalletBalanceException;
import com.ridehailing.exception.InvalidRideStatusException;
import com.ridehailing.exception.RideHailingException;
import com.ridehailing.model.payment.CashPayment;
import com.ridehailing.model.payment.Payable;
import com.ridehailing.model.payment.PaymentMethod;
import com.ridehailing.model.payment.PaymentStatus;
import com.ridehailing.model.payment.UpiPayment;
import com.ridehailing.model.payment.WalletPayment;
import com.ridehailing.model.ride.Ride;
import com.ridehailing.model.ride.RideStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Collects the money for a ride. Success moves the ride to COMPLETED and books the
 * earnings; failure moves it to PAYMENT_PENDING.
 */
public class PaymentService {

    private static final long UPI_REFERENCE_SEED = 7L;

    /** A known UPI outage window for one bank handle (e.g. "okhdfcbank"). */
    private static final class UpiOutage {
        private final String bankHandle;
        private final LocalDateTime from;
        private final LocalDateTime to;

        private UpiOutage(String bankHandle, LocalDateTime from, LocalDateTime to) {
            this.bankHandle = bankHandle;
            this.from = from;
            this.to = to;
        }

        private boolean covers(String upiId, LocalDateTime at) {
            return upiId.endsWith("@" + bankHandle) && !at.isBefore(from) && at.isBefore(to);
        }
    }

    private final EarningsService earningsService;
    private final Random upiReferenceGenerator = new Random(UPI_REFERENCE_SEED);
    private final List<UpiOutage> outages = new ArrayList<>();

    public PaymentService(EarningsService earningsService) {
        this.earningsService = earningsService;
    }

    /** Deterministic simulation of a UPI failure: the bank is down during this window. */
    public void reportUpiOutage(String bankHandle, LocalDateTime from, LocalDateTime to) {
        outages.add(new UpiOutage(bankHandle, from, to));
    }

    public boolean isBankReachable(String upiId, LocalDateTime at) {
        for (UpiOutage outage : outages) {
            if (outage.covers(upiId, at)) {
                return false;
            }
        }
        return true;
    }

    /** UPCASTING: three different classes are returned through one Payable type. */
    private Payable createPayment(PaymentMethod method, Ride ride, LocalDateTime at) {
        switch (method) {
            case UPI:
                String upiId = ride.getCustomer().getUpiId();
                return new UpiPayment(upiId, isBankReachable(upiId, at), upiReferenceGenerator);
            case CASH:
                return new CashPayment(ride.getCaptain());
            case WALLET:
                return new WalletPayment(ride.getCustomer());
            default:
                throw new IllegalArgumentException("Unknown payment method " + method);
        }
    }

    /**
     * Tries to collect the ride's total. Returns the Payable so the caller can read the result.
     * A ride that is already COMPLETED is refused: a payment can never succeed twice.
     */
    public Payable collect(Ride ride, PaymentMethod method, LocalDateTime at)
            throws InsufficientWalletBalanceException, InvalidRideStatusException {
        if (ride.getStatus() == RideStatus.COMPLETED) {
            throw new InvalidRideStatusException(ride.getId() + " is already paid - a payment can never succeed twice");
        }
        if (ride.getReceipt() == null) {
            throw new InvalidRideStatusException(ride.getId() + " has no bill yet (status " + ride.getStatus() + ")");
        }
        Payable payable = createPayment(method, ride, at);
        PaymentStatus status;
        try {
            status = payable.pay(ride.getReceipt().getTotal());   // DYNAMIC DISPATCH
        } catch (InsufficientWalletBalanceException e) {
            ride.markPaymentPending(method, "wallet balance " + e.getBalance() + " too low", at);
            throw e;
        } catch (RideHailingException e) {
            ride.markPaymentPending(method, e.getMessage(), at);
            return payable;
        }
        if (status == PaymentStatus.SUCCESS) {
            ride.complete(method, at);
            // A trip counts on the day it was booked, even if it ends (or is paid) after midnight.
            earningsService.recordPaidRide(ride, method, ride.getRequestedAt().toLocalDate());
        } else {
            ride.markPaymentPending(method, payable.resultMessage(), at);
        }
        return payable;
    }
}
