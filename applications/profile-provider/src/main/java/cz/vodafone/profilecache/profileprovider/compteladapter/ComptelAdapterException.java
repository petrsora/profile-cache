package cz.vodafone.profilecache.profileprovider.compteladapter;

/**
 * Generic exception indicating that something wrong happened when calling Comptel adapter.
 */
public class ComptelAdapterException extends Exception {

    public ComptelAdapterException(String message) {
        super(message);
    }

    public ComptelAdapterException(String message, Throwable cause) {
        super(message, cause);
    }
}
