package cz.vodafone.profilecache.initload;

import cz.vodafone.profilecache.cache.CacheException;
import cz.vodafone.profilecache.cache.MemcachedCache;
import cz.vodafone.profilecache.helper.LoggingHelper;
import cz.vodafone.profilecache.location.Location;
import cz.vodafone.profilecache.model.Attribute;
import cz.vodafone.profilecache.model.Profile;
import cz.vodafone.profilecache.model.impl.AttributeImpl;
import cz.vodafone.profilecache.model.impl.ProfileImpl;
import cz.vodafone.profilecache.persistence.helper.ConnectionFactory;
import cz.vodafone.profilecache.persistence.helper.DbObjects;
import cz.vodafone.profilecache.persistence.helper.JdbcTemplate;
import cz.vodafone.profilecache.persistence.helper.RawConnectionFactory;
import cz.vodafone.profilecache.services.configuration.Configuration;
import cz.vodafone.profilecache.services.configuration.ConfigurationItems;
import cz.vodafone.profilecache.services.configuration.Initializer;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.sql.Connection;

public class InitLoad implements Runnable {

    private static final Logger LOG = Logger.getLogger(InitLoad.class);

    private MemcachedCache memcachedCache;

    public static void main(String[] args) {
        LoggingHelper.info("Starting init load application ...", LOG);

        try {
            InitLoad initLoad = new InitLoad(args);
            initLoad.run();
        } catch (CacheException e) {
            LoggingHelper.error(String.format("Error while initializing cache: %s", e.getMessage()), LOG, e);
        } catch (IOException e) {
            LoggingHelper.error(String.format("Error while reading configuration: %s", e.getMessage()), LOG, e);
        }
    }

    public InitLoad(String[] args) throws CacheException, IOException {
        if (args.length > 0 && args[0] != null) {
            Initializer.readConfiguration(args[0]);
        } else {
            Initializer.readConfiguration();
        }

        String servers = Configuration.getMandatoryString(ConfigurationItems.MEMCACHED_SERVERS);
        int expiryPeriod = Configuration.getMandatoryInt(ConfigurationItems.MEMCACHED_EXPIRY_PERIOD);
        int imsiExpiryPeriod = Configuration.getMandatoryInt(ConfigurationItems.MEMCACHED_IMSI_EXPIRY_PERIOD);
        int timeToWaitForResult = Configuration.getMandatoryInt(ConfigurationItems.MEMCACHED_TIME_TO_WAIT_FOR_RESULT);
        memcachedCache = new MemcachedCache.Builder().
                setBinaryProtocol().
                setServers(servers).
                setExpiryPeriod(expiryPeriod * 60).
                setImsiExpiryPeriod(imsiExpiryPeriod * 60).
                setTimeToWaitForResult(timeToWaitForResult).
                build();
    }

    @Override
    public void run() {
        long ts = System.currentTimeMillis();
        long count = 0;

        JdbcTemplate jdbcTemplate = new JdbcTemplate(getConnectionFactory());

        Connection con = null;
        DbObjects dbObjects = null;
        try {
            con = jdbcTemplate.getConnection();

            long tsQuery = System.currentTimeMillis();
            dbObjects = jdbcTemplate.query(con,
                    "select MSISDN.ID, MSISDN.MSISDN, ATTR.ATTRIBUTE_NAME, ATTR.ATTRIBUTE_VALUE, ATTR.EVENT_ID, ATTR.LAST_UPDATE " +
                            "from MSISDN_LIST MSISDN left outer join ATTRIBUTE_LIST ATTR on MSISDN.ID = ATTR.ID " +
                            "order by MSISDN.ID");
            LoggingHelper.info(String.format("SQL query finished (%d ms)", (System.currentTimeMillis() - tsQuery)), LOG);

            MsisdnAttributeMapper msisdnAttributeMapper = new MsisdnAttributeMapper();
            MsisdnAttribute msisdnAttribute;
            Integer lastId = null;
            ProfileImpl.Builder builder = null;
            while ((msisdnAttribute = jdbcTemplate.nextRow(dbObjects, msisdnAttributeMapper)) != null) {
                // new profile
                if (lastId == null || msisdnAttribute.getId() != lastId) {
                    if (builder != null) {
                        processProfile(builder);
                    }
                    builder = prepareNewProfileBuilder(msisdnAttribute);
                    lastId = msisdnAttribute.getId();
                    count++;

                    if (count % 1000 == 0) {
                        LoggingHelper.info(String.format("Init load still working (%d records, %d ms) ...", count, (System.currentTimeMillis() - ts)),
                                LOG);
                    }
                }

                Attribute attr = new AttributeImpl(msisdnAttribute.getName(), msisdnAttribute.getValue(), msisdnAttribute.getLastUpdate());
                builder.setAttribute(attr);
            }

            if (builder != null) {
                processProfile(builder);
            }

        } catch (Exception e) {
            LoggingHelper.fatal(String.format("Fatal error while running init load (%s)", e.getMessage()), LOG, e);
        } finally {
            JdbcTemplate.closeDbObjects(dbObjects);
            JdbcTemplate.closeConnection(con);

            memcachedCache.shutdown();

            LoggingHelper.info(String.format("Init load finished (%d records, %d ms)", count, (System.currentTimeMillis() - ts)), LOG);
        }
    }

    private void processProfile(ProfileImpl.Builder builder) throws CacheException {
        try {
            Profile profile = builder.build();
            if (!memcachedCache.add(profile)) {
                LoggingHelper.info(
                        String.format("Profile already registered in the cache, skipped (%s)", profile.getMsisdn()), LOG);
            }

        } catch (IllegalStateException e) {
            LoggingHelper.error(String.format("Not valid profile found: %s (%d, %s)", e.getMessage(), builder.getId(), builder.getMsisdn()), LOG);
        }
    }

    private ProfileImpl.Builder prepareNewProfileBuilder(MsisdnAttribute msisdnAttribute) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("Preparing new profile for %s", msisdnAttribute.getMsisdn()));
        }
        return new ProfileImpl.Builder().
                setMsisdn(msisdnAttribute.getMsisdn()).
                setOperatorId(Location.OPERATOR_ID_VFCZ). // in DB should be only VFCZ subscriber
                setId(msisdnAttribute.getId());
    }

    private static ConnectionFactory getConnectionFactory() {
        String driver = Configuration.getMandatoryString(ConfigurationItems.DB_DRIVER);
        String url = Configuration.getMandatoryString(ConfigurationItems.DB_URL);
        String username = Configuration.getMandatoryString(ConfigurationItems.DB_USERNAME);
        String password = Configuration.getMandatoryString(ConfigurationItems.DB_PASSWORD);
        return new RawConnectionFactory(driver, url, username, password);
    }

}
