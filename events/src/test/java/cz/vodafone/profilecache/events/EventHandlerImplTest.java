package cz.vodafone.profilecache.events;

import cz.vodafone.profilecache.Utils;
import cz.vodafone.profilecache.builder.ProfileBuilder;
import cz.vodafone.profilecache.builder.impl.PropertiesProfileBuilder;
import cz.vodafone.profilecache.cache.CacheException;
import cz.vodafone.profilecache.cache.MemcachedCache;
import cz.vodafone.profilecache.location.Location;
import cz.vodafone.profilecache.model.Profile;
import cz.vodafone.profilecache.model.impl.AttributeImpl;
import cz.vodafone.profilecache.model.impl.MsisdnValidatorImpl;
import cz.vodafone.profilecache.model.impl.ProfileImpl;
import cz.vodafone.profilecache.persistence.ProviderDao;
import cz.vodafone.profilecache.persistence.ProviderDaoException;
import cz.vodafone.profilecache.persistence.ProviderDaoImpl;
import cz.vodafone.profilecache.persistence.helper.RawConnectionFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.xml.datatype.DatatypeFactory;
import java.io.FileReader;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;

public class EventHandlerImplTest {

    private static final Logger LOG = Logger.getLogger(EventHandlerImplTest.class);

    private static final String MSISDN_PLACE_HOLDER = "MSISDN";
    private static final String EFFECTIVE_DATE_PLACE_HOLDER = "EFFECTIVE_DATE";
    private static final String PRODUCT_CODE_PLACE_HOLDER = "PRODUCT_CODE";

    private static final String MSISDN = "420777350243";
    private static final String NEW_MSISDN = "420777350244";

    private static final String EVENT_NEW_SVC_PRODUCT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "\n" +
            "<ns0:serviceProductInstanceChangeEvent xmlns:ns0=\"http://www.vodafone.cz/CustomerSubscriber/xml/Events\">\n" +
            "    <ns1:header xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Common\">\n" +
            "        <ns1:correlationId>1376046131351</ns1:correlationId>\n" +
            "        <ns1:eventTimeStamp>2013-08-09T13:02:11.352+02:00</ns1:eventTimeStamp>\n" +
            "        <ns1:applicationCode>V4 TIBCO Integration</ns1:applicationCode>\n" +
            "        <ns1:effectiveDate>" + EFFECTIVE_DATE_PLACE_HOLDER + "</ns1:effectiveDate>\n" +
            "    </ns1:header>\n" +
            "    <ns1:customerAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">1007468831</ns1:customerAccountNumber>\n" +
            "    <ns1:billingAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">7468831</ns1:billingAccountNumber>\n" +
            "    <ns1:msisdn xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">" + MSISDN_PLACE_HOLDER + "</ns1:msisdn>\n" +
            "    <ns0:newProductInstance>\n" +
            "        <ns1:auditInfo xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Common\"/>\n" +
            "        <ns1:productOfferingKey xmlns:ns1=\"http://www.vodafone.cz/Common/xml/ServiceProduct\">\n" +
            "            <ns1:productCode>" + PRODUCT_CODE_PLACE_HOLDER + "</ns1:productCode>\n" +
            "            <ns1:productDescription>Adult content barring</ns1:productDescription>\n" +
            "        </ns1:productOfferingKey>\n" +
            "    </ns0:newProductInstance>\n" +
            "</ns0:serviceProductInstanceChangeEvent>";

    private static final String EVENT_OLD_SVC_PRODUCT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "\n" +
            "<ns0:serviceProductInstanceChangeEvent xmlns:ns0=\"http://www.vodafone.cz/CustomerSubscriber/xml/Events\">\n" +
            "    <ns1:header xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Common\">\n" +
            "        <ns1:correlationId>1376046131351</ns1:correlationId>\n" +
            "        <ns1:eventTimeStamp>2013-08-09T13:02:11.352+02:00</ns1:eventTimeStamp>\n" +
            "        <ns1:applicationCode>V4 TIBCO Integration</ns1:applicationCode>\n" +
            "        <ns1:effectiveDate>" + EFFECTIVE_DATE_PLACE_HOLDER + "</ns1:effectiveDate>\n" +
            "    </ns1:header>\n" +
            "    <ns1:customerAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">1007468831</ns1:customerAccountNumber>\n" +
            "    <ns1:billingAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">7468831</ns1:billingAccountNumber>\n" +
            "    <ns1:msisdn xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">" + MSISDN_PLACE_HOLDER + "</ns1:msisdn>\n" +
            "    <ns0:oldProductInstance>\n" +
            "        <ns1:auditInfo xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Common\"/>\n" +
            "        <ns1:productOfferingKey xmlns:ns1=\"http://www.vodafone.cz/Common/xml/ServiceProduct\">\n" +
            "            <ns1:productCode>" + PRODUCT_CODE_PLACE_HOLDER + "</ns1:productCode>\n" +
            "            <ns1:productDescription>Adult content barring</ns1:productDescription>\n" +
            "        </ns1:productOfferingKey>\n" +
            "    </ns0:oldProductInstance>\n" +
            "</ns0:serviceProductInstanceChangeEvent>";

    private ProviderDao providerDao;
    private MemcachedCache memcachedCache;
    private EventHandler eventHandler;

    private String oldEffectiveDate;
    private String newEffectiveDate;

    @Before
    public void before() throws Exception {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.INFO);

        GregorianCalendar gCal = (GregorianCalendar) GregorianCalendar.getInstance();
        gCal.add(GregorianCalendar.MINUTE, -2);
        oldEffectiveDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(gCal).toXMLFormat();
        gCal.add(GregorianCalendar.MINUTE, 4);
        newEffectiveDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(gCal).toXMLFormat();

        providerDao = new ProviderDaoImpl(getLocalRawConnectionFactory());

        FileReader fr = new FileReader("/home/milan/work/vfcz/svn/profile-cache/trunk/events/src/test/resources/profiles.properties");
//        FileReader fr = new FileReader("src/test/resources/profiles.properties");
        Properties props = new Properties();
        props.load(fr);
        ProfileBuilder builder = new PropertiesProfileBuilder(props);

        memcachedCache = new MemcachedCache.Builder().
                setBinaryProtocol().
                setServers(getLocalServers()).
                build();

        eventHandler = new EventHandlerImpl(new MsisdnValidatorImpl(), providerDao, builder, memcachedCache);
    }

    @After
    public void after() {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        memcachedCache.shutdown();
    }

    @Test
    public void testSubscriberActivationEvent() throws Exception {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        LOG.info("cleaning before the unit test");
        deleteProfile(MSISDN);

        String event = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<ns0:subscriberActivationEvent xmlns:ns0=\"http://www.vodafone.cz/CustomerSubscriber/xml/Events\">\n" +
                "    <ns1:header xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Common\">\n" +
                "        <ns1:correlationId>1350292116400</ns1:correlationId>\n" +
                "        <ns1:eventTimeStamp>2012-10-15T11:08:36.4+02:00</ns1:eventTimeStamp>\n" +
                "        <ns1:applicationCode>V4 Tibco Integration</ns1:applicationCode>\n" +
                "        <ns1:effectiveDate>2012-10-15T11:08:36.4+02:00</ns1:effectiveDate>\n" +
                "    </ns1:header>\n" +
                "    <ns1:customerAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">1028008181</ns1:customerAccountNumber>\n" +
                "    <ns1:billingAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">31944878</ns1:billingAccountNumber>\n" +
                "    <ns1:msisdn xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">" + MSISDN + "</ns1:msisdn>\n" +
                "    <ns0:reasonCode>PORDTEDIN</ns0:reasonCode>\n" +
                "    <ns0:subscriberType>POSTPAID</ns0:subscriberType>\n" +
                "    <ns0:volteEnabled>true</ns0:volteEnabled>\n" +
                "</ns0:subscriberActivationEvent>";

        LOG.info("Test case: subscriber does not exist");
        eventHandler.handle(event, "V2");
        Profile profile = memcachedCache.get(MSISDN);
        Assert.assertNotNull(profile);
        Assert.assertEquals("V2", profile.getAttribute(Profile.ATTR_LOCATION).getValue());

        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        Assert.assertEquals("V2", profile.getAttribute(Profile.ATTR_LOCATION).getValue());
        Assert.assertNotNull(profile.getAttribute(Profile.ATTR_LOCATION).getLastUpdate());
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_IS_PREPAID).getValue());
        Assert.assertNotNull(profile.getAttribute(Profile.ATTR_IS_PREPAID).getLastUpdate());
        Assert.assertEquals(Profile.ATTR_VALUE_TRUE, profile.getAttribute(Profile.ATTR_IS_VOLTE).getValue());
        Assert.assertNotNull(profile.getAttribute(Profile.ATTR_IS_VOLTE).getLastUpdate());

        LOG.info("Test case: subscriber exists");
        eventHandler.handle(event, "V4");
        profile = memcachedCache.get(MSISDN);
        Assert.assertNotNull(profile);
        Assert.assertEquals("V4", profile.getAttribute(Profile.ATTR_LOCATION).getValue());

        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        Assert.assertEquals("V4", profile.getAttribute(Profile.ATTR_LOCATION).getValue());
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_IS_PREPAID).getValue());
        Assert.assertEquals(Profile.ATTR_VALUE_TRUE, profile.getAttribute(Profile.ATTR_IS_VOLTE).getValue());
    }

    @Test
    public void testSubscriberDeactivationEvent() throws Exception {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        LOG.info("cleaning before the unit test");
        deleteProfile(MSISDN);

        LOG.info("preparing before the unit test");
        Profile profile = new ProfileImpl.Builder().
                setMsisdn(MSISDN).
                setOperatorId(Location.OPERATOR_ID_VFCZ).
                setAttribute(new AttributeImpl(Profile.ATTR_LOCATION, "V2", new Date())).
                build();
        providerDao.insertProfile(profile);
        memcachedCache.set(profile);

        String event = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "\n" +
                "<ns0:subscriberDeactivationEvent xmlns:ns0=\"http://www.vodafone.cz/CustomerSubscriber/xml/Events\">\n" +
                "    <ns1:header xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Common\">\n" +
                "        <ns1:correlationId>1375866406703</ns1:correlationId>\n" +
                "        <ns1:eventTimeStamp>2013-08-07T11:06:46.703+02:00</ns1:eventTimeStamp>\n" +
                "        <ns1:applicationCode>V4 Tibco Integration</ns1:applicationCode>\n" +
                "        <ns1:effectiveDate>2013-08-07T11:06:46.703+02:00</ns1:effectiveDate>\n" +
                "    </ns1:header>\n" +
                "    <ns1:customerAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">1003179476</ns1:customerAccountNumber>\n" +
                "    <ns1:billingAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">3179476</ns1:billingAccountNumber>\n" +
                "    <ns1:msisdn xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">" + MSISDN + "</ns1:msisdn>\n" +
                "    <ns0:reasonCode>STANDARD</ns0:reasonCode>\n" +
                "</ns0:subscriberDeactivationEvent>";

        LOG.info("if subscriber exists");
        eventHandler.handle(event, "V2");
        Assert.assertNull(providerDao.getProfile(MSISDN));
        Assert.assertNull(memcachedCache.get(MSISDN));

        LOG.info("if subscriber does not exist");
        eventHandler.handle(event, "V2");
        Assert.assertNull(providerDao.getProfile(MSISDN));
        Assert.assertNull(memcachedCache.get(MSISDN));
    }

    @Test
    public void testPre2PostMigrationEvent() throws Exception {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        LOG.info("cleaning before the unit test");
        deleteProfile(MSISDN);

        LOG.info("preparing before the unit test");
        Profile profile = new ProfileImpl.Builder().
                setMsisdn(MSISDN).
                setOperatorId(Location.OPERATOR_ID_VFCZ).
                setAttribute(new AttributeImpl(Profile.ATTR_LOCATION, "V2", new Date())).
                setAttribute(new AttributeImpl(Profile.ATTR_IS_PREPAID, Profile.ATTR_VALUE_FALSE, new Date())).
                build();
        providerDao.insertProfile(profile);
        memcachedCache.set(profile);

        String EVENT_POST2PRE_MIGRATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "\n" +
                "<ns0:subscriberPost2PreMigrationEvent xmlns:ns0=\"http://www.vodafone.cz/CustomerSubscriber/xml/Events\">\n" +
                "    <ns1:header xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Common\">\n" +
                "        <ns1:correlationId>1036393841_2013-08-07T11:09:38.188+02:00</ns1:correlationId>\n" +
                "        <ns1:eventTimeStamp>2013-08-07T11:09:38.188+02:00</ns1:eventTimeStamp>\n" +
                "        <ns1:applicationCode>V4 TIBCO Integration</ns1:applicationCode>\n" +
                "        <ns1:effectiveDate>" + EFFECTIVE_DATE_PLACE_HOLDER + "</ns1:effectiveDate>\n" +
                "    </ns1:header>\n" +
                "    <ns1:customerAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">1036393841</ns1:customerAccountNumber>\n" +
                "    <ns1:billingAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">32807229</ns1:billingAccountNumber>\n" +
                "    <ns1:msisdn xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">" + MSISDN_PLACE_HOLDER + "</ns1:msisdn>\n" +
                "</ns0:subscriberPost2PreMigrationEvent>";

        LOG.info("if event is older then last update");
        String event = EVENT_POST2PRE_MIGRATION.
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, oldEffectiveDate);

        eventHandler.handle(event, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        // not migrated to prepaid because of older event
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_IS_PREPAID).getValue());

        LOG.info("if subscriber exist");
        event = EVENT_POST2PRE_MIGRATION.
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);

        eventHandler.handle(event, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        // migrated to prepaid
        Assert.assertEquals(Profile.ATTR_VALUE_TRUE, profile.getAttribute(Profile.ATTR_IS_PREPAID).getValue());

        LOG.info("if subscriber does not exist");
        providerDao.deleteProfile(profile);
        eventHandler.handle(event, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        // migrated to prepaid
        Assert.assertEquals(Profile.ATTR_VALUE_TRUE, profile.getAttribute(Profile.ATTR_IS_PREPAID).getValue());
    }

    @Test
    public void testPost2PreMigrationEvent() throws Exception {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        LOG.info("cleaning before the unit test");
        deleteProfile(MSISDN);

        LOG.info("preparing before the unit test");
        Profile profile = new ProfileImpl.Builder().
                setMsisdn(MSISDN).
                setOperatorId(Location.OPERATOR_ID_VFCZ).
                setAttribute(new AttributeImpl(Profile.ATTR_LOCATION, "V2", new Date())).
                setAttribute(new AttributeImpl(Profile.ATTR_IS_PREPAID, Profile.ATTR_VALUE_TRUE, new Date())).
                build();
        providerDao.insertProfile(profile);
        memcachedCache.set(profile);

        String EVENT_PRE2POST_MIGRATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "\n" +
                "<ns0:subscriberPre2PostMigrationEvent xmlns:ns0=\"http://www.vodafone.cz/CustomerSubscriber/xml/Events\">\n" +
                "    <ns1:header xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Common\">\n" +
                "        <ns1:correlationId>1003697606_2013-08-07T11:01:16.34+02:00</ns1:correlationId>\n" +
                "        <ns1:eventTimeStamp>2013-08-07T11:01:16.34+02:00</ns1:eventTimeStamp>\n" +
                "        <ns1:applicationCode>V4 TIBCO Integration</ns1:applicationCode>\n" +
                "        <ns1:effectiveDate>" + EFFECTIVE_DATE_PLACE_HOLDER + "</ns1:effectiveDate>\n" +
                "    </ns1:header>\n" +
                "    <ns1:customerAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">1003697606</ns1:customerAccountNumber>\n" +
                "    <ns1:billingAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">32807051</ns1:billingAccountNumber>\n" +
                "    <ns1:msisdn xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">" + MSISDN_PLACE_HOLDER + "</ns1:msisdn>\n" +
                "</ns0:subscriberPre2PostMigrationEvent>";

        LOG.info("if event is older then last update");
        String event = EVENT_PRE2POST_MIGRATION.
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, oldEffectiveDate);

        eventHandler.handle(event, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        // not migrated to postpaid because of older event
        Assert.assertEquals(Profile.ATTR_VALUE_TRUE, profile.getAttribute(Profile.ATTR_IS_PREPAID).getValue());

        LOG.info("if subscriber exist");
        event = EVENT_PRE2POST_MIGRATION.
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);

        eventHandler.handle(event, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        // migrated to postpaid
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_IS_PREPAID).getValue());

        LOG.info("if subscriber does not exist");
        providerDao.deleteProfile(profile);
        eventHandler.handle(event, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        // migrated to postpaid
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_IS_PREPAID).getValue());
    }

    @Test
    public void testMsisdnSwap() throws Exception {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        LOG.info(String.format("Testing MSISDN swap (new=%s, old=%s)", NEW_MSISDN, MSISDN));
        LOG.info("Cleaning before the unit test");
        deleteProfile(MSISDN);
        deleteProfile(NEW_MSISDN);

        String event = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "\n" +
                "<ns0:subscriberMsisdnSwapEvent xmlns:ns0=\"http://www.vodafone.cz/CustomerSubscriber/xml/Events\">\n" +
                "    <ns1:header xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Common\">\n" +
                "        <ns1:correlationId>1375866940042</ns1:correlationId>\n" +
                "        <ns1:eventTimeStamp>2013-08-07T11:15:40.042+02:00</ns1:eventTimeStamp>\n" +
                "        <ns1:applicationCode>V4 TIBCO Integration</ns1:applicationCode>\n" +
                "        <ns1:effectiveDate>2013-08-07T11:15:40.042+02:00</ns1:effectiveDate>\n" +
                "    </ns1:header>\n" +
                "    <ns1:customerAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">1002330374</ns1:customerAccountNumber>\n" +
                "    <ns1:billingAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">2330374</ns1:billingAccountNumber>\n" +
                "    <ns1:msisdn xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">" + MSISDN + "</ns1:msisdn>\n" +
                "    <ns0:newMsisdn>" + NEW_MSISDN + "</ns0:newMsisdn>\n" +
                "</ns0:subscriberMsisdnSwapEvent>";

        LOG.info("Test case: old profile does not exist, new does not exist");
        eventHandler.handle(event, "V2");
        Assert.assertNull(providerDao.getProfile(MSISDN));
        Assert.assertNotNull(providerDao.getProfile(NEW_MSISDN));
        Assert.assertNull(memcachedCache.get(MSISDN));
        Assert.assertNotNull(memcachedCache.get(NEW_MSISDN));
        Assert.assertEquals("V2", providerDao.getProfile(NEW_MSISDN).getAttribute(Profile.ATTR_LOCATION).getValue());
        Assert.assertEquals("V2", memcachedCache.get(NEW_MSISDN).getAttribute(Profile.ATTR_LOCATION).getValue());

        LOG.info("Test case: old profile exists, new profile does not exist");
        providerDao.deleteProfile(providerDao.getProfile(NEW_MSISDN));
        memcachedCache.delete(NEW_MSISDN);
        Profile profile = new ProfileImpl.Builder().
                setMsisdn(MSISDN).
                setOperatorId(Location.OPERATOR_ID_VFCZ).
                setAttribute(new AttributeImpl(Profile.ATTR_LOCATION, "V2", new Date())).
                setAttribute(new AttributeImpl(Profile.ATTR_IS_PREPAID, Profile.ATTR_VALUE_TRUE, new Date())).
                build();
        providerDao.insertProfile(profile);
        memcachedCache.set(profile);

        eventHandler.handle(event, "V4");
        Assert.assertNull(providerDao.getProfile(MSISDN));
        Assert.assertNotNull(providerDao.getProfile(NEW_MSISDN));
        Assert.assertNull(memcachedCache.get(MSISDN));
        Assert.assertNotNull(memcachedCache.get(NEW_MSISDN));
        Assert.assertEquals("V4", providerDao.getProfile(NEW_MSISDN).getAttribute(Profile.ATTR_LOCATION).getValue());
        Assert.assertEquals("V4", memcachedCache.get(NEW_MSISDN).getAttribute(Profile.ATTR_LOCATION).getValue());

        LOG.info("Test case: new profile exists, old profile does not exist");
        eventHandler.handle(event, "V2");
        Assert.assertNull(providerDao.getProfile(MSISDN));
        Assert.assertNotNull(providerDao.getProfile(NEW_MSISDN));
        Assert.assertNull(memcachedCache.get(MSISDN));
        Assert.assertNotNull(memcachedCache.get(NEW_MSISDN));
        Assert.assertEquals("V2", providerDao.getProfile(NEW_MSISDN).getAttribute(Profile.ATTR_LOCATION).getValue());
        Assert.assertEquals("V2", memcachedCache.get(NEW_MSISDN).getAttribute(Profile.ATTR_LOCATION).getValue());
    }

    @Test
    public void testCommunicationProfileChangeEvent() throws Exception {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        String oldCommProfile = "DEFAULT";
        String newCommProfile1 = "MO_FRI_6_22";
        String newCommProfile2 = "NOLIMIT";

        LOG.info("cleaning before the unit test");
        deleteProfile(MSISDN);

        LOG.info("preparing before the unit test");
        Profile profile = new ProfileImpl.Builder().
                setMsisdn(MSISDN).
                setOperatorId(Location.OPERATOR_ID_VFCZ).
                setAttribute(new AttributeImpl(Profile.ATTR_LOCATION, "V2", new Date())).
                setAttribute(new AttributeImpl(Profile.ATTR_SCHEDULING_PROFILE, oldCommProfile, new Date())).
                build();
        providerDao.insertProfile(profile);
        memcachedCache.set(profile);

        String COMM_PROFILE_PLACE_HOLDER = "COMM_PROFILE";
        String COMM_PROFILE_EVENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "\n" +
                "<ns0:communicationProfileChangeEvent xmlns:ns0=\"http://www.vodafone.cz/CustomerSubscriber/xml/Events\">\n" +
                "    <ns1:header xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Common\">\n" +
                "        <ns1:correlationId>1375866197018</ns1:correlationId>\n" +
                "        <ns1:eventTimeStamp>2013-08-07T11:03:17.018+02:00</ns1:eventTimeStamp>\n" +
                "        <ns1:applicationCode>V4 TIBCO Integration</ns1:applicationCode>\n" +
                "        <ns1:effectiveDate>" + EFFECTIVE_DATE_PLACE_HOLDER + "</ns1:effectiveDate>\n" +
                "    </ns1:header>\n" +
                "    <ns1:customerAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">1002337160</ns1:customerAccountNumber>\n" +
                "    <ns1:billingAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">2337160</ns1:billingAccountNumber>\n" +
                "    <ns1:msisdn xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">" + MSISDN_PLACE_HOLDER + "</ns1:msisdn>\n" +
                "    <ns0:communicationProfile>" + COMM_PROFILE_PLACE_HOLDER + "</ns0:communicationProfile>\n" +
                "</ns0:communicationProfileChangeEvent>";

        LOG.info("if event is older then last update");
        String event = COMM_PROFILE_EVENT.
                replaceAll(COMM_PROFILE_PLACE_HOLDER, newCommProfile1).
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, oldEffectiveDate);

        eventHandler.handle(event, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        // not changed because of older event
        Assert.assertEquals(oldCommProfile, profile.getAttribute(Profile.ATTR_SCHEDULING_PROFILE).getValue());

        LOG.info("if subscriber exist");
        event = COMM_PROFILE_EVENT.
                replaceAll(COMM_PROFILE_PLACE_HOLDER, newCommProfile1).
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);

        eventHandler.handle(event, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        // changed
        Assert.assertEquals(newCommProfile1, profile.getAttribute(Profile.ATTR_SCHEDULING_PROFILE).getValue());

        LOG.info("if subscriber does not exist");
        providerDao.deleteProfile(profile);
        event = COMM_PROFILE_EVENT.
                replaceAll(COMM_PROFILE_PLACE_HOLDER, newCommProfile2).
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);
        eventHandler.handle(event, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        // changed
        Assert.assertEquals(newCommProfile2, profile.getAttribute(Profile.ATTR_SCHEDULING_PROFILE).getValue());
    }

    @Test
    public void testServiceProductChangeMpenebar() throws Exception {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        LOG.info("cleaning before the unit test");
        deleteProfile(MSISDN);

        LOG.info("preparing before the unit test");
        Profile profile = new ProfileImpl.Builder().
                setMsisdn(MSISDN).
                setOperatorId(Location.OPERATOR_ID_VFCZ).
                setAttribute(new AttributeImpl(Profile.ATTR_LOCATION, "V2", new Date())).
                build();
        providerDao.insertProfile(profile);
        memcachedCache.set(profile);
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNull(profile.getAttribute(Profile.ATTR_HAS_MPENEZENKA_BARRING));

        LOG.info("setting attribute to true");
        String eventNewMpenebar = EVENT_NEW_SVC_PRODUCT.
                replaceAll(PRODUCT_CODE_PLACE_HOLDER, "MPENEBAR").
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);
        eventHandler.handle(eventNewMpenebar, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        // set
        Assert.assertEquals(Profile.ATTR_VALUE_TRUE, profile.getAttribute(Profile.ATTR_HAS_MPENEZENKA_BARRING).getValue());

        LOG.info("setting attribute to false");
        String eventOldMpenebar = EVENT_OLD_SVC_PRODUCT.
                replaceAll(PRODUCT_CODE_PLACE_HOLDER, "MPENEBAR").
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);
        eventHandler.handle(eventOldMpenebar, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        // set
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_MPENEZENKA_BARRING).getValue());
    }

    @Test
    public void testServiceProductChangeBarprsms() throws Exception {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        LOG.info("cleaning before the unit test");
        deleteProfile(MSISDN);

        LOG.info("preparing before the unit test");
        Profile profile = new ProfileImpl.Builder().
                setMsisdn(MSISDN).
                setOperatorId(Location.OPERATOR_ID_VFCZ).
                setAttribute(new AttributeImpl(Profile.ATTR_LOCATION, "V2", new Date())).
                build();
        providerDao.insertProfile(profile);
        memcachedCache.set(profile);
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNull(profile.getAttribute(Profile.ATTR_HAS_PRSMS_BARRING));

        LOG.info("setting attribute to true");
        String eventNewBarprsms = EVENT_NEW_SVC_PRODUCT.
                replaceAll(PRODUCT_CODE_PLACE_HOLDER, "BARPRSMS").
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);
        eventHandler.handle(eventNewBarprsms, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        // set
        Assert.assertEquals(Profile.ATTR_VALUE_TRUE, profile.getAttribute(Profile.ATTR_HAS_PRSMS_BARRING).getValue());

        LOG.info("setting attribute to false");
        String eventOldBarprsms = EVENT_OLD_SVC_PRODUCT.
                replaceAll(PRODUCT_CODE_PLACE_HOLDER, "BARPRSMS").
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);
        eventHandler.handle(eventOldBarprsms, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        // set
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_PRSMS_BARRING).getValue());
    }

    @Test
    public void testServiceProductChangeAdultbar() throws Exception {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        LOG.info("cleaning before the unit test");
        deleteProfile(MSISDN);

        LOG.info("preparing before the unit test");
        Profile profile = new ProfileImpl.Builder().
                setMsisdn(MSISDN).
                setOperatorId(Location.OPERATOR_ID_VFCZ).
                setAttribute(new AttributeImpl(Profile.ATTR_LOCATION, "V2", new Date())).
                build();
        providerDao.insertProfile(profile);
        memcachedCache.set(profile);
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNull(profile.getAttribute(Profile.ATTR_IS_CHILD));

        LOG.info("setting attribute to true");
        String eventNewAdultbar = EVENT_NEW_SVC_PRODUCT.
                replaceAll(PRODUCT_CODE_PLACE_HOLDER, "ADULTBAR").
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);
        eventHandler.handle(eventNewAdultbar, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        // set
        Assert.assertEquals(Profile.ATTR_VALUE_TRUE, profile.getAttribute(Profile.ATTR_IS_CHILD).getValue());

        LOG.info("setting attribute to false");
        String eventOldAdultbar = EVENT_OLD_SVC_PRODUCT.
                replaceAll(PRODUCT_CODE_PLACE_HOLDER, "ADULTBAR").
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);
        eventHandler.handle(eventOldAdultbar, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        // set
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_IS_CHILD).getValue());
    }

    @Test
    public void testRestrictionChange() throws Exception {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        LOG.info("cleaning before the unit test");
        deleteProfile(MSISDN);

        LOG.info("preparing before the unit test");
        Profile profile = new ProfileImpl.Builder().
                setMsisdn(MSISDN).
                setOperatorId(Location.OPERATOR_ID_VFCZ).
                setAttribute(new AttributeImpl(Profile.ATTR_LOCATION, "V2", new Date())).
                build();
        providerDao.insertProfile(profile);
        memcachedCache.set(profile);
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        Assert.assertNull(profile.getAttribute(Profile.ATTR_IS_RESTRICTED));
        Assert.assertNull(profile.getAttribute(Profile.ATTR_HAS_PRSMS_BARRING));
        Assert.assertNull(profile.getAttribute(Profile.ATTR_HAS_MPENEZENKA_BARRING));

        String RESTRICTION_STATUS_PLACE_HOLDER = "RESTRICTION_STATUS";
        String RESTRICTION_CODE_PLACE_HOLDER = "RESTRICTION_CODE";

        String restrictionEvent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "\n" +
                "<ns0:restrictionInstanceChangeEvent xmlns:ns0=\"http://www.vodafone.cz/CustomerSubscriber/xml/Events\">\n" +
                "    <ns1:header xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Common\">\n" +
                "        <ns1:correlationId>1375866024970</ns1:correlationId>\n" +
                "        <ns1:eventTimeStamp>2013-08-07T11:00:24.97+02:00</ns1:eventTimeStamp>\n" +
                "        <ns1:applicationCode>V4 TIBCO Integration</ns1:applicationCode>\n" +
                "        <ns1:effectiveDate>" + EFFECTIVE_DATE_PLACE_HOLDER + "</ns1:effectiveDate>\n" +
                "    </ns1:header>\n" +
                "    <ns1:customerAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">1001315171</ns1:customerAccountNumber>\n" +
                "    <ns1:msisdn xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">" + MSISDN_PLACE_HOLDER + "</ns1:msisdn>\n" +
                "    <ns0:restrictionCode>" + RESTRICTION_CODE_PLACE_HOLDER + "</ns0:restrictionCode>\n" +
                "    <ns0:restrictionStatus>" + RESTRICTION_STATUS_PLACE_HOLDER + "</ns0:restrictionStatus>\n" +
                "</ns0:restrictionInstanceChangeEvent>";

        LOG.info("PR_SMS activation");
        String prsmsActivation = restrictionEvent.
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(RESTRICTION_CODE_PLACE_HOLDER, "PR_SMS").
                replaceAll(RESTRICTION_STATUS_PLACE_HOLDER, "ACTIVE").
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);
        eventHandler.handle(prsmsActivation, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        Assert.assertEquals(Profile.ATTR_VALUE_TRUE, profile.getAttribute(Profile.ATTR_HAS_PRSMS_BARRING).getValue());
        Assert.assertNull(profile.getAttribute(Profile.ATTR_HAS_MPENEZENKA_BARRING));
        Assert.assertNull(profile.getAttribute(Profile.ATTR_IS_RESTRICTED));

        LOG.info("MPENEBAR activation");
        String mpenebarActivation = restrictionEvent.
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(RESTRICTION_CODE_PLACE_HOLDER, "MPENEBAR").
                replaceAll(RESTRICTION_STATUS_PLACE_HOLDER, "ACTIVE").
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);
        eventHandler.handle(mpenebarActivation, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        Assert.assertEquals(Profile.ATTR_VALUE_TRUE, profile.getAttribute(Profile.ATTR_HAS_PRSMS_BARRING).getValue());
        Assert.assertEquals(Profile.ATTR_VALUE_TRUE, profile.getAttribute(Profile.ATTR_HAS_MPENEZENKA_BARRING).getValue());
        Assert.assertNull(profile.getAttribute(Profile.ATTR_IS_RESTRICTED));

        LOG.info("PR_SMS deactivation");
        String prsmsDeactivation = restrictionEvent.
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(RESTRICTION_CODE_PLACE_HOLDER, "PR_SMS").
                replaceAll(RESTRICTION_STATUS_PLACE_HOLDER, "INACTIVE").
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);
        eventHandler.handle(prsmsDeactivation, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_PRSMS_BARRING).getValue());
        Assert.assertEquals(Profile.ATTR_VALUE_TRUE, profile.getAttribute(Profile.ATTR_HAS_MPENEZENKA_BARRING).getValue());
        Assert.assertNull(profile.getAttribute(Profile.ATTR_IS_RESTRICTED));

        LOG.info("MPENEBAR deactivation");
        String mpenebarDeactivation = restrictionEvent.
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(RESTRICTION_CODE_PLACE_HOLDER, "MPENEBAR").
                replaceAll(RESTRICTION_STATUS_PLACE_HOLDER, "INACTIVE").
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);
        eventHandler.handle(mpenebarDeactivation, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_PRSMS_BARRING).getValue());
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_MPENEZENKA_BARRING).getValue());
        Assert.assertNull(profile.getAttribute(Profile.ATTR_IS_RESTRICTED));

        LOG.info("STOLEN_LOST activation");
        String stolenLostActivation = restrictionEvent.
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(RESTRICTION_CODE_PLACE_HOLDER, "STOLEN_LOST").
                replaceAll(RESTRICTION_STATUS_PLACE_HOLDER, "ACTIVE").
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);
        eventHandler.handle(stolenLostActivation, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_PRSMS_BARRING).getValue());
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_MPENEZENKA_BARRING).getValue());
        Assert.assertEquals("256", profile.getAttribute(Profile.ATTR_IS_RESTRICTED).getValue());

        LOG.info("STOLEN_LOST deactivation");
        String stolenLostDeactivation = restrictionEvent.
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(RESTRICTION_CODE_PLACE_HOLDER, "STOLEN_LOST").
                replaceAll(RESTRICTION_STATUS_PLACE_HOLDER, "INACTIVE").
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);
        eventHandler.handle(stolenLostDeactivation, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_PRSMS_BARRING).getValue());
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_MPENEZENKA_BARRING).getValue());
        Assert.assertEquals("0", profile.getAttribute(Profile.ATTR_IS_RESTRICTED).getValue());

        LOG.info("Restriction 2 activation");
        String activation2 = restrictionEvent.
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(RESTRICTION_CODE_PLACE_HOLDER, "2").
                replaceAll(RESTRICTION_STATUS_PLACE_HOLDER, "ACTIVE").
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);
        eventHandler.handle(activation2, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_PRSMS_BARRING).getValue());
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_MPENEZENKA_BARRING).getValue());
        Assert.assertEquals("2", profile.getAttribute(Profile.ATTR_IS_RESTRICTED).getValue());

        LOG.info("Restriction 9 activation");
        String activation9 = restrictionEvent.
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(RESTRICTION_CODE_PLACE_HOLDER, "9").
                replaceAll(RESTRICTION_STATUS_PLACE_HOLDER, "ACTIVE").
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);
        eventHandler.handle(activation9, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_PRSMS_BARRING).getValue());
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_MPENEZENKA_BARRING).getValue());
        Assert.assertEquals("258", profile.getAttribute(Profile.ATTR_IS_RESTRICTED).getValue()); // 2 + 256

        LOG.info("Restriction 10 activation");
        String activation10 = restrictionEvent.
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(RESTRICTION_CODE_PLACE_HOLDER, "10").
                replaceAll(RESTRICTION_STATUS_PLACE_HOLDER, "ACTIVE").
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);
        eventHandler.handle(activation10, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_PRSMS_BARRING).getValue());
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_MPENEZENKA_BARRING).getValue());
        Assert.assertEquals("770", profile.getAttribute(Profile.ATTR_IS_RESTRICTED).getValue()); // 2 + 256 + 512

        LOG.info("Restriction 10 deactivation");
        activation10 = restrictionEvent.
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(RESTRICTION_CODE_PLACE_HOLDER, "10").
                replaceAll(RESTRICTION_STATUS_PLACE_HOLDER, "INACTIVE").
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);
        eventHandler.handle(activation10, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_PRSMS_BARRING).getValue());
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_MPENEZENKA_BARRING).getValue());
        Assert.assertEquals("258", profile.getAttribute(Profile.ATTR_IS_RESTRICTED).getValue()); // 2 + 256 + 512

        LOG.info("STOLEN_LOST deactivation");
        eventHandler.handle(stolenLostDeactivation, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_PRSMS_BARRING).getValue());
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_MPENEZENKA_BARRING).getValue());
        Assert.assertEquals("2", profile.getAttribute(Profile.ATTR_IS_RESTRICTED).getValue());

        LOG.info("Activation 0 drops all restrictions");
        String activation0 = restrictionEvent.
                replaceAll(MSISDN_PLACE_HOLDER, MSISDN).
                replaceAll(RESTRICTION_CODE_PLACE_HOLDER, "0").
                replaceAll(RESTRICTION_STATUS_PLACE_HOLDER, "ACTIVE").
                replaceAll(EFFECTIVE_DATE_PLACE_HOLDER, newEffectiveDate);
        eventHandler.handle(activation0, "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_PRSMS_BARRING).getValue());
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_HAS_MPENEZENKA_BARRING).getValue());
        Assert.assertEquals("0", profile.getAttribute(Profile.ATTR_IS_RESTRICTED).getValue());
    }

    @Test
    public void testSubscriberChangeEvent() throws Exception {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        LOG.info("cleaning before the unit test");
        deleteProfile(MSISDN);

        LOG.info("preparing before the unit test");
        Profile profile = new ProfileImpl.Builder().
                setMsisdn(MSISDN).
                setOperatorId(Location.OPERATOR_ID_VFCZ).
                setAttribute(new AttributeImpl(Profile.ATTR_LOCATION, "V2", new Date())).
                build();
        providerDao.insertProfile(profile);
        memcachedCache.set(profile);
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        Assert.assertNull(profile.getAttribute(Profile.ATTR_IS_VOLTE));

        String event = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<ns0:subscriberChangeEvent xmlns:ns0=\"http://www.vodafone.cz/CustomerSubscriber/xml/Events\">\n" +
                "    <ns1:header xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Common\">\n" +
                "        <ns1:correlationId>1350292116400</ns1:correlationId>\n" +
                "        <ns1:eventTimeStamp>2012-10-15T11:08:36.4+02:00</ns1:eventTimeStamp>\n" +
                "        <ns1:applicationCode>V4 Tibco Integration</ns1:applicationCode>\n" +
                "        <ns1:effectiveDate>${EFFECTIVE_DATE}T11:08:36.4+02:00</ns1:effectiveDate>\n" +
                "    </ns1:header>\n" +
                "    <ns1:customerAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">1028008181</ns1:customerAccountNumber>\n" +
                "    <ns1:billingAccountNumber xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">31944878</ns1:billingAccountNumber>\n" +
                "    <ns1:msisdn xmlns:ns1=\"http://www.vodafone.cz/Common/xml/Customer\">" + MSISDN + "</ns1:msisdn>\n" +
                "    <ns0:volteEnabled>${VOLTE}</ns0:volteEnabled>\n" +
                "</ns0:subscriberChangeEvent>";

        LOG.info("enabling volte");
        eventHandler.handle(event.
                        replace("${VOLTE}", "true").
                        replace("${EFFECTIVE_DATE}", "2012-10-15"), // date in the past
                "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        Assert.assertEquals(Profile.ATTR_VALUE_TRUE, profile.getAttribute(Profile.ATTR_IS_VOLTE).getValue());
        Assert.assertNotNull(profile.getAttribute(Profile.ATTR_IS_VOLTE).getLastUpdate());

        LOG.info("disabling volte");
        eventHandler.handle(event.
                        replace("${VOLTE}", "false").
                        replace("${EFFECTIVE_DATE}", (Calendar.getInstance().get(Calendar.YEAR) + 1) + "-10-15"), // date in the future (current year + 1)
                "V2");
        Assert.assertNull(memcachedCache.get(MSISDN));
        profile = providerDao.getProfile(MSISDN);
        Assert.assertNotNull(profile);
        Assert.assertEquals(Profile.ATTR_VALUE_FALSE, profile.getAttribute(Profile.ATTR_IS_VOLTE).getValue());
        Assert.assertNotNull(profile.getAttribute(Profile.ATTR_IS_VOLTE).getLastUpdate());
    }

    private static String getLocalServers() {
        return "localhost:11211 localhost:11212";
    }

    private static RawConnectionFactory getLocalRawConnectionFactory() {
        return new RawConnectionFactory(
                "oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:@localhost:1521:xe", "profile_own", "profile_own123");
    }

    private void deleteProfile(String msisdn) throws ProviderDaoException, CacheException {
        Profile profile = new ProfileImpl.Builder().
                setMsisdn(msisdn).
                setOperatorId(Location.OPERATOR_ID_VFCZ).
                setAttribute(new AttributeImpl(Profile.ATTR_LOCATION, "V2")).
                build();
        providerDao.deleteProfile(profile);
        memcachedCache.delete(MSISDN);
    }


}
