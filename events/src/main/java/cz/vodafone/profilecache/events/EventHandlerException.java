package cz.vodafone.profilecache.events;

public class EventHandlerException extends Exception {

    public EventHandlerException() {
    }

    public EventHandlerException(String message) {
        super(message);
    }

    public EventHandlerException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventHandlerException(Throwable cause) {
        super(cause);
    }

}
