package cz.vodafone.profilecache.location.impl;

import cz.vodafone.profilecache.location.Location;

public class LocationImpl implements Location {

    private String location;
    private String operatorId;

    public LocationImpl(String operatorId, String location) {
        if (location == null) {
            throw new IllegalArgumentException("Location is null");
        }
        if (operatorId == null) {
            throw new IllegalArgumentException("OperatorId is null");
        }
        this.location = location;
        this.operatorId = operatorId;
    }

    @Override
    public String getLocation() {
        return this.location;
    }

    @Override
    public String getOperatorId() {
        return this.operatorId;
    }

    @Override
    public boolean isOffnet() {
        return !Location.OPERATOR_ID_VFCZ.equals(this.operatorId);
    }
}
