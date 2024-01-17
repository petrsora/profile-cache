package cz.vodafone.profilecache.events.handlers;

import cz.vodafone.common.xml.common.Event;
import cz.vodafone.customersubscriber.xml.events.CommunicationProfileChangeEvent;
import cz.vodafone.customersubscriber.xml.events.SubscriberPost2PreMigrationEvent;
import cz.vodafone.customersubscriber.xml.events.SubscriberPre2PostMigrationEvent;
import cz.vodafone.profilecache.events.AttributeHandler;
import cz.vodafone.profilecache.events.AttributeHandlerException;
import cz.vodafone.profilecache.model.Attribute;
import cz.vodafone.profilecache.model.Profile;
import cz.vodafone.profilecache.model.impl.AttributeImpl;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CommunicationProfileChangeHandler implements AttributeHandler {

    private static final Logger LOG = Logger.getLogger(CommunicationProfileChangeHandler.class);

    @Override
    public List<Attribute> handle(Profile profile, boolean newProfile, Event event) throws AttributeHandlerException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method handle(%s,%s,%s) invoked ...",
                    profile.getMsisdn(), newProfile, event.getClass().getName()));
        }
        if (!HandlerUtils.isUpdateEventInValidTimeFrame(profile, newProfile, event, Profile.ATTR_SCHEDULING_PROFILE)) {
            LOG.info("Communication profile change was not done due to obsolete event");
            return null;
        }

        String communicationProfile = ((CommunicationProfileChangeEvent) event).getCommunicationProfile();
        if (communicationProfile == null || communicationProfile.length() == 0) {
            throw new AttributeHandlerException("Missing communication profile in incoming event");
        }

        List<Attribute> attributes = new ArrayList<Attribute>();
        attributes.add(new AttributeImpl(Profile.ATTR_SCHEDULING_PROFILE, communicationProfile, new Date()));
        LOG.info(String.format("Communication profile changed to %s", communicationProfile));
        return attributes;
    }

}
