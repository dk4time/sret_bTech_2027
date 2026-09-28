package com.ridehailing.service;

import com.ridehailing.exception.InvalidRideStatusException;
import com.ridehailing.model.ride.Ride;
import com.ridehailing.model.user.Captain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Ratings: only for COMPLETED rides, once per side, 1 to 5 stars. */
public class RatingService {

    private final CaptainService captainService;

    public RatingService(CaptainService captainService) {
        this.captainService = captainService;
    }

    public void rateCaptain(Ride ride, int stars) throws InvalidRideStatusException {
        checkStars(stars);
        ride.markCaptainRated();              // throws unless COMPLETED and not rated yet
        ride.getCaptain().receiveRating(stars);
    }

    public void rateCustomer(Ride ride, int stars) throws InvalidRideStatusException {
        checkStars(stars);
        ride.markCustomerRated();
        ride.getCustomer().receiveRating(stars);
    }

    private void checkStars(int stars) {
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("Rating must be 1 to 5 stars, got " + stars);
        }
    }

    /** Captains with at least one rating, best average first. */
    public List<Captain> leaderboard() {
        List<Captain> rated = new ArrayList<>();
        for (Captain captain : captainService.getAllCaptains()) {
            if (captain.getRatingCount() > 0) {
                rated.add(captain);
            }
        }
        rated.sort(new Comparator<Captain>() {
            @Override
            public int compare(Captain a, Captain b) {
                int byAverage = Double.compare(b.getAverageRating(), a.getAverageRating());
                if (byAverage != 0) {
                    return byAverage;
                }
                return Integer.compare(b.getRatingCount(), a.getRatingCount());
            }
        });
        return rated;
    }

    public List<Captain> flaggedCaptains() {
        List<Captain> flagged = new ArrayList<>();
        for (Captain captain : captainService.getAllCaptains()) {
            if (captain.isFlagged()) {
                flagged.add(captain);
            }
        }
        return flagged;
    }
}
