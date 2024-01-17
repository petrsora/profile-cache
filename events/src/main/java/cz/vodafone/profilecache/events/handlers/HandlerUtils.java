package cz.vodafone.profilecache.events.handlers;

import cz.vodafone.common.xml.common.Event;
import cz.vodafone.profilecache.events.AttributeHandlerException;
import cz.vodafone.profilecache.model.Attribute;
import cz.vodafone.profilecache.model.Profile;
import org.apache.log4j.Logger;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;

public abstract class HandlerUtils {

    private static final Logger LOG = Logger.getLogger(HandlerUtils.class);

    public static boolean isUpdateEventInValidTimeFrame(Profile profile, boolean newProfile, Event event, String attribute)
            throws AttributeHandlerException {
        if (newProfile) {
            LOG.info("Newly generated profile. Updating attribute...");
            return true;
        }

        Date effectiveDate = getEffectiveDate(event);

        Attribute attr = profile.getAttribute(attribute);
        if (attr == null) {
            LOG.info(String.format("Attribute (%s) does not exist yet. Updating attribute...", attribute));
            return true;
        }

        Date lastUpdate = attr.getLastUpdate();
        if (lastUpdate == null) {
            LOG.info("Last update of attribute is not specified. Updating attribute...");
            return true;
        }

        if (lastUpdate.getTime() > effectiveDate.getTime()) {
            LOG.info("Attribute was updated with more recent event. Attribute will NOT be updated");
            return false;
        } else {
            LOG.info("New event has more recent value than old value. Updating attribute...");
            return true;
        }
    }

    public static Date getEffectiveDate(Event event) throws AttributeHandlerException {
        if (event.getHeader() == null) {
            throw new AttributeHandlerException("Missing header element");
        }
        XMLGregorianCalendar effectiveDate = event.getHeader().getEffectiveDate();
        if (effectiveDate == null) {
            throw new AttributeHandlerException("Missing effectiveDate element");
        }
        return effectiveDate.toGregorianCalendar().getTime();
    }

}
