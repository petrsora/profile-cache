package cz.vodafone.profilecache.location;

public interface Location {

    String LOCATION_OFFNET = "OFFNET";
    String OPERATOR_ID_VFCZ = "213";

    String getLocation();

    String getOperatorId();

    boolean isOffnet();

}
