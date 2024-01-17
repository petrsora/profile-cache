package cz.vodafone.profilecache.bulkupload.impl;

import cz.vodafone.profilecache.bulkupload.AbstractWorker;
import cz.vodafone.profilecache.cache.CacheException;
import cz.vodafone.profilecache.cache.MemcachedCache;
import org.apache.log4j.Logger;

/**
 * Created by: xpetsora
 * Date: 06.11.2013
 */
public class MCWorker extends AbstractWorker {
    private static final Logger LOG = Logger.getLogger(MCWorker.class);

    private MCPool mcPool;
    private String data;

    public MCWorker(MCPool mcPool, String data) {
        this.mcPool = mcPool;
        this.data = data;
    }

    @Override
    protected void free() {
        mcPool.freeClient(index);
    }

    @Override
    protected void processData() {
        MemcachedCache currClient = mcPool.getClient(index);
        try {
            long startTime = 0;
            if (LOG.isDebugEnabled()) {
                startTime = System.currentTimeMillis();
                LOG.debug("Purging item " + data + " from MemCache...");
            }
            currClient.delete(data);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Item " + data + " purged from MemCache. Elapsed time: " + (System.currentTimeMillis() - startTime));
            }
        } catch (CacheException e) {
            LOG.error(String.format("Error purging item \"%s\" from memCache!!!", e.getMessage()), e);
        }
    }
}
