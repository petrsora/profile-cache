package cz.vodafone.profilecache.events.handlers;

import cz.vodafone.common.xml.common.Event;
import cz.vodafone.customersubscriber.xml.events.RestrictionInstanceChangeEvent;
import cz.vodafone.profilecache.events.AttributeHandler;
import cz.vodafone.profilecache.events.AttributeHandlerException;
import cz.vodafone.profilecache.model.Attribute;
import cz.vodafone.profilecache.model.Profile;
import cz.vodafone.profilecache.model.impl.AttributeImpl;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RestrictionInstanceChangeHandler implements AttributeHandler {

    private static final Logger LOG = Logger.getLogger(RestrictionInstanceChangeHandler.class);

    @Override
    public List<Attribute> handle(Profile profile, boolean newProfile, Event event) throws AttributeHandlerException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method handle(%s,%s,%s) invoked ...",
                    profile.getMsisdn(), newProfile, event.getClass().getName()));
        }
        RestrictionInstanceChangeEvent restrictionChangeEvent = (RestrictionInstanceChangeEvent) event;

        boolean restrictionStatusActive;
        if ("ACTIVE".equals(restrictionChangeEvent.getRestrictionStatus())) {
            restrictionStatusActive = true;
        } else if ("INACTIVE".equals(restrictionChangeEvent.getRestrictionStatus())) {
            restrictionStatusActive = false;
        } else {
            throw new AttributeHandlerException(
                    String.format("Unsupported restriction status (%s)", restrictionChangeEvent.getRestrictionStatus()));
        }

        if (restrictionChangeEvent.getRestrictionCode() == null || restrictionChangeEvent.getRestrictionCode().length() == 0) {
            throw new AttributeHandlerException("Missing mandatory restriction code information");
        }

        int restrictionCode;
        try {
            restrictionCode = Integer.parseInt(restrictionChangeEvent.getRestrictionCode());
        } catch (NumberFormatException e) {
            String textRestrictionCode = restrictionChangeEvent.getRestrictionCode();
            if ("STOLEN_LOST".equals(textRestrictionCode)) {
                restrictionCode = 9;
            } else if ("PR_SMS".equals(textRestrictionCode)) {
                return handleAttributeUpdate(Profile.ATTR_HAS_PRSMS_BARRING,
                        restrictionStatusActive ? Profile.ATTR_VALUE_TRUE : Profile.ATTR_VALUE_FALSE,
                        profile, newProfile, event);
            } else if ("MPENEBAR".equals(textRestrictionCode)) {
                return handleAttributeUpdate(Profile.ATTR_HAS_MPENEZENKA_BARRING,
                        restrictionStatusActive ? Profile.ATTR_VALUE_TRUE : Profile.ATTR_VALUE_FALSE,
                        profile, newProfile, event);
            } else {
                LOG.info("Ignoring unsupported restriction code (%s)");
                return null;
            }
        }

        if (restrictionCode < 0 || restrictionCode > 10) {
            throw new AttributeHandlerException(String.format("Illegal restriction code (%d)", restrictionCode));
        }

        if (restrictionCode == 0) {
            // restriction code 0 is dropping all restrictions
            if (!restrictionStatusActive) {
                throw new AttributeHandlerException("Illegal restriction status for 0 restriction code");
            }
            return handleAttributeUpdate(Profile.ATTR_IS_RESTRICTED, "0",
                    profile, newProfile, event);
        }

        // restriction code is between 1 and 9
        int existingRestriction = 0;
        Attribute restrictionAttribute = profile.getAttribute(Profile.ATTR_IS_RESTRICTED);
        if (restrictionAttribute != null && restrictionAttribute.getValue() != null) {
            try {
                existingRestriction = Integer.parseInt(restrictionAttribute.getValue());
            } catch (NumberFormatException e) {
                LOG.warn(String.format("Error while parsing restriction attribute (%s). Value cleared",
                        restrictionAttribute.getValue()));
                existingRestriction = 0;
            }
        }

        int newRestriction;
        if (restrictionStatusActive) {
            // setting corresponding bit
            newRestriction = existingRestriction | 1 << (restrictionCode - 1);
        } else {
            // setting off corresponding bit
            newRestriction = existingRestriction & (~(1 << (restrictionCode - 1)));
        }

        return handleAttributeUpdate(Profile.ATTR_IS_RESTRICTED, String.valueOf(newRestriction),
                profile, newProfile, event);
    }

    private List<Attribute> handleAttributeUpdate(String attrName, String attrValue,
                                                  Profile profile, boolean newProfile, Event event)
            throws AttributeHandlerException {
        if (!HandlerUtils.isUpdateEventInValidTimeFrame(profile, newProfile, event, attrName)) {
            LOG.info(String.format("Attribute %s change was not done due to obsolete event", attrName));
            return null;
        }

        List<Attribute> attributes = new ArrayList<Attribute>();
        attributes.add(new AttributeImpl(attrName, attrValue, new Date()));
        LOG.info(String.format("Attribute %s set to %s", attrName, attrValue));
        return attributes;
    }

}
