package cz.vodafone.profilecache.bulkupload.impl;

import cz.vodafone.profilecache.bulkupload.AbstractBulkPool;
import cz.vodafone.profilecache.bulkupload.BulkUploadClientException;
import cz.vodafone.profilecache.cache.CacheException;
import cz.vodafone.profilecache.cache.MemcachedCache;
import cz.vodafone.profilecache.services.configuration.Configuration;
import cz.vodafone.profilecache.services.configuration.ConfigurationItems;
import org.apache.log4j.Logger;

/**
 * Created by: xpetsora
 * Date: 05.11.2013
 */
public class MCPool extends AbstractBulkPool {
    private static final Logger LOG = Logger.getLogger(MCPool.class);

    private MemcachedCache[] memCachePool;

    public MCPool(int size) {
        super(size);
    }

    @Override
    protected void initClientPool() {
        memCachePool = new MemcachedCache[poolSize];
    }

    @Override
    protected void initClient(int i) throws BulkUploadClientException {
        String servers = Configuration.getMandatoryString(ConfigurationItems.MEMCACHED_SERVERS);
        try {
            memCachePool[i] = new MemcachedCache.Builder().setBinaryProtocol().setServers(servers).build();
        } catch (CacheException e) {
            throw new BulkUploadClientException(
                    String.format("Error while building memcached client (%s)", e.getMessage()), e);
        }
    }

    @Override
    public void close() {
        while (!finished()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                LOG.error("Cannot sleep!", e);
            }
        }
        LOG.info("Closing MC clients ..");
        if (memCachePool != null) {
            for (MemcachedCache client : memCachePool) {
                if (client != null) {
                    client.shutdown();
                }
            }
        }
        LOG.info("MC clients closed.");
        executor.shutdown();
    }

    public MemcachedCache getClient(int index) {
        return memCachePool[index];
    }
}
