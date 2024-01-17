package cz.vodafone.profilecache.events;

public class AttributeHandlerException extends Exception {

    public AttributeHandlerException() {
    }

    public AttributeHandlerException(String message) {
        super(message);
    }

    public AttributeHandlerException(String message, Throwable cause) {
        super(message, cause);
    }

    public AttributeHandlerException(Throwable cause) {
        super(cause);
    }
}
