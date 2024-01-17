package cz.vodafone.profilecache.events.handlers;

import cz.vodafone.common.xml.common.Event;
import cz.vodafone.customersubscriber.xml.events.SubscriberPost2PreMigrationEvent;
import cz.vodafone.customersubscriber.xml.events.SubscriberPre2PostMigrationEvent;
import cz.vodafone.profilecache.events.AttributeHandler;
import cz.vodafone.profilecache.events.AttributeHandlerException;
import cz.vodafone.profilecache.events.EventHandlerImpl;
import cz.vodafone.profilecache.model.Attribute;
import cz.vodafone.profilecache.model.Profile;
import cz.vodafone.profilecache.model.impl.AttributeImpl;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Pre2PostMigrationHandler implements AttributeHandler {

    private static final Logger LOG = Logger.getLogger(Pre2PostMigrationHandler.class);

    @Override
    public List<Attribute> handle(Profile profile, boolean newProfile, Event event) throws AttributeHandlerException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method handle(%s,%s,%s) invoked ...",
                    profile.getMsisdn(), newProfile, event.getClass().getName()));
        }
        if (!HandlerUtils.isUpdateEventInValidTimeFrame(profile, newProfile, event, Profile.ATTR_IS_PREPAID)) {
            LOG.info("Pre2Post migration (or vice versa) was not done due to obsolete event");
            return null;
        }

        List<Attribute> attributes = new ArrayList<Attribute>();

        if (event instanceof SubscriberPost2PreMigrationEvent) {
            attributes.add(new AttributeImpl(Profile.ATTR_IS_PREPAID,Profile.ATTR_VALUE_TRUE,new Date()));
            LOG.info("Migrated to prepaid profile");
        } else if (event instanceof SubscriberPre2PostMigrationEvent) {
            attributes.add(new AttributeImpl(Profile.ATTR_IS_PREPAID,Profile.ATTR_VALUE_FALSE,new Date()));
            LOG.info("Migrated to postpaid profile");
        } else {
            throw new AttributeHandlerException(String.format("Unsupported Pre2PostMigration event: %s", event.getClass().getName()));
        }
        return attributes;
    }

}
