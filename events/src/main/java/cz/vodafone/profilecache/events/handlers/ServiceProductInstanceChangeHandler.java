package cz.vodafone.profilecache.events.handlers;

import cz.vodafone.common.xml.common.Event;
import cz.vodafone.customersubscriber.xml.events.ServiceProductInstanceChangeEvent;
import cz.vodafone.profilecache.events.AttributeHandler;
import cz.vodafone.profilecache.events.AttributeHandlerException;
import cz.vodafone.profilecache.model.Attribute;
import cz.vodafone.profilecache.model.Profile;
import cz.vodafone.profilecache.model.impl.AttributeImpl;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ServiceProductInstanceChangeHandler implements AttributeHandler {

    private static final Logger LOG = Logger.getLogger(ServiceProductInstanceChangeHandler.class);

    @Override
    public List<Attribute> handle(Profile profile, boolean newProfile, Event event) throws AttributeHandlerException {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Method handle(%s,%s,%s) invoked ...",
                    profile.getMsisdn(), newProfile, event.getClass().getName()));
        }
        ServiceProductInstanceChangeEvent productChangeEvent = (ServiceProductInstanceChangeEvent) event;

        String productCode;
        String attrValue;
        if (productChangeEvent.getOldProductInstance() != null &&
                productChangeEvent.getOldProductInstance().getProductOfferingKey() != null &&
                productChangeEvent.getOldProductInstance().getProductOfferingKey().getProductCode() != null) {
            productCode = productChangeEvent.getOldProductInstance().getProductOfferingKey().getProductCode();
            attrValue = Profile.ATTR_VALUE_FALSE;
        } else if (productChangeEvent.getNewProductInstance() != null &&
                productChangeEvent.getNewProductInstance().getProductOfferingKey() != null &&
                productChangeEvent.getNewProductInstance().getProductOfferingKey().getProductCode() != null) {
            productCode = productChangeEvent.getNewProductInstance().getProductOfferingKey().getProductCode();
            attrValue = Profile.ATTR_VALUE_TRUE;
        } else {
            throw new AttributeHandlerException("Missing mandatory old or new product information");
        }

        String attrName;
        if ("ADULTBAR".equals(productCode)) {
            attrName = Profile.ATTR_IS_CHILD;
        } else if ("BARPRSMS".equals(productCode)) {
            attrName = Profile.ATTR_HAS_PRSMS_BARRING;
        } else if ("MPENEBAR".equals(productCode)) {
            attrName = Profile.ATTR_HAS_MPENEZENKA_BARRING;
        } else {
            throw new AttributeHandlerException(String.format("Unsupported product code (%s)", productCode));
        }

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
