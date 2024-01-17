package cz.vodafone.profilecache.events.handlers;

import cz.vodafone.common.xml.common.Event;
import cz.vodafone.customersubscriber.xml.events.SubscriberChangeEvent;
import cz.vodafone.profilecache.events.AttributeHandler;
import cz.vodafone.profilecache.events.AttributeHandlerException;
import cz.vodafone.profilecache.model.Attribute;
import cz.vodafone.profilecache.model.Profile;
import cz.vodafone.profilecache.model.impl.AttributeImpl;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SubscriberChangeHandler implements AttributeHandler {

    private static final Logger LOG = Logger.getLogger(SubscriberChangeHandler.class);

    @Override
    public List<Attribute> handle(Profile profile, boolean newProfile, Event event) throws AttributeHandlerException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method handle(%s,%s,%s) invoked ...",
                    profile.getMsisdn(), newProfile, event.getClass().getName()));
        }
        if (!HandlerUtils.isUpdateEventInValidTimeFrame(profile, newProfile, event, Profile.ATTR_IS_VOLTE)) {
            LOG.info("Subscriber change was not done due to obsolete event");
            return null;
        }

        List<Attribute> attributes = new ArrayList<>();

        // volte
        Boolean isVolteEnabled = ((SubscriberChangeEvent) event).isVolteEnabled();
        if (isVolteEnabled != null) {
            attributes.add(new AttributeImpl(Profile.ATTR_IS_VOLTE,
                    isVolteEnabled ? Profile.ATTR_VALUE_TRUE : Profile.ATTR_VALUE_FALSE,
                    new Date()));
            LOG.info(String.format("VOLTE attribute changed to %s", isVolteEnabled));
        }

        if (attributes.size() == 0) {
            LOG.info("No attribute changed by subscriber change event. Empty event or not recognized attribute.");
        }

        return attributes;
    }

}
