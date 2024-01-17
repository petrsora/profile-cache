package cz.vodafone.profilecache.persistence;

public class ProviderDaoException extends Exception {

    public ProviderDaoException() {
    }

    public ProviderDaoException(String message) {
        super(message);
    }

    public ProviderDaoException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProviderDaoException(Throwable cause) {
        super(cause);
    }

    public ProviderDaoException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
