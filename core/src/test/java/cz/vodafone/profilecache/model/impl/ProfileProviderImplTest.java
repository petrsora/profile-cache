package cz.vodafone.profilecache.model.impl;

import cz.vodafone.profilecache.Utils;
import cz.vodafone.profilecache.builder.ProfileBuilder;
import cz.vodafone.profilecache.builder.impl.PropertiesProfileBuilder;
import cz.vodafone.profilecache.cache.MemcachedCache;
import cz.vodafone.profilecache.cache.MemcachedCacheTest;
import cz.vodafone.profilecache.location.impl.LocationImpl;
import cz.vodafone.profilecache.location.impl.LocationProviderMock;
import cz.vodafone.profilecache.maintenance.impl.StatusImpl;
import cz.vodafone.profilecache.model.GetProfileResponse;
import cz.vodafone.profilecache.location.Location;
import cz.vodafone.profilecache.model.Profile;
import cz.vodafone.profilecache.model.ProfileProvider;
import cz.vodafone.profilecache.model.ProfileProviderException;
import cz.vodafone.profilecache.persistence.ProviderDaoImpl;
import cz.vodafone.profilecache.persistence.ProviderDaoImplTest;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileReader;
import java.util.Date;
import java.util.Properties;

public class ProfileProviderImplTest {

    private static final Logger LOG = Logger.getLogger(ProfileProviderImplTest.class);

    private static final long LOCATION_EXPIRY_PERIOD = 3;

    private ProviderDaoImpl providerDao;
    private ProfileBuilder profileBuilder;
    private MemcachedCache memcachedCache;
    private LocationProviderMock locationProvider;

    private static final Location V2 = new LocationImpl(Location.OPERATOR_ID_VFCZ, "V2");
    private static final Location V4 = new LocationImpl(Location.OPERATOR_ID_VFCZ, "V4");
    private static final Location OFFNET = new LocationImpl("232", Location.LOCATION_OFFNET);

    @Before
    public void before() throws Exception {
        Utils.initializeLogging();

        if (!Utils.runIntegrationTests()) {
            return;
        }

        providerDao = new ProviderDaoImpl(ProviderDaoImplTest.getConnectionFactory());

        FileReader fr = new FileReader("core/src/test/resources/profiles.properties");
//        FileReader fr = new FileReader("src/test/resources/profiles.properties");
        Properties props = new Properties();
        props.load(fr);
        profileBuilder = new PropertiesProfileBuilder(props);

        memcachedCache = new MemcachedCache.Builder().
                setBinaryProtocol().
                setServers(MemcachedCacheTest.getServers()).
                build();

        locationProvider = new LocationProviderMock();

    }

    @After
    public void after() {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        memcachedCache.shutdown();
    }

    @Test
    public void tests() throws Exception {
        LOG.info("Entering method tests() ...");
        if (!Utils.runIntegrationTests()) {
            return;
        }

        String msisdn = "420777350243";

        ProfileProvider profileProvider = new ProfileProviderImpl.Builder().
                setCache(memcachedCache).
                setLocationProvider(locationProvider).
                setMsisdnValidator(new MsisdnValidatorImpl()).
                setImsiValidator(new ImsiValidatorImpl()).
                setProfileBuilder(profileBuilder).
                setProviderDao(providerDao).
                setLocationExpiryPeriod(LOCATION_EXPIRY_PERIOD).
                setDbOfflineMode(false).
                setStatus(new StatusImpl(5)).
                build();

        // cleaning before test
        memcachedCache.delete(msisdn);
        providerDao.deleteProfile(new ProfileImpl.Builder().
                setMsisdn(msisdn).
                setOperatorId(Location.OPERATOR_ID_VFCZ).
                setAttribute(new AttributeImpl(Profile.ATTR_LOCATION, "V2", new Date())).
                build());
        Assert.assertNull(providerDao.getProfile(msisdn));
        Assert.assertNull(memcachedCache.get(msisdn));

        LOG.info("profile not found in cache, building new default profile, inserted into db and updated in cache");
        locationProvider.useLocation(V2);
        GetProfileResponse getProfileResponse = profileProvider.getProfile(msisdn);
        Assert.assertEquals(GetProfileResponse.Stage.NEW_INSERTED, getProfileResponse.getStage());
        Profile profile = getProfileResponse.getProfile();
        Assert.assertNotNull(profile);
        Assert.assertEquals(profile.getOperatorId(), V2.getOperatorId());
        Assert.assertEquals(profile.getAttribute(Profile.ATTR_LOCATION).getValue(), V2.getLocation());
        Assert.assertNotNull(providerDao.getProfile(msisdn));
        Integer v2ProfileDbId = providerDao.getProfile(msisdn).getId();
        Assert.assertNotNull(memcachedCache.get(msisdn));
        Assert.assertTrue(locationProvider.wasUsedLocation());

        LOG.info("profile found in cache, not expired");
        locationProvider.resetUsed();
        getProfileResponse = profileProvider.getProfile(msisdn);
        profile = getProfileResponse.getProfile();
        Assert.assertEquals(GetProfileResponse.Stage.CACHED, getProfileResponse.getStage());
        Assert.assertNotNull(profile);
        Assert.assertEquals(profile.getOperatorId(), V2.getOperatorId());
        Assert.assertEquals(profile.getAttribute(Profile.ATTR_LOCATION).getValue(), V2.getLocation());
        Assert.assertFalse(locationProvider.wasUsedLocation());

        LOG.info("profile found in cache, but expired, location not changed, updated cache");
        Thread.sleep(LOCATION_EXPIRY_PERIOD * 1000);
        locationProvider.resetUsed();
        getProfileResponse = profileProvider.getProfile(msisdn);
        profile = getProfileResponse.getProfile();
        Assert.assertEquals(GetProfileResponse.Stage.CACHED_LOC_CHECKED, getProfileResponse.getStage());
        Assert.assertNotNull(profile);
        Assert.assertEquals(profile.getOperatorId(), V2.getOperatorId());
        Assert.assertEquals(profile.getAttribute(Profile.ATTR_LOCATION).getValue(), V2.getLocation());
        Assert.assertTrue(locationProvider.wasUsedLocation());

        LOG.info("profile found in cache, not expired");
        locationProvider.resetUsed();
        getProfileResponse = profileProvider.getProfile(msisdn);
        profile = getProfileResponse.getProfile();
        Assert.assertEquals(GetProfileResponse.Stage.CACHED, getProfileResponse.getStage());
        Assert.assertNotNull(profile);
        Assert.assertEquals(profile.getOperatorId(), V2.getOperatorId());
        Assert.assertEquals(profile.getAttribute(Profile.ATTR_LOCATION).getValue(), V2.getLocation());
        Assert.assertFalse(locationProvider.wasUsedLocation());

        LOG.info("profile not found in cache, found in db, location not changed");
        locationProvider.resetUsed();
        memcachedCache.delete(msisdn);
        getProfileResponse = profileProvider.getProfile(msisdn);
        profile = getProfileResponse.getProfile();
        Assert.assertEquals(GetProfileResponse.Stage.DB_LOC_CHECKED, getProfileResponse.getStage());
        Assert.assertNotNull(profile);
        Assert.assertEquals(profile.getOperatorId(), V2.getOperatorId());
        Assert.assertEquals(profile.getAttribute(Profile.ATTR_LOCATION).getValue(), V2.getLocation());
        Assert.assertTrue(locationProvider.wasUsedLocation());

        LOG.info("profile found in cache, but expired, location changed to V4, default profile built, updated in DB, updated in cache");
        Thread.sleep(LOCATION_EXPIRY_PERIOD * 1000);
        locationProvider.useLocation(V4);
        locationProvider.resetUsed();
        getProfileResponse = profileProvider.getProfile(msisdn);
        profile = getProfileResponse.getProfile();
        Assert.assertEquals(GetProfileResponse.Stage.NEW_UPDATED, getProfileResponse.getStage());
        Assert.assertNotNull(profile);
        Assert.assertEquals(profile.getOperatorId(), V4.getOperatorId());
        Assert.assertEquals(profile.getAttribute(Profile.ATTR_LOCATION).getValue(), V4.getLocation());
        Assert.assertEquals(providerDao.getProfile(msisdn).getAttribute(Profile.ATTR_LOCATION).getValue(), V4.getLocation());
        Assert.assertEquals(providerDao.getProfile(msisdn).getId().intValue(), v2ProfileDbId.intValue()); // DB id not changed, just changed attributes
        Assert.assertEquals(memcachedCache.get(msisdn).getAttribute(Profile.ATTR_LOCATION).getValue(), V4.getLocation());
        Assert.assertTrue(locationProvider.wasUsedLocation());

        LOG.info("profile found in cache, not expired");
        locationProvider.resetUsed();
        getProfileResponse = profileProvider.getProfile(msisdn);
        profile = getProfileResponse.getProfile();
        Assert.assertEquals(GetProfileResponse.Stage.CACHED, getProfileResponse.getStage());
        Assert.assertNotNull(profile);
        Assert.assertEquals(profile.getOperatorId(), V4.getOperatorId());
        Assert.assertEquals(profile.getAttribute(Profile.ATTR_LOCATION).getValue(), V4.getLocation());
        Assert.assertFalse(locationProvider.wasUsedLocation());

        LOG.info("profile found in cache, but expired, location changed to OFFNET, default profile built, deleted from DB, deleted in cache");
        Thread.sleep(LOCATION_EXPIRY_PERIOD * 1000);
        locationProvider.useLocation(OFFNET);
        locationProvider.resetUsed();
        getProfileResponse = profileProvider.getProfile(msisdn);
        profile = getProfileResponse.getProfile();
        Assert.assertEquals(GetProfileResponse.Stage.NEW_OFFNET_OLD_DELETED, getProfileResponse.getStage());
        Assert.assertNotNull(profile);
        Assert.assertEquals(profile.getOperatorId(), OFFNET.getOperatorId());
        Assert.assertEquals(profile.getAttribute(Profile.ATTR_LOCATION).getValue(), OFFNET.getLocation());
        Assert.assertNull(providerDao.getProfile(msisdn));
        Assert.assertNull(memcachedCache.get(msisdn));
        Assert.assertTrue(locationProvider.wasUsedLocation());

        LOG.info("profile not found in cache, not found in DB, location OFFNET, default profile built");
        locationProvider.useLocation(OFFNET);
        locationProvider.resetUsed();
        getProfileResponse = profileProvider.getProfile(msisdn);
        profile = getProfileResponse.getProfile();
        Assert.assertEquals(GetProfileResponse.Stage.NEW_OFFNET, getProfileResponse.getStage());
        Assert.assertNotNull(profile);
        Assert.assertEquals(profile.getOperatorId(), OFFNET.getOperatorId());
        Assert.assertEquals(profile.getAttribute(Profile.ATTR_LOCATION).getValue(), OFFNET.getLocation());
        Assert.assertNull(providerDao.getProfile(msisdn));
        Assert.assertNull(memcachedCache.get(msisdn));
        Assert.assertTrue(locationProvider.wasUsedLocation());

        // cleaning after test
        memcachedCache.delete(msisdn);
    }

    @Test
    public void testDbOfflineMode() throws Exception {
        LOG.info("Entering method testDbOfflineMode() ...");
        if (!Utils.runIntegrationTests()) {
            return;
        }
        String msisdn1 = "420777350243";
        String msisdn2 = "420777350244";

        ProfileProvider profileProvider = new ProfileProviderImpl.Builder().
                setCache(memcachedCache).
                setLocationProvider(locationProvider).
                setMsisdnValidator(new MsisdnValidatorImpl()).
                setImsiValidator(new ImsiValidatorImpl()).
                setProfileBuilder(profileBuilder).
                setProviderDao(providerDao).
                setLocationExpiryPeriod(LOCATION_EXPIRY_PERIOD).
                setDbOfflineMode(true).
                setStatus(new StatusImpl(5)).
                build();

        // cleaning before test
        memcachedCache.set(new ProfileImpl.Builder().
                setMsisdn(msisdn1).
                setOperatorId(Location.OPERATOR_ID_VFCZ).
                setAttribute(new AttributeImpl(Profile.ATTR_LOCATION, "V2", new Date())).
                build());
        memcachedCache.delete(msisdn2);
        Assert.assertNotNull(memcachedCache.get(msisdn1));
        Assert.assertNull(memcachedCache.get(msisdn2));

        LOG.info("profile found in cache");
        locationProvider.useLocation(V2);
        locationProvider.resetUsed();
        GetProfileResponse getProfileResponse = profileProvider.getProfile(msisdn1);
        Assert.assertEquals(GetProfileResponse.Stage.CACHED_DB_OFFLINE, getProfileResponse.getStage());
        Profile profile = getProfileResponse.getProfile();
        Assert.assertNotNull(profile);
        Assert.assertEquals(profile.getOperatorId(), V2.getOperatorId());
        Assert.assertEquals(profile.getAttribute(Profile.ATTR_LOCATION).getValue(), V2.getLocation());
        Assert.assertNotNull(memcachedCache.get(msisdn1));
        Assert.assertFalse(locationProvider.wasUsedLocation());

        LOG.info("profile not found in cache");
        locationProvider.useLocation(V2);
        locationProvider.resetUsed();
        try {
            profileProvider.getProfile(msisdn2);
            throw new Exception("Should not come here");
        } catch (ProfileProviderException e) {
            Assert.assertEquals(ProfileProviderException.ERROR_CODE_MSISDN_NOT_FOUND_1, e.getErrorCode());
            Assert.assertEquals(ProfileProviderException.ERROR_DESC_MSISDN_NOT_FOUND, e.getErrorDescription());
            Assert.assertNull(memcachedCache.get(msisdn2));
            Assert.assertFalse(locationProvider.wasUsedLocation());
        }

        // cleaning after test
        memcachedCache.delete(msisdn1);
        memcachedCache.delete(msisdn2);
    }

    @Test
    public void testGetByImsi() throws Exception {
        LOG.info("Entering method testGetByImsi() ...");
        if (!Utils.runIntegrationTests()) {
            return;
        }

        String imsi = "230030090937423";
        String msisdn = "420777350243";

        ProfileProvider profileProvider = new ProfileProviderImpl.Builder().
                setCache(memcachedCache).
                setLocationProvider(locationProvider).
                setMsisdnValidator(new MsisdnValidatorImpl()).
                setImsiValidator(new ImsiValidatorImpl()).
                setProfileBuilder(profileBuilder).
                setProviderDao(providerDao).
                setLocationExpiryPeriod(LOCATION_EXPIRY_PERIOD).
                setDbOfflineMode(false).
                setStatus(new StatusImpl(5)).
                build();

        // cleaning before test
        memcachedCache.delete(imsi);
        memcachedCache.delete(msisdn);
        providerDao.deleteProfile(new ProfileImpl.Builder().
                setMsisdn(msisdn).
                setOperatorId(Location.OPERATOR_ID_VFCZ).
                setAttribute(new AttributeImpl(Profile.ATTR_LOCATION, "V2", new Date())).
                build());
        Assert.assertNull(providerDao.getProfile(msisdn));
        Assert.assertNull(memcachedCache.get(imsi));
        Assert.assertNull(memcachedCache.get(msisdn));

        LOG.info("imsi not found in cache, profile not found");
        locationProvider.useMsisdn(msisdn);
        locationProvider.useLocation(V2);
        GetProfileResponse getProfileResponse = profileProvider.getProfileByImsi(imsi);
        Assert.assertEquals(GetProfileResponse.Stage.NEW_INSERTED, getProfileResponse.getStage());
        Profile profile = getProfileResponse.getProfile();
        Assert.assertNotNull(profile);
        Assert.assertEquals(profile.getOperatorId(), V2.getOperatorId());
        Assert.assertEquals(profile.getAttribute(Profile.ATTR_LOCATION).getValue(), V2.getLocation());
        Assert.assertNotNull(providerDao.getProfile(msisdn));
        Integer v2ProfileDbId = providerDao.getProfile(msisdn).getId();
        Assert.assertNotNull(memcachedCache.get(msisdn));
        Assert.assertTrue(locationProvider.wasUsedMsisdn());
        Assert.assertTrue(locationProvider.wasUsedLocation());

        LOG.info("profile found in cache, not expired");
        locationProvider.resetUsed();
        getProfileResponse = profileProvider.getProfileByImsi(imsi);
        profile = getProfileResponse.getProfile();
        Assert.assertEquals(GetProfileResponse.Stage.CACHED, getProfileResponse.getStage());
        Assert.assertNotNull(profile);
        Assert.assertEquals(profile.getOperatorId(), V2.getOperatorId());
        Assert.assertEquals(profile.getAttribute(Profile.ATTR_LOCATION).getValue(), V2.getLocation());
        Assert.assertFalse(locationProvider.wasUsedMsisdn());
        Assert.assertFalse(locationProvider.wasUsedLocation());
//
//        LOG.info("profile found in cache, but expired, location not changed, updated cache");
//        Thread.sleep(LOCATION_EXPIRY_PERIOD * 1000);
//        locationProvider.resetUsed();
//        getProfileResponse = profileProvider.getProfile(msisdn);
//        profile = getProfileResponse.getProfile();
//        Assert.assertEquals(GetProfileResponse.Stage.CACHED_LOC_CHECKED, getProfileResponse.getStage());
//        Assert.assertNotNull(profile);
//        Assert.assertEquals(profile.getOperatorId(), V2.getOperatorId());
//        Assert.assertEquals(profile.getAttribute(Profile.ATTR_LOCATION).getValue(), V2.getLocation());
//        Assert.assertTrue(locationProvider.wasUsedLocation());
//
//        LOG.info("profile found in cache, not expired");
//        locationProvider.resetUsed();
//        getProfileResponse = profileProvider.getProfile(msisdn);
//        profile = getProfileResponse.getProfile();
//        Assert.assertEquals(GetProfileResponse.Stage.CACHED, getProfileResponse.getStage());
//        Assert.assertNotNull(profile);
//        Assert.assertEquals(profile.getOperatorId(), V2.getOperatorId());
//        Assert.assertEquals(profile.getAttribute(Profile.ATTR_LOCATION).getValue(), V2.getLocation());
//        Assert.assertFalse(locationProvider.wasUsedLocation());
//
//        LOG.info("profile not found in cache, found in db, location not changed");
//        locationProvider.resetUsed();
//        memcachedCache.delete(msisdn);
//        getProfileResponse = profileProvider.getProfile(msisdn);
//        profile = getProfileResponse.getProfile();
//        Assert.assertEquals(GetProfileResponse.Stage.DB_LOC_CHECKED, getProfileResponse.getStage());
//        Assert.assertNotNull(profile);
//        Assert.assertEquals(profile.getOperatorId(), V2.getOperatorId());
//        Assert.assertEquals(profile.getAttribute(Profile.ATTR_LOCATION).getValue(), V2.getLocation());
//        Assert.assertTrue(locationProvider.wasUsedLocation());
//
//        LOG.info("profile found in cache, but expired, location changed to V4, default profile built, updated in DB, updated in cache");
//        Thread.sleep(LOCATION_EXPIRY_PERIOD * 1000);
//        locationProvider.useLocation(V4);
//        locationProvider.resetUsed();
//        getProfileResponse = profileProvider.getProfile(msisdn);
//        profile = getProfileResponse.getProfile();
//        Assert.assertEquals(GetProfileResponse.Stage.NEW_UPDATED, getProfileResponse.getStage());
//        Assert.assertNotNull(profile);
//        Assert.assertEquals(profile.getOperatorId(), V4.getOperatorId());
//        Assert.assertEquals(profile.getAttribute(Profile.ATTR_LOCATION).getValue(), V4.getLocation());
//        Assert.assertEquals(providerDao.getProfile(msisdn).getAttribute(Profile.ATTR_LOCATION).getValue(), V4.getLocation());
//        Assert.assertEquals(providerDao.getProfile(msisdn).getId().intValue(), v2ProfileDbId.intValue()); // DB id not changed, just changed attributes
//        Assert.assertEquals(memcachedCache.get(msisdn).getAttribute(Profile.ATTR_LOCATION).getValue(), V4.getLocation());
//        Assert.assertTrue(locationProvider.wasUsedLocation());
//
//        LOG.info("profile found in cache, not expired");
//        locationProvider.resetUsed();
//        getProfileResponse = profileProvider.getProfile(msisdn);
//        profile = getProfileResponse.getProfile();
//        Assert.assertEquals(GetProfileResponse.Stage.CACHED, getProfileResponse.getStage());
//        Assert.assertNotNull(profile);
//        Assert.assertEquals(profile.getOperatorId(), V4.getOperatorId());
//        Assert.assertEquals(profile.getAttribute(Profile.ATTR_LOCATION).getValue(), V4.getLocation());
//        Assert.assertFalse(locationProvider.wasUsedLocation());
//
//        LOG.info("profile found in cache, but expired, location changed to OFFNET, default profile built, deleted from DB, deleted in cache");
//        Thread.sleep(LOCATION_EXPIRY_PERIOD * 1000);
//        locationProvider.useLocation(OFFNET);
//        locationProvider.resetUsed();
//        getProfileResponse = profileProvider.getProfile(msisdn);
//        profile = getProfileResponse.getProfile();
//        Assert.assertEquals(GetProfileResponse.Stage.NEW_OFFNET_OLD_DELETED, getProfileResponse.getStage());
//        Assert.assertNotNull(profile);
//        Assert.assertEquals(profile.getOperatorId(), OFFNET.getOperatorId());
//        Assert.assertEquals(profile.getAttribute(Profile.ATTR_LOCATION).getValue(), OFFNET.getLocation());
//        Assert.assertNull(providerDao.getProfile(msisdn));
//        Assert.assertNull(memcachedCache.get(msisdn));
//        Assert.assertTrue(locationProvider.wasUsedLocation());
//
//        LOG.info("profile not found in cache, not found in DB, location OFFNET, default profile built");
//        locationProvider.useLocation(OFFNET);
//        locationProvider.resetUsed();
//        getProfileResponse = profileProvider.getProfile(msisdn);
//        profile = getProfileResponse.getProfile();
//        Assert.assertEquals(GetProfileResponse.Stage.NEW_OFFNET, getProfileResponse.getStage());
//        Assert.assertNotNull(profile);
//        Assert.assertEquals(profile.getOperatorId(), OFFNET.getOperatorId());
//        Assert.assertEquals(profile.getAttribute(Profile.ATTR_LOCATION).getValue(), OFFNET.getLocation());
//        Assert.assertNull(providerDao.getProfile(msisdn));
//        Assert.assertNull(memcachedCache.get(msisdn));
//        Assert.assertTrue(locationProvider.wasUsedLocation());

        // cleaning after test
        memcachedCache.delete(imsi);
        memcachedCache.delete(msisdn);
    }

}
