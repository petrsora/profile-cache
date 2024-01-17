package cz.vodafone.profilecache.location.impl;

import cz.vodafone.profilecache.location.Location;
import cz.vodafone.profilecache.location.LocationProvider;
import cz.vodafone.profilecache.location.LocationProviderException;

public class LocationProviderMock implements LocationProvider {

    private boolean usedLocation = false;
    private boolean usedMsisdn = false;
    private Location location;
    private String msisdn;

    public void useLocation(Location location) {
        this.location = location;
    }

    public void useMsisdn(String msisdn) { this.msisdn = msisdn; }

    public boolean wasUsedLocation() {
        return usedLocation;
    }

    public boolean wasUsedMsisdn() {
        return usedMsisdn;
    }

    public void resetUsed() {
        this.usedLocation = false;
        this.usedMsisdn = false;
    }


    @Override
    public Location getLocation(String msisdn) throws LocationProviderException {
        this.usedLocation = true;
        return this.location;
    }

    @Override
    public String getMsisdn(String imsi) throws LocationProviderException {
        this.usedMsisdn = true;
        return this.msisdn;
    }

}
