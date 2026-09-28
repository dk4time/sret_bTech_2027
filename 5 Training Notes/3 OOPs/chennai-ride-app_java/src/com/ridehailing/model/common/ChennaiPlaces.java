package com.ridehailing.model.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Named places in Chennai with real latitude/longitude.
 *
 * FINAL CLASS with a private constructor: it only holds STATIC FINAL constants, so nobody
 * should create or extend it.
 */
public final class ChennaiPlaces {

    public static final Location T_NAGAR = new Location("T. Nagar", 13.0418, 80.2341);
    public static final Location ANNA_NAGAR = new Location("Anna Nagar", 13.0850, 80.2101);
    public static final Location ADYAR = new Location("Adyar", 13.0012, 80.2565);
    public static final Location VELACHERY = new Location("Velachery", 12.9815, 80.2180);
    public static final Location GUINDY = new Location("Guindy", 13.0067, 80.2206);
    public static final Location MYLAPORE = new Location("Mylapore", 13.0368, 80.2676);
    public static final Location EGMORE = new Location("Egmore", 13.0732, 80.2609);
    public static final Location CHENNAI_CENTRAL = new Location("Chennai Central", 13.0827, 80.2757);
    public static final Location CHENNAI_AIRPORT = new Location("Chennai Airport (Meenambakkam)", 12.9941, 80.1709);
    public static final Location SHOLINGANALLUR = new Location("Sholinganallur (OMR)", 12.9010, 80.2279);
    public static final Location PORUR = new Location("Porur", 13.0382, 80.1565);
    public static final Location VADAPALANI = new Location("Vadapalani", 13.0500, 80.2121);
    public static final Location BESANT_NAGAR = new Location("Besant Nagar", 13.0003, 80.2667);
    public static final Location KOYAMBEDU = new Location("Koyambedu", 13.0694, 80.1948);
    public static final Location TAMBARAM = new Location("Tambaram", 12.9249, 80.1000);
    public static final Location CHROMEPET = new Location("Chromepet", 12.9516, 80.1462);
    public static final Location PALLAVARAM = new Location("Pallavaram", 12.9675, 80.1491);
    public static final Location GUDUVANCHERY = new Location("Guduvanchery", 12.8447, 80.0600);
    public static final Location MEDAVAKKAM = new Location("Medavakkam", 12.9171, 80.1923);
    public static final Location PERUNGUDI = new Location("Perungudi (OMR)", 12.9654, 80.2461);
    public static final Location THIRUVANMIYUR = new Location("Thiruvanmiyur", 12.9830, 80.2594);
    public static final Location ASHOK_NAGAR = new Location("Ashok Nagar", 13.0350, 80.2120);
    public static final Location KK_NAGAR = new Location("KK Nagar", 13.0405, 80.1996);
    public static final Location KILPAUK = new Location("Kilpauk", 13.0850, 80.2420);
    public static final Location ROYAPETTAH = new Location("Royapettah", 13.0540, 80.2640);
    public static final Location WASHERMANPET = new Location("Washermanpet", 13.1148, 80.2872);

    /** A landmark used in captain messages ("... near Kathipara junction"). */
    public static final Location KATHIPARA_JUNCTION = new Location("Kathipara junction", 13.0070, 80.2050);

    /** Outside the service area on purpose (about 55 km south on ECR). */
    public static final Location MAHABALIPURAM = new Location("Mahabalipuram", 12.6208, 80.1945);

    /** Also outside on purpose (about 70 km west). */
    public static final Location KANCHIPURAM = new Location("Kanchipuram", 12.8342, 79.7036);

    private static final List<Location> NAMED_AREAS = new ArrayList<>();

    // STATIC INITIALISER: runs once when the class is first used.
    static {
        NAMED_AREAS.add(T_NAGAR);
        NAMED_AREAS.add(ANNA_NAGAR);
        NAMED_AREAS.add(ADYAR);
        NAMED_AREAS.add(VELACHERY);
        NAMED_AREAS.add(GUINDY);
        NAMED_AREAS.add(MYLAPORE);
        NAMED_AREAS.add(EGMORE);
        NAMED_AREAS.add(CHENNAI_CENTRAL);
        NAMED_AREAS.add(CHENNAI_AIRPORT);
        NAMED_AREAS.add(SHOLINGANALLUR);
        NAMED_AREAS.add(PORUR);
        NAMED_AREAS.add(VADAPALANI);
        NAMED_AREAS.add(BESANT_NAGAR);
        NAMED_AREAS.add(KOYAMBEDU);
        NAMED_AREAS.add(TAMBARAM);
        NAMED_AREAS.add(CHROMEPET);
        NAMED_AREAS.add(PALLAVARAM);
        NAMED_AREAS.add(GUDUVANCHERY);
        NAMED_AREAS.add(MEDAVAKKAM);
        NAMED_AREAS.add(PERUNGUDI);
        NAMED_AREAS.add(THIRUVANMIYUR);
        NAMED_AREAS.add(ASHOK_NAGAR);
        NAMED_AREAS.add(KK_NAGAR);
        NAMED_AREAS.add(KILPAUK);
        NAMED_AREAS.add(ROYAPETTAH);
        NAMED_AREAS.add(WASHERMANPET);
        NAMED_AREAS.add(KATHIPARA_JUNCTION);
    }

    private ChennaiPlaces() {
        // no objects: this class only holds constants
    }

    public static List<Location> namedAreas() {
        return Collections.unmodifiableList(NAMED_AREAS);
    }

    /** Real places just outside the service area, used to demo the "outside service area" rejection. */
    public static List<Location> outsideServiceAreaExamples() {
        List<Location> outside = new ArrayList<>();
        outside.add(MAHABALIPURAM);
        outside.add(KANCHIPURAM);
        return outside;
    }

    /** The named area closest to any GPS point, e.g. for "near Thiruvanmiyur". */
    public static Location nearestNamedArea(Location point) {
        Location nearest = NAMED_AREAS.get(0);
        double best = point.distanceTo(nearest);
        for (Location candidate : NAMED_AREAS) {
            double d = point.distanceTo(candidate);
            if (d < best) {
                best = d;
                nearest = candidate;
            }
        }
        return nearest;
    }

    /** Gives a GPS point a readable name: "Guindy" if it is right there, else "near Guindy". */
    public static Location nameGpsPoint(Location point) {
        Location nearest = nearestNamedArea(point);
        if (point.isWithinMeters(nearest, 150)) {
            return new Location(nearest.getName(), point.getLatitude(), point.getLongitude());
        }
        return new Location("near " + nearest.getName(), point.getLatitude(), point.getLongitude());
    }
}
