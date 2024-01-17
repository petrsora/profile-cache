package cz.vodafone.profilecache.events;

public class EventHandlerRetryException extends Exception {

    public EventHandlerRetryException() {
    }

    public EventHandlerRetryException(String message) {
        super(message);
    }

    public EventHandlerRetryException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventHandlerRetryException(Throwable cause) {
        super(cause);
    }
}
