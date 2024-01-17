package cz.vodafone.profilecache.events;

import cz.vodafone.common.xml.common.Event;
import cz.vodafone.profilecache.model.Attribute;
import cz.vodafone.profilecache.model.Profile;

import java.util.List;

public interface AttributeHandler {

    List<Attribute> handle(Profile profile, boolean newProfile, Event event) throws AttributeHandlerException;

}
