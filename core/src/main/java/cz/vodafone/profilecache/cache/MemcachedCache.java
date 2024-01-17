package cz.vodafone.profilecache.cache;

import cz.vodafone.profilecache.cache.impl.BuilderKeyValueSerializer;
import cz.vodafone.profilecache.cache.impl.StateKeyValueParser;
import cz.vodafone.profilecache.location.Location;
import cz.vodafone.profilecache.model.Attribute;
import cz.vodafone.profilecache.model.Profile;
import cz.vodafone.profilecache.model.impl.AttributeImpl;
import cz.vodafone.profilecache.model.impl.ProfileImpl;
import net.spy.memcached.*;
import net.spy.memcached.internal.GetFuture;
import net.spy.memcached.internal.OperationFuture;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MemcachedCache implements Cache {

    private static final Logger LOG = Logger.getLogger(MemcachedCache.class);

    private static final String ATTR_LAST_UPDATE = "TS";

    public static final int DEFAULT_EXPIRY_PERIOD = 60 * 60 * 24 * 30; // 30 days
    public static final int DEFAULT_TIME_TO_WAIT_FOR_RESULT = 1000;

    private MemcachedClient memcachedClient = null;
    private int expiryPeriod; // in seconds
    private int imsiExpiryPeriod; // in seconds
    private int timeToWaitForResult; // in milliseconds

    private KeyValueSerializer serializer = new BuilderKeyValueSerializer();
    private KeyValueParser parser = new StateKeyValueParser();

    public static class Builder {

        private String servers;
        private boolean binaryProtocol = true;
        private int expiryPeriod = DEFAULT_EXPIRY_PERIOD;
        private int imsiExpiryPeriod = DEFAULT_EXPIRY_PERIOD;
        private int timeToWaitForResult = DEFAULT_TIME_TO_WAIT_FOR_RESULT;

        public Builder setServers(String servers) {
            this.servers = servers;
            return this;
        }

        public Builder setBinaryProtocol() {
            this.binaryProtocol = true;
            return this;
        }

        public Builder setTextProtocol() {
            this.binaryProtocol = false;
            return this;
        }

        public Builder setExpiryPeriod(int expiryPeriod) {
            this.expiryPeriod = expiryPeriod;
            return this;
        }

        public Builder setImsiExpiryPeriod(int imsiExpiryPeriod) {
            this.imsiExpiryPeriod = imsiExpiryPeriod;
            return this;
        }

        public Builder setTimeToWaitForResult(int timeToWaitForResult) {
            this.timeToWaitForResult = timeToWaitForResult;
            return this;
        }

        public MemcachedCache build() throws CacheException {
            if (servers == null || servers.length() == 0) {
                throw new IllegalStateException("Missing servers");
            }

            ConnectionFactoryBuilder connectionFactoryBuilder = new ConnectionFactoryBuilder().
//                    setLocatorType(ConnectionFactoryBuilder.Locator.CONSISTENT).
//                    setHashAlg(DefaultHashAlgorithm.NATIVE_HASH).
        setFailureMode(FailureMode.Cancel);

            if (this.binaryProtocol) {
                connectionFactoryBuilder.setProtocol(ConnectionFactoryBuilder.Protocol.BINARY);
            } else {
                connectionFactoryBuilder.setProtocol(ConnectionFactoryBuilder.Protocol.TEXT);
            }

            ConnectionFactory connFactory = connectionFactoryBuilder.build();
            try {
                MemcachedCache memcachedCache = new MemcachedCache(
                        new MemcachedClient(connFactory, AddrUtil.getAddresses(servers)),
                        this.expiryPeriod, this.imsiExpiryPeriod, this.timeToWaitForResult);
                LOG.info("Memcached client successfully created");
                return memcachedCache;
            } catch (IOException e) {
                throw new CacheException("Error while creating memcached client", e);
            }
        }

    }

    private MemcachedCache(MemcachedClient memcachedClient, int expiryPeriod, int imsiExpiryPeriod, int timeToWaitForResult) {
        this.memcachedClient = memcachedClient;
        this.expiryPeriod = expiryPeriod;
        this.imsiExpiryPeriod = imsiExpiryPeriod;
        this.timeToWaitForResult = timeToWaitForResult;
    }

    @Override
    public Profile get(String msisdn) throws CacheException {
        String serializedAttributes = (String) getAsync(msisdn);
        if (serializedAttributes == null) {
            return null;
        }
        Map<String, String> attributes = parser.parse(serializedAttributes);
        ProfileImpl.Builder builder = new ProfileImpl.Builder().
                setMsisdn(msisdn).
                setOperatorId(Location.OPERATOR_ID_VFCZ); // in the cache should be only VFCZ customer
        for (Map.Entry<String, String> attribute : attributes.entrySet()) {
            // last update needs to be handled separately
            if (attribute.getKey().equals(ATTR_LAST_UPDATE)) {
                builder.setLastUpdate(Long.parseLong(attribute.getValue()));
            } else {
                Attribute attr = new AttributeImpl(attribute.getKey(), attribute.getValue());
                builder.setAttribute(attr);
            }
        }
        return builder.build();
    }

    @Override
    public boolean set(Profile profile) throws CacheException {
        String serializedAttributes = serializeAttributes(profile);

        return setAsync(profile.getMsisdn(), serializedAttributes, getExpiryPeriod(this.expiryPeriod));
    }

    @Override
    public boolean add(Profile profile) throws CacheException {
        String serializedAttributes = serializeAttributes(profile);

        return addAsync(profile.getMsisdn(), serializedAttributes, getExpiryPeriod(this.expiryPeriod));
    }

    @Override
    public String getMsisdn(String imsi) throws CacheException {
        return (String) getAsync(imsi);
    }

    @Override
    public boolean setMsisdn(String imsi, String msisdn) throws CacheException {
        return setAsync(imsi, msisdn, getExpiryPeriod(this.imsiExpiryPeriod));
    }

    private String serializeAttributes(Profile profile) {
        List<Attribute> attributes = profile.getAttributes();

        // last update needs to be cached as well
        attributes.add(new AttributeImpl(ATTR_LAST_UPDATE, String.valueOf(profile.getLastUpdate())));
        return serializer.serialize(attributes);
    }

    @Override
    public boolean delete(String msisdn) throws CacheException {
        return deleteAsync(msisdn);
    }

    public void shutdown() {
        boolean shutdownResult = this.memcachedClient.shutdown(10, TimeUnit.SECONDS);
        if (shutdownResult) {
            LOG.info("Memcached client gracefully shutdown");
        } else {
            LOG.warn("Memcached client was not shutdown gracefully. Trying immediate shutdown ...");
            this.memcachedClient.shutdown();
            LOG.info("Memcached client successfully shutdown by immediate attempt");
        }
    }

    private Object getAsync(String key) throws CacheException {
        long ts = System.currentTimeMillis();
        GetFuture<Object> getResult = memcachedClient.asyncGet(key);
        try {
            Object result = getResult.get(this.timeToWaitForResult, TimeUnit.MILLISECONDS);
            LOG.info(String.format("SYS=CACHE OP=GET RES=SUCCESS RT=%d KEY=%s", (System.currentTimeMillis() - ts), key));
            return result;
        } catch (Exception e) {
            getResult.cancel(true);
            LOG.error(String.format("SYS=CACHE OP=GET RES=ERROR RT=%d KEY=%s", (System.currentTimeMillis() - ts), key));
            throw new CacheException(String.format("Error while getting object from cache (%s)", key), e);
        }
    }

    private boolean setAsync(String key, Object value, int exp) throws CacheException {
        long ts = System.currentTimeMillis();
        OperationFuture<Boolean> setResult = memcachedClient.set(key, exp, value);
        try {
            boolean result = setResult.get(this.timeToWaitForResult, TimeUnit.MILLISECONDS);
            LOG.info(String.format("SYS=CACHE OP=SET RES=SUCCESS RT=%d KEY=%s", (System.currentTimeMillis() - ts), key));
            return result;
        } catch (Exception e) {
            setResult.cancel();
            LOG.error(String.format("SYS=CACHE OP=GET RES=ERROR RT=%d KEY=%s", (System.currentTimeMillis() - ts), key));
            throw new CacheException(String.format("Error while setting object to cache (%s, %s)", key, value), e);
        }
    }

    private boolean addAsync(String key, Object value, int exp) throws CacheException {
        long ts = System.currentTimeMillis();
        OperationFuture<Boolean> addResult = memcachedClient.add(key, exp, value);
        try {
            boolean result = addResult.get(this.timeToWaitForResult, TimeUnit.MILLISECONDS);
            LOG.info(String.format("SYS=CACHE OP=ADD RES=SUCCESS RT=%d KEY=%s", (System.currentTimeMillis() - ts), key));
            return result;
        } catch (Exception e) {
            addResult.cancel();
            LOG.error(String.format("SYS=CACHE OP=ADD RES=ERROR RT=%d KEY=%s", (System.currentTimeMillis() - ts), key));
            throw new CacheException(String.format("Error while adding object to cache (%s, %s)", key, value), e);
        }
    }

    private boolean deleteAsync(String key) throws CacheException {
        long ts = System.currentTimeMillis();
        OperationFuture<Boolean> deleteResult = memcachedClient.delete(key);
        try {
            boolean result = deleteResult.get(this.timeToWaitForResult, TimeUnit.MILLISECONDS);
            LOG.info(String.format("SYS=CACHE OP=DELETE RES=SUCCESS RT=%d KEY=%s", (System.currentTimeMillis() - ts), key));
            return result;
//            TODO Consider handling the following exception. It occurs if both tests (see MemcachedCacheTests) are executed in one shot
//            Caused by: java.util.concurrent.ExecutionException: java.util.concurrent.CancellationException: Cancelled
//            at net.spy.memcached.internal.OperationFuture.get(OperationFuture.java:170)
//            at cz.vodafone.profilecache.cache.MemcachedCache.deleteAsync(MemcachedCache.java:147)
//            ... 37 more
//            Caused by: java.util.concurrent.CancellationException: Cancelled
//            ... 39 more

        } catch (Exception e) {
            deleteResult.cancel();
            LOG.error(String.format("SYS=CACHE OP=DELETE RES=ERROR RT=%d KEY=%s", (System.currentTimeMillis() - ts), key));
            throw new CacheException(String.format("Error while deleting key (%s)", key), e);
        }
    }

    private static int getExpiryPeriod(int exp) {
        // Quote from memcached documentation:
        // The actual value sent may either be Unix time (number of seconds since January 1, 1970, as a 32-bit value),
        // or a number of seconds starting from current time. In the latter case, this number of seconds may not exceed
        // 60*60*24*30 (number of seconds in 30 days); if the number sent by a client is larger than that, the server
        // will consider it to be real Unix time value rather than an offset from current time.
        if (exp > DEFAULT_EXPIRY_PERIOD) {
            return (int) (System.currentTimeMillis() / 1000 + exp);
        } else {
            return exp;
        }
    }

}
