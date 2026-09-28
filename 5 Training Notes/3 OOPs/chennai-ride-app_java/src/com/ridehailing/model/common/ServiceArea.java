package com.ridehailing.model.common;

import com.ridehailing.exception.OutOfServiceAreaException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The region where the app operates: a bounding box, the high-demand hotspots and the
 * zones that crawl during peak hours.
 *
 * COMPOSITION: a ServiceArea HAS lists of Locations that it owns and never exposes for editing.
 */
public final class ServiceArea {

    private static final double HOTSPOT_RADIUS_KM = 1.5;
    private static final double CONGESTION_RADIUS_KM = 3.0;

    private final String name;
    private final double minLatitude;
    private final double maxLatitude;
    private final double minLongitude;
    private final double maxLongitude;
    private final List<Location> hotspots = new ArrayList<>();
    private final List<Location> congestedZones = new ArrayList<>();

    public ServiceArea(String name, double minLatitude, double maxLatitude,
                       double minLongitude, double maxLongitude) {
        if (minLatitude >= maxLatitude || minLongitude >= maxLongitude) {
            throw new IllegalArgumentException("Invalid bounding box for " + name);
        }
        this.name = name;
        this.minLatitude = minLatitude;
        this.maxLatitude = maxLatitude;
        this.minLongitude = minLongitude;
        this.maxLongitude = maxLongitude;
    }

    /** The Chennai metro area used in the demo: roughly Washermanpet to Guduvanchery, Porur to the coast. */
    public static ServiceArea chennaiMetro() {
        ServiceArea chennai = new ServiceArea("Chennai Metro", 12.80, 13.20, 80.00, 80.32);
        chennai.hotspots.add(ChennaiPlaces.CHENNAI_CENTRAL);
        chennai.hotspots.add(ChennaiPlaces.KOYAMBEDU);
        chennai.hotspots.add(ChennaiPlaces.CHENNAI_AIRPORT);
        chennai.congestedZones.add(ChennaiPlaces.T_NAGAR);
        chennai.congestedZones.add(ChennaiPlaces.KOYAMBEDU);
        chennai.congestedZones.add(ChennaiPlaces.PERUNGUDI);      // OMR
        chennai.congestedZones.add(ChennaiPlaces.SHOLINGANALLUR); // OMR
        return chennai;
    }

    public boolean contains(Location location) {
        return location.getLatitude() >= minLatitude && location.getLatitude() <= maxLatitude
                && location.getLongitude() >= minLongitude && location.getLongitude() <= maxLongitude;
    }

    public void requireInside(Location location, String label) throws OutOfServiceAreaException {
        if (!contains(location)) {
            throw new OutOfServiceAreaException(label + " '" + location.getName()
                    + "' is outside the " + name + " service area");
        }
    }

    public boolean isHotspot(Location location) {
        for (Location hotspot : hotspots) {
            if (location.isWithinKm(hotspot, HOTSPOT_RADIUS_KM)) {
                return true;
            }
        }
        return false;
    }

    /** T. Nagar, Koyambedu and OMR crawl during peak hours. */
    public boolean isCongested(Location location) {
        for (Location zone : congestedZones) {
            if (location.isWithinKm(zone, CONGESTION_RADIUS_KM)) {
                return true;
            }
        }
        return false;
    }

    /** Chennai peak traffic: 8-11 AM and 5-9 PM. */
    public boolean isPeakHour(LocalDateTime time) {
        int hour = time.getHour();
        return (hour >= 8 && hour < 11) || (hour >= 17 && hour < 21);
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name + " [" + minLatitude + ".." + maxLatitude + ", " + minLongitude + ".." + maxLongitude + "]";
    }
}
