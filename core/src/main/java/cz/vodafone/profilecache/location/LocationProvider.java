package cz.vodafone.profilecache.location;

public interface LocationProvider {

    Location getLocation(String msisdn) throws LocationProviderException;
    String getMsisdn(String imsi) throws LocationProviderException;

}
