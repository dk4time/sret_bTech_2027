package com.ridehailing.model.common;

/**
 * A point on the map (latitude/longitude) with a human-friendly name.
 *
 * VALUE OBJECT: final class, final fields, no setters. Two locations are equal when they
 * have the same coordinates. The name is only a label and does not affect equality.
 */
public final class Location {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final String name;
    private final double latitude;
    private final double longitude;

    public Location(String name, double latitude, double longitude) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Location name is required");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Invalid coordinates: " + latitude + ", " + longitude);
        }
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // CONSTRUCTOR OVERLOADING with this() chaining: an unnamed GPS point.
    public Location(double latitude, double longitude) {
        this("GPS point", latitude, longitude);
    }

    /** Straight-line (great-circle) distance in km, using the haversine formula. */
    public double distanceTo(Location other) {
        double dLat = Math.toRadians(other.latitude - latitude);
        double dLon = Math.toRadians(other.longitude - longitude);
        double lat1 = Math.toRadians(latitude);
        double lat2 = Math.toRadians(other.latitude);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public boolean isWithinKm(Location other, double km) {
        return distanceTo(other) <= km;
    }

    public boolean isWithinMeters(Location other, double meters) {
        return distanceTo(other) * 1000 <= meters;
    }

    /**
     * A point part of the way from here to the destination.
     * fraction 0.0 = here, 1.0 = destination. Used for "end trip early" and "change destination".
     */
    public Location pointTowards(Location destination, double fraction, String pointName) {
        if (fraction < 0 || fraction > 1) {
            throw new IllegalArgumentException("Fraction must be between 0 and 1: " + fraction);
        }
        double lat = latitude + (destination.latitude - latitude) * fraction;
        double lon = longitude + (destination.longitude - longitude) * fraction;
        return new Location(pointName, lat, lon);
    }

    public String getName() {
        return name;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Location)) {
            return false;
        }
        Location other = (Location) o;
        return Double.compare(latitude, other.latitude) == 0
                && Double.compare(longitude, other.longitude) == 0;
    }

    @Override
    public int hashCode() {
        return 31 * Double.hashCode(latitude) + Double.hashCode(longitude);
    }

    @Override
    public String toString() {
        return name;
    }
}
