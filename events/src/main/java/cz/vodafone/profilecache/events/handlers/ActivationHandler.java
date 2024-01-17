package cz.vodafone.profilecache.events.handlers;

import cz.vodafone.common.xml.common.Event;
import cz.vodafone.customersubscriber.xml.events.FMSActivationEvent;
import cz.vodafone.customersubscriber.xml.events.SubscriberActivationEvent;
import cz.vodafone.profilecache.events.AttributeHandler;
import cz.vodafone.profilecache.events.AttributeHandlerException;
import cz.vodafone.profilecache.model.Attribute;
import cz.vodafone.profilecache.model.Profile;
import cz.vodafone.profilecache.model.impl.AttributeImpl;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ActivationHandler implements AttributeHandler {

    private static final Logger LOG = Logger.getLogger(ActivationHandler.class);

    @Override
    public List<Attribute> handle(Profile profile, boolean newProfile, Event event) throws AttributeHandlerException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method handle(%s,%s,%s) invoked ...",
                    profile.getMsisdn(), newProfile, event.getClass().getName()));
        }
        String subscriberType;
        Boolean isVolteEnabled = null;
        if (event instanceof SubscriberActivationEvent) {
            subscriberType = ((SubscriberActivationEvent) event).getSubscriberType();
            isVolteEnabled = ((SubscriberActivationEvent) event).isVolteEnabled();
        } else if (event instanceof FMSActivationEvent) {
            subscriberType = ((FMSActivationEvent) event).getSubscriberType();
        } else {
            throw new AttributeHandlerException(String.format("Unsupported activation event: %s", event.getClass().getName()));
        }

        List<Attribute> attributes = new ArrayList<Attribute>();

        // subscriber type
        if ("PREPAID".equalsIgnoreCase(subscriberType)) {
            attributes.add(new AttributeImpl(Profile.ATTR_IS_PREPAID, Profile.ATTR_VALUE_TRUE, new Date()));
            LOG.info("Activated profile is prepaid");
        } else {
            attributes.add(new AttributeImpl(Profile.ATTR_IS_PREPAID, Profile.ATTR_VALUE_FALSE, new Date()));
            LOG.info("Activated profile is postpaid");
        }

        // volte
        if (isVolteEnabled != null) {
            if (isVolteEnabled) {
                attributes.add(new AttributeImpl(Profile.ATTR_IS_VOLTE, Profile.ATTR_VALUE_TRUE, new Date()));
                LOG.info("Activated profile is VOLTE enabled");
            } else {
                attributes.add(new AttributeImpl(Profile.ATTR_IS_VOLTE, Profile.ATTR_VALUE_FALSE, new Date()));
                LOG.info("Activated profile is VOLTE disabled");
            }
        } else {
            LOG.info("Activated profile does not have VOLTE attribute defined");
        }

        return attributes;
    }
}
