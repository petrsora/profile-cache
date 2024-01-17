package cz.vodafone.profilecache.profileprovider.ejb;

import cz.vodafone.profilecache.builder.impl.PropertiesProfileBuilder;
import cz.vodafone.profilecache.cache.CacheException;
import cz.vodafone.profilecache.cache.MemcachedCache;
import cz.vodafone.profilecache.location.LocationProvider;
import cz.vodafone.profilecache.location.impl.LocationProviderImpl;
import cz.vodafone.profilecache.model.GetProfileResponse;
import cz.vodafone.profilecache.model.ProfileProvider;
import cz.vodafone.profilecache.model.ProfileProviderException;
import cz.vodafone.profilecache.model.impl.ImsiValidatorImpl;
import cz.vodafone.profilecache.model.impl.MsisdnValidatorImpl;
import cz.vodafone.profilecache.model.impl.ProfileProviderImpl;
import cz.vodafone.profilecache.persistence.ProviderDaoImpl;
import cz.vodafone.profilecache.persistence.helper.ConnectionFactory;
import cz.vodafone.profilecache.persistence.helper.DataSourceConnectionFactory;
import cz.vodafone.profilecache.services.configuration.Configuration;
import cz.vodafone.profilecache.services.configuration.ConfigurationItems;
import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.*;
import javax.sql.DataSource;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@DependsOn("InitializerBean")
@Startup
@Stateless
public class ProfileProviderBean {

    private static final Logger LOG = Logger.getLogger(ProfileProviderBean.class);

    // todo Use JTA?? Transaction isolation?? min pool size?? max pool size??
    @Resource(name = "java:jboss/datasources/ProfileCacheDS")
    private DataSource dataSource;

    @EJB
    private StatusHolderBean statusHolderBean;

    private MemcachedCache memcachedCache;
    private ProfileProvider profileProvider;

    @PostConstruct
    public void postConstruct() {
        LOG.info("Method postConstruct() invoked ...");

        try {
            ConnectionFactory connectionFactory = new DataSourceConnectionFactory(dataSource);

            ProviderDaoImpl providerDao = new ProviderDaoImpl(connectionFactory);

            PropertiesProfileBuilder builder = new PropertiesProfileBuilder(System.getProperties());

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

            // fake location provider
/*          LocationProvider locationProvider = new LocationProvider() {
                @Override
                public Location getLocation(String msisdn) throws LocationProviderException {
                    return new LocationImpl("213", "V2");
                }
            }; */

            URL wsdlLocation = new URL(Configuration.getMandatoryString(ConfigurationItems.LOCATION_PROVIDER_WSDL_LOCATION));
            String endpointAddress = Configuration.getMandatoryString(ConfigurationItems.LOCATION_PROVIDER_ENDPOINT_ADDRESS);
            String applicationCode = Configuration.getMandatoryString(ConfigurationItems.LOCATION_PROVIDER_APPLICATION_CODE);
            String userId = Configuration.getMandatoryString(ConfigurationItems.LOCATION_PROVIDER_USER_ID);
            String username = Configuration.getMandatoryString(ConfigurationItems.LOCATION_PROVIDER_USERNAME);
            String password = Configuration.getMandatoryString(ConfigurationItems.LOCATION_PROVIDER_PASSWORD);

            LocationProvider locationProvider = new LocationProviderImpl.Builder().
                    setWsdlLocation(wsdlLocation).
                    setEndpointAddress(endpointAddress).
                    setApplicationCode(applicationCode).
                    setUserId(userId).
                    setUsername(username).
                    setPassword(password).
                    build();

            long locationExpiryPeriod = Configuration.getMandatoryLong(ConfigurationItems.INTERNAL_LOCATION_EXPIRY_PERIOD);

            boolean dbOfflineMode = Configuration.getMandatoryBoolean(ConfigurationItems.INTERNAL_DB_OFFLINE_MODE);

            this.profileProvider = new ProfileProviderImpl.Builder().
                    setCache(memcachedCache).
                    setLocationProvider(locationProvider).
                    setMsisdnValidator(new MsisdnValidatorImpl()).
                    setImsiValidator(new ImsiValidatorImpl()).
                    setProfileBuilder(builder).
                    setProviderDao(providerDao).
                    setLocationExpiryPeriod(locationExpiryPeriod).
                    setDbOfflineMode(dbOfflineMode).
                    setStatus(statusHolderBean.getStatus()).
                    build();
        } catch (CacheException e) {
            LOG.error(String.format("Fatal error while creating ProfileProviderBean - cache problem (%s)", e.getMessage()), e);
            throw new EJBException(e);
        } catch (MalformedURLException e) {
            LOG.error(String.format("Fatal error while creating ProfileProviderBean - WSDL URL problem (%s)", e.getMessage()), e);
            throw new EJBException(e);
        }

    }

    @PreDestroy
    public void preDestroy() {
        LOG.info("Method preDestroy() invoked ...");
        if (memcachedCache != null) {
            memcachedCache.shutdown();
        }
    }

    public GetProfileResponse getProfile(String subscriber) throws ProfileProviderException {
        GetProfileResponse getProfileResponse = profileProvider.getProfile(subscriber);
        return getProfileResponse;
    }

    public List<GetProfileResponse> getProfileList(List<String> subscriberList) throws ProfileProviderException {
        List<GetProfileResponse> getProfileResponses = profileProvider.getProfileList(subscriberList);
        return getProfileResponses;
    }

    public GetProfileResponse getProfileByImsi(String imsi) throws ProfileProviderException {
        GetProfileResponse getProfileResponse = profileProvider.getProfileByImsi(imsi);
        return getProfileResponse;
    }

}