package cz.vodafone.profilecache.location;

public class LocationProviderException extends Exception {

    public LocationProviderException() {
    }

    public LocationProviderException(String message) {
        super(message);
    }

    public LocationProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public LocationProviderException(Throwable cause) {
        super(cause);
    }

    public LocationProviderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
