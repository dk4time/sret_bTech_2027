package com.ridehailing.service;

import com.ridehailing.exception.CaptainBusyException;
import com.ridehailing.exception.CaptainNotEligibleException;
import com.ridehailing.model.common.Location;
import com.ridehailing.model.common.Money;
import com.ridehailing.model.user.Captain;
import com.ridehailing.model.user.CaptainStatus;
import com.ridehailing.time.SimulatedClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Registers captains and runs their day: KYC, online/offline, settling cash dues. */
public class CaptainService {

    private final SimulatedClock clock;
    private final List<Captain> captains = new ArrayList<>();

    public CaptainService(SimulatedClock clock) {
        this.clock = clock;
    }

    /** New captains always start as KYC_PENDING (the Captain constructor guarantees it). */
    public Captain register(Captain captain) {
        if (captains.contains(captain)) {
            throw new IllegalArgumentException(captain.getName() + " is already registered");
        }
        for (Captain existing : captains) {
            if (existing.getVehicle().equals(captain.getVehicle())) {
                throw new IllegalArgumentException(captain.getVehicle().getRegistrationNumber()
                        + " is already registered to " + existing.getName());
            }
        }
        captains.add(captain);
        return captain;
    }

    public void verifyKyc(Captain captain) {
        captain.verifyKyc();
    }

    public void goOnline(Captain captain, Location at) throws CaptainNotEligibleException {
        captain.goOnline(at, clock.now());
    }

    public void goOffline(Captain captain) throws CaptainBusyException {
        captain.goOffline();
    }

    public Money settleDues(Captain captain) {
        return captain.settleDues();
    }

    public List<Captain> getAllCaptains() {
        return Collections.unmodifiableList(captains);
    }

    /** Captains who are online right now: free or on a ride. */
    public int countOnline() {
        int online = 0;
        for (Captain captain : captains) {
            if (captain.getStatus() == CaptainStatus.AVAILABLE || captain.getStatus() == CaptainStatus.ON_RIDE) {
                online++;
            }
        }
        return online;
    }

    public Captain findByName(String name) {
        for (Captain captain : captains) {
            if (captain.getName().equals(name)) {
                return captain;
            }
        }
        throw new IllegalArgumentException("No captain named " + name);
    }
}
