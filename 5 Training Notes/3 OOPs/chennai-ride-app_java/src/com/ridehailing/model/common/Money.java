package com.ridehailing.model.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * An amount of Indian Rupees, always 2 decimals, rounded HALF_UP.
 *
 * VALUE OBJECT + IMMUTABILITY: the class is final and the field is final, so a Money can
 * never change after it is created. Every operation returns a NEW Money. Two Money objects
 * are equal when their amounts are equal (equality by value, not by identity).
 *
 * Money is never negative. A negative result means money is "disappearing" somewhere, which
 * is a bug, so the constructor throws an unchecked IllegalArgumentException.
 */
public final class Money implements Comparable<Money> {

    // STATIC members: shared by the whole class, not by one object.
    private static final int SCALE = 2;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    public static final Money ZERO = new Money(BigDecimal.ZERO);

    private final BigDecimal amount;

    private Money(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        BigDecimal scaled = amount.setScale(SCALE, RoundingMode.HALF_UP);
        if (scaled.signum() < 0) {
            throw new IllegalArgumentException("Money cannot be negative: " + scaled);
        }
        this.amount = scaled;
    }

    // Static factory methods (overloaded): three ways to create the same kind of object.
    public static Money of(long rupees) {
        return new Money(BigDecimal.valueOf(rupees));
    }

    public static Money of(String rupees) {
        return new Money(new BigDecimal(rupees));
    }

    public static Money of(BigDecimal rupees) {
        return new Money(rupees);
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    /** Multiplies by a quantity such as kilometres, minutes or a surge multiplier. */
    public Money times(double factor) {
        return new Money(amount.multiply(BigDecimal.valueOf(factor)));
    }

    public Money times(BigDecimal factor) {
        return new Money(amount.multiply(factor));
    }

    /** e.g. percent(5) gives 5% of this amount (used for GST, commission, night charge). */
    public Money percent(int percent) {
        return new Money(amount.multiply(BigDecimal.valueOf(percent)).divide(HUNDRED, SCALE, RoundingMode.HALF_UP));
    }

    public boolean isGreaterThan(Money other) {
        return amount.compareTo(other.amount) > 0;
    }

    public boolean isLessThan(Money other) {
        return amount.compareTo(other.amount) < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public Money max(Money other) {
        return isLessThan(other) ? other : this;
    }

    public Money min(Money other) {
        return isGreaterThan(other) ? other : this;
    }

    public BigDecimal toBigDecimal() {
        return amount;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(other.amount);
    }

    // equals/hashCode BY VALUE: ₹20.00 equals ₹20.00 even if they are different objects.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Money)) {
            return false;
        }
        Money other = (Money) o;
        return amount.equals(other.amount); // same scale is guaranteed, so equals is safe
    }

    @Override
    public int hashCode() {
        return amount.hashCode();
    }

    @Override
    public String toString() {
        return "₹" + String.format("%,.2f", amount);
    }
}
