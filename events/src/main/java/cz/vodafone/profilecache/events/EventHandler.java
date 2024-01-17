package cz.vodafone.profilecache.events;

public interface EventHandler {

    void handle(String event, String subOperator) throws EventHandlerRetryException, EventHandlerException;

}
