package com.ridehailing.model.ride;

import com.ridehailing.model.common.Location;
import com.ridehailing.model.payment.PaymentMethod;
import com.ridehailing.model.user.Customer;
import com.ridehailing.model.vehicle.VehicleType;

import java.time.LocalDateTime;

/** What the customer asked for when booking. Immutable: a request is a fact that happened. */
public final class RideRequest {

    private final Customer customer;
    private final Location pickup;
    private final Location drop;
    private final VehicleType vehicleType;
    private final int passengers;
    private final PaymentMethod paymentMethod;
    private final String couponCode;   // null when no coupon
    private final LocalDateTime requestedAt;

    public RideRequest(Customer customer, Location pickup, Location drop, VehicleType vehicleType,
                       int passengers, PaymentMethod paymentMethod, String couponCode, LocalDateTime requestedAt) {
        if (customer == null || pickup == null || drop == null || vehicleType == null
                || paymentMethod == null || requestedAt == null) {
            throw new IllegalArgumentException("Every part of a ride request is required (except the coupon)");
        }
        this.customer = customer;
        this.pickup = pickup;
        this.drop = drop;
        this.vehicleType = vehicleType;
        this.passengers = passengers;
        this.paymentMethod = paymentMethod;
        this.couponCode = couponCode;
        this.requestedAt = requestedAt;
    }

    public double straightLineKm() {
        return pickup.distanceTo(drop);
    }

    public Customer getCustomer() {
        return customer;
    }

    public Location getPickup() {
        return pickup;
    }

    public Location getDrop() {
        return drop;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public int getPassengers() {
        return passengers;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public boolean hasCoupon() {
        return couponCode != null;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    @Override
    public String toString() {
        return customer.getName() + ": " + pickup + " -> " + drop + " by " + vehicleType
                + " for " + passengers + " (" + paymentMethod + ")";
    }
}
