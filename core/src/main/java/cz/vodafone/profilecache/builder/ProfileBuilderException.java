package cz.vodafone.profilecache.builder;

public class ProfileBuilderException extends Exception {

    public ProfileBuilderException() {
    }

    public ProfileBuilderException(String message) {
        super(message);
    }

    public ProfileBuilderException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProfileBuilderException(Throwable cause) {
        super(cause);
    }

    public ProfileBuilderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
