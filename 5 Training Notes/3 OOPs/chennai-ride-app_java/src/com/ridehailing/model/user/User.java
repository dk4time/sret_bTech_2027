package com.ridehailing.model.user;

import com.ridehailing.model.common.Location;

/**
 * ABSTRACTION + HIERARCHICAL INHERITANCE: User is abstract; Customer and Captain extend it.
 * Everything common to both sides of the app (id, name, phone, where they are, their
 * rating) lives here once.
 *
 * ENCAPSULATION: all fields are private and there are no public setters. A user's location
 * changes only through event methods in the subclasses (captain drops a customer, customer
 * reaches a destination) which call the protected moveTo().
 */
public abstract class User {

    private static final String INDIAN_MOBILE = "\\+91 [6-9]\\d{4} \\d{5}";

    private final String id;
    private final String name;
    private final String phone;
    private Location location;
    private int ratingCount;
    private int ratingTotal;

    protected User(String id, String name, String phone, Location location) {
        this(id, name, phone, location, 0, 0);
    }

    // CONSTRUCTOR OVERLOADING: a user who joined earlier arrives with their past ratings.
    protected User(String id, String name, String phone, Location location, int pastRatingCount, int pastRatingTotal) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (phone == null || !phone.matches(INDIAN_MOBILE)) {
            throw new IllegalArgumentException("Phone must look like +91 98401 23456, got: " + phone);
        }
        if (location == null) {
            throw new IllegalArgumentException("Location is required");
        }
        if (pastRatingCount < 0 || pastRatingTotal < pastRatingCount || pastRatingTotal > pastRatingCount * 5) {
            throw new IllegalArgumentException("Past ratings are inconsistent");
        }
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.location = location;
        this.ratingCount = pastRatingCount;
        this.ratingTotal = pastRatingTotal;
    }

    /** Each subclass says what it is: "Customer" or "Captain". */
    public abstract String getRole();

    /** Only subclasses may move a user, and only from inside their event methods. */
    protected final void moveTo(Location newLocation) {
        if (newLocation == null) {
            throw new IllegalArgumentException("Location is required");
        }
        this.location = newLocation;
    }

    /** Stars from the other side of a completed ride (the RatingService checks the ride). */
    public void receiveRating(int stars) {
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("Rating must be 1 to 5 stars, got " + stars);
        }
        ratingCount++;
        ratingTotal += stars;
    }

    public double getAverageRating() {
        if (ratingCount == 0) {
            return 0.0;
        }
        return (double) ratingTotal / ratingCount;
    }

    public int getRatingCount() {
        return ratingCount;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    public Location getLocation() {
        return location;
    }

    // ENTITY equality: two User objects are the same person if they have the same id.
    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User)) {
            return false;
        }
        return id.equals(((User) o).id);
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return getRole() + " " + name + " (" + id + ")";
    }
}
