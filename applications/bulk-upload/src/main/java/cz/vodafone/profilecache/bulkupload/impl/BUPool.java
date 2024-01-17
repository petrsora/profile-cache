package cz.vodafone.profilecache.bulkupload.impl;

import cz.vodafone.profilecache.bulkupload.AbstractBulkPool;
import cz.vodafone.profilecache.bulkupload.BulkUploadClientException;
import cz.vodafone.profilecache.bulkupload.BulkUploadDAO;
import org.apache.log4j.Logger;

/**
 * Created by: xpetsora
 * Date: 05.11.2013
 */
public class BUPool extends AbstractBulkPool {
    private static final Logger LOG = Logger.getLogger(BUPool.class);

    private BulkUploadDAO[] clientPool;

    public BUPool(int size) {
        super(size);
    }

    @Override
    protected void initClientPool() {
        clientPool = new BulkUploadDAOImpl[poolSize];
    }

    @Override
    protected void initClient(int i) throws BulkUploadClientException {
        clientPool[i] = new BulkUploadDAOImpl();
    }

    @Override
    public void close() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Waiting for tasks to finish ...");
        }
        while (!finished()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                LOG.error("Cannot sleep!", e);
            }
        }
        LOG.info("Jobs finished. Closing DB clients");
        if (clientPool != null) {
            for (BulkUploadDAO client : clientPool) {
                if (client != null) {
                    client.close();
                }
            }
        }
        LOG.info("DB clients closed.");
        executor.shutdown();
    }

    public BulkUploadDAO getClient(int index) {
        return clientPool[index];
    }
}
