package cz.vodafone.profilecache.events;

import cz.vodafone.common.xml.common.Event;
import cz.vodafone.common.xml.customer.RestrictionEvent;
import cz.vodafone.common.xml.customer.SubscriberEvent;
import cz.vodafone.customersubscriber.xml.events.*;
import cz.vodafone.profilecache.builder.ProfileBuilder;
import cz.vodafone.profilecache.builder.ProfileBuilderException;
import cz.vodafone.profilecache.cache.Cache;
import cz.vodafone.profilecache.cache.CacheException;
import cz.vodafone.profilecache.events.handlers.*;
import cz.vodafone.profilecache.location.Location;
import cz.vodafone.profilecache.location.impl.LocationImpl;
import cz.vodafone.profilecache.model.Attribute;
import cz.vodafone.profilecache.model.MsisdnValidator;
import cz.vodafone.profilecache.model.Profile;
import cz.vodafone.profilecache.model.impl.AttributeImpl;
import cz.vodafone.profilecache.persistence.ProviderDao;
import cz.vodafone.profilecache.persistence.ProviderDaoException;
import org.apache.log4j.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.CharArrayReader;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventHandlerImpl implements EventHandler {

    private static Logger LOG = Logger.getLogger(EventHandlerImpl.class);

    private JAXBContext context;

    private MsisdnValidator msisdnValidator;
    private ProviderDao providerDao;
    private ProfileBuilder profileBuilder;
    private Cache cache;

    private Map<String, AttributeHandler> handlers;

    public EventHandlerImpl(MsisdnValidator msisdnValidator, ProviderDao providerDao, ProfileBuilder profileBuilder,
                            Cache cache) throws JAXBException {

        this.msisdnValidator = msisdnValidator;
        this.providerDao = providerDao;
        this.profileBuilder = profileBuilder;
        this.cache = cache;

        context = JAXBContext.newInstance("cz.vodafone.customersubscriber.xml.events");
        initializeHandlers();
    }

    private void initializeHandlers() {
        handlers = new HashMap<String, AttributeHandler>();

        // activation events
        ActivationHandler activationHandler = new ActivationHandler();
        handlers.put(SubscriberActivationEvent.class.getName(), activationHandler);
        handlers.put(FMSActivationEvent.class.getName(), activationHandler);

        // attribute update events
        Pre2PostMigrationHandler pre2PostMigrationHandler = new Pre2PostMigrationHandler();
        handlers.put(SubscriberPost2PreMigrationEvent.class.getName(), pre2PostMigrationHandler);
        handlers.put(SubscriberPre2PostMigrationEvent.class.getName(), pre2PostMigrationHandler);

        handlers.put(ServiceProductInstanceChangeEvent.class.getName(), new ServiceProductInstanceChangeHandler());
        handlers.put(RestrictionInstanceChangeEvent.class.getName(), new RestrictionInstanceChangeHandler());
        handlers.put(CommunicationProfileChangeEvent.class.getName(), new CommunicationProfileChangeHandler());
        handlers.put(SubscriberChangeEvent.class.getName(), new SubscriberChangeHandler());
    }

    @Override
    public void handle(String eventContent, String subOperator) throws EventHandlerRetryException, EventHandlerException {
        try {
            CharArrayReader reader = new CharArrayReader(eventContent.toCharArray());
            Unmarshaller unmarshaller = context.createUnmarshaller();
            Event event = (Event) unmarshaller.unmarshal(reader);

            // deactivation does not need any handler
            if (event instanceof SubscriberDeactivationEvent ||
                    event instanceof FMSDeactivationEvent) {
                processDeactivation(event);
                return;
            }

            // msisdn swap event
            if (event instanceof SubscriberMsisdnSwapEvent) {
                processMsisdnSwap(event, subOperator);
                return;
            }

            AttributeHandler attributeHandler = handlers.get(event.getClass().getName());
            if (attributeHandler == null) {
                throw new EventHandlerException(String.format("Unsupported event type: %s", event.getClass().getName()));
            }

            // activation
            if (event instanceof SubscriberActivationEvent ||
                    event instanceof FMSActivationEvent) {
                processActivation(event, attributeHandler, subOperator);
                return;
            }

            // other update events
            processUpdate(event, attributeHandler, subOperator);

        } catch (JAXBException e) {
            throw new EventHandlerException("Error while unmarshalling event", e);
        }

    }

    private void processActivation(Event event, AttributeHandler attributeHandler, String subOperator)
            throws EventHandlerRetryException, EventHandlerException {
        try {
            LOG.info("Activation process started");
            String msisdn = ((SubscriberEvent) event).getMsisdn();
            if (!msisdnValidator.isMsisdnValid(msisdn)) {
                throw new EventHandlerException(String.format("Not valid MSISDN %s", msisdn));
            }

            Profile profile = providerDao.getProfile(msisdn);

            if (profile != null) {
                LOG.info(String.format("Profile should not exist in DB as this is activation event. Removing from DB ... (%s)", msisdn));
                providerDao.deleteProfile(profile);
            } else {
                LOG.info(String.format("Profile does not exist in DB (%s)", msisdn));
            }

            if (cache.delete(msisdn)) {
                LOG.info(String.format("Profile deleted from cache (%s)", msisdn));
            } else {
                LOG.info(String.format("Profile was not registered in cache (%s)", msisdn));
            }

            Location location = new LocationImpl(Location.OPERATOR_ID_VFCZ, subOperator);
            profile = profileBuilder.buildProfile(msisdn, location);

            List<Attribute> attributes = attributeHandler.handle(profile, true, event);
            if (attributes != null) {
                for (Attribute attr : attributes) {
                    profile.setAttribute(attr);
                }
            }

            providerDao.insertProfile(profile);
            LOG.info(String.format("Profile inserted into DB (%s)", msisdn));

            cache.set(profile);
            LOG.info(String.format("Profile set to cache (%s)", msisdn));

            LOG.info(String.format("Activation process successfully ended (%s)", msisdn));
        } catch (ProviderDaoException e) {
            throw new EventHandlerRetryException("Error while working with DB in activation process", e);
        } catch (CacheException e) {
            throw new EventHandlerRetryException("Error while working with cache in activation process", e);
        } catch (ProfileBuilderException e) {
            throw new EventHandlerException("Error while building default profile in activation process", e);
        } catch (AttributeHandlerException e) {
            throw new EventHandlerException("Error while handling attributes in activation process", e);
        }
    }

    private void processDeactivation(Event event)
            throws EventHandlerRetryException, EventHandlerException {
        try {
            LOG.info("Deactivation process started");
            String msisdn = ((SubscriberEvent) event).getMsisdn();
            if (!msisdnValidator.isMsisdnValid(msisdn)) {
                throw new EventHandlerException(String.format("Not valid MSISDN %s", msisdn));
            }

            Profile profile = providerDao.getProfile(msisdn);
            if (profile != null) {
                LOG.info(String.format("Profile found in DB. Deleting profile from DB ... (%s)", msisdn));
                providerDao.deleteProfile(profile);
            } else {
                LOG.info(String.format("Profile not found in DB (%s)", msisdn));
            }

            if (cache.delete(msisdn)) {
                LOG.info(String.format("Profile deleted from cache (%s)", msisdn));
            } else {
                LOG.info(String.format("Profile was not registered in cache (%s)", msisdn));
            }
            LOG.info(String.format("Deactivation process successfully ended (%s)", msisdn));
        } catch (ProviderDaoException e) {
            throw new EventHandlerRetryException("Error while working with DB in deactivation process", e);
        } catch (CacheException e) {
            throw new EventHandlerRetryException("Error while working with cache in deactivation process", e);
        }
    }

    private void processUpdate(Event event, AttributeHandler attributeHandler, String subOperator)
            throws EventHandlerRetryException, EventHandlerException {
        try {
            LOG.info("Attribute update process started");
            String msisdn;
            if (event instanceof SubscriberEvent) {
                msisdn = ((SubscriberEvent) event).getMsisdn();
            } else if (event instanceof RestrictionEvent) {
                msisdn = ((RestrictionEvent) event).getMsisdn();
            } else {
                throw new EventHandlerException(String.format("Unsupported event type (%s)", event.getClass().getName()));
            }
            if (!msisdnValidator.isMsisdnValid(msisdn)) {
                throw new EventHandlerException(String.format("Not valid MSISDN %s", msisdn));
            }

            boolean newProfile = false;
            Profile profile = providerDao.getProfile(msisdn);

            if (profile == null) {
                LOG.info(String.format("Profile not found in DB. Building new default profile ... (%s)", msisdn));
                Location location = new LocationImpl(Location.OPERATOR_ID_VFCZ, subOperator);
                profile = profileBuilder.buildProfile(msisdn, location);
                newProfile = true;
            } else {
                LOG.info(String.format("Profile found in DB (%s)", msisdn));
                String storedLocation = profile.getAttribute(Profile.ATTR_LOCATION) != null ?
                        profile.getAttribute(Profile.ATTR_LOCATION).getValue() : null;
                if (!subOperator.equals(storedLocation)) {
                    LOG.warn(String.format("Something went wrong. " +
                            "Location of stored profile (%s) and received subOperator (%s) is not the same",
                            storedLocation, subOperator));
                }
            }

            List<Attribute> attributes = attributeHandler.handle(profile, newProfile, event);
            if (attributes != null) {
                for (Attribute attr : attributes) {
                    profile.setAttribute(attr);
                }
            }

            if (newProfile) {
                providerDao.insertProfile(profile);
                LOG.info(String.format("New profile inserted to DB (%s)", profile.getMsisdn()));
            } else {
                providerDao.updateAttributes(profile);
                LOG.info(String.format("Existing profile updated in DB (%s)", profile.getMsisdn()));
            }

            if (cache.delete(msisdn)) {
                LOG.info(String.format("Profile deleted from cache (%s)", msisdn));
            } else {
                LOG.info(String.format("Profile was not registered in cache (%s)", msisdn));
            }
            LOG.info(String.format("Attribute update process successfully ended (%s)", msisdn));
        } catch (ProviderDaoException e) {
            throw new EventHandlerRetryException("Error while working with DB in attribute update process", e);
        } catch (ProfileBuilderException e) {
            throw new EventHandlerException("Error while building default profile in attribute update process", e);
        } catch (AttributeHandlerException e) {
            throw new EventHandlerException("Error while handling attributes in attribute update process", e);
        } catch (CacheException e) {
            throw new EventHandlerRetryException("Error while working with cache in attribute update process", e);
        }

    }

    private void processMsisdnSwap(Event event, String subOperator)
            throws EventHandlerRetryException, EventHandlerException {
        try {
            LOG.info("Msisdn swap process started");
            SubscriberMsisdnSwapEvent swapEvent = (SubscriberMsisdnSwapEvent) event;
            String oldMsisdn = swapEvent.getMsisdn();
            String newMsisdn = swapEvent.getNewMsisdn();
            if (!msisdnValidator.isMsisdnValid(oldMsisdn)) {
                throw new EventHandlerException(String.format("Not valid old MSISDN %s", oldMsisdn));
            }
            if (!msisdnValidator.isMsisdnValid(newMsisdn)) {
                throw new EventHandlerException(String.format("Not valid new MSISDN %s", newMsisdn));
            }

            Profile newProfile = providerDao.getProfile(newMsisdn);
            if (newProfile != null) {
                processMsisdnSwapIfNewProfileExists(newProfile, oldMsisdn, subOperator);

                LOG.info(String.format("Msisdn swap process successfully ended (n=%s,o=%s)", newMsisdn, oldMsisdn));
                return;
            }

            Profile oldProfile = providerDao.getProfile(oldMsisdn);
            if (oldProfile == null) {
                processMsisdnSwapIfOldProfileDoesNotExist(oldMsisdn, newMsisdn, subOperator);

                LOG.info(String.format("Msisdn swap process successfully ended (n=%s,o=%s)", newMsisdn, oldMsisdn));
                return;
            }

            processMsisdnSwapIfOldProfileExists(oldProfile, newMsisdn, subOperator);

            LOG.info(String.format("Msisdn swap process successfully ended (n=%s,o=%s)", newMsisdn, oldMsisdn));
        } catch (ProviderDaoException e) {
            throw new EventHandlerRetryException("Error while working with DB in deactivation process", e);
        } catch (CacheException e) {
            throw new EventHandlerRetryException("Error while working with cache in deactivation process", e);
        } catch (ProfileBuilderException e) {
            throw new EventHandlerException("Error while building default profile in swap process", e);
        }
    }

    private void processMsisdnSwapIfOldProfileExists(Profile oldProfile, String newMsisdn, String subOperator)
            throws ProviderDaoException, EventHandlerException, CacheException {
        String oldMsisdn = oldProfile.getMsisdn();
        LOG.info(String.format(
                "Old profile is registered in DB, new profile is not. Swapping old MSISDN with new one (n=%s,o=%s)",
                newMsisdn, oldMsisdn));
        if (providerDao.updateMsisdn(oldMsisdn, newMsisdn)) {
            LOG.info(String.format("MSISDN successfully updated in DB (n=%s,o=%s)", newMsisdn, oldMsisdn));
        } else {
            throw new EventHandlerException("MSISDN was not updated in DB due to unknown reason");
        }

        if (cache.delete(oldMsisdn)) {
            LOG.info(String.format("Old profile deleted from cache (%s)", oldMsisdn));
        } else {
            LOG.info(String.format("Old profile is not registered in cache (%s)", newMsisdn));
        }

        Profile newProfile = providerDao.getProfile(newMsisdn);
        updateLocationInDb(newProfile, subOperator);

        newProfile.touchLastUpdate();
        cache.set(newProfile);
        LOG.info(String.format("New profile updated in cache (%s)", newMsisdn));
    }

    private void processMsisdnSwapIfOldProfileDoesNotExist(String oldMsisdn, String newMsisdn, String subOperator)
            throws ProfileBuilderException, ProviderDaoException, CacheException {
        LOG.info(String.format("Old profile is not stored in DB. New profile is going to be created (n=%s,o=%s)",
                newMsisdn, oldMsisdn));

        Location location = new LocationImpl(Location.OPERATOR_ID_VFCZ, subOperator);
        Profile newProfile = profileBuilder.buildProfile(newMsisdn, location);

        providerDao.insertProfile(newProfile);
        LOG.info(String.format("Profile inserted into DB (%s)", newMsisdn));

        cache.set(newProfile);
        LOG.info(String.format("Profile set to cache (%s)", newMsisdn));

        if (cache.delete(oldMsisdn)) {
            LOG.info(String.format("Old profile deleted from cache (%s)", oldMsisdn));
        } else {
            LOG.info(String.format("Old profile is not registered in cache (%s)", oldMsisdn));
        }
    }

    private void processMsisdnSwapIfNewProfileExists(Profile newProfile, String oldMsisdn, String subOperator)
            throws ProviderDaoException, CacheException {
        LOG.info(String.format("New profile is already stored in DB. We will keep it and try to remove the old one (n=%s, o=%s)",
                newProfile.getMsisdn(), oldMsisdn));
        Profile oldProfile = providerDao.getProfile(oldMsisdn);
        if (oldProfile != null) {
            LOG.info(String.format("Old profile is registered in DB. Deleting... (%s)", oldMsisdn));
            providerDao.deleteProfile(oldProfile);
            LOG.info(String.format("Old profile deleted from DB (%s)", oldMsisdn));
        } else {
            LOG.info(String.format("Old profile is not registered in DB (%s)", oldMsisdn));
        }

        if (cache.delete(oldMsisdn)) {
            LOG.info(String.format("Old profile deleted from cache (%s)", oldMsisdn));
        } else {
            LOG.info(String.format("Old profile is not registered in cache (%s)", oldMsisdn));
        }

        updateLocationInDb(newProfile, subOperator);

        newProfile.touchLastUpdate();
        cache.set(newProfile);
        LOG.info(String.format("New profile updated in cache (%s)", newProfile.getMsisdn()));
    }

    private void updateLocationInDb(Profile profile, String subOperator) throws ProviderDaoException {
        String location = profile.getAttribute(Profile.ATTR_LOCATION).getValue();
        if (!subOperator.equals(location)) {
            LOG.info(String.format("Incoming subOperator (%s) differs from existing location (%s). " +
                    "We will update location in DB to %s (%s)", subOperator, location, subOperator, profile.getMsisdn()));
            Attribute att = new AttributeImpl(Profile.ATTR_LOCATION, subOperator, new Date());
            profile.setAttribute(att);
            providerDao.updateAttributes(profile);
        }
    }

}
