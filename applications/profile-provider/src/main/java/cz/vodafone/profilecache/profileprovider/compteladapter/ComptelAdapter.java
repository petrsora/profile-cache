package cz.vodafone.profilecache.profileprovider.compteladapter;

/**
 * Interface covering communication with Comptel adapter
 */
public interface ComptelAdapter {

    void setImsSubscription(String subscriberId, String privateUserId, String userPassword) throws ComptelAdapterException;

}
