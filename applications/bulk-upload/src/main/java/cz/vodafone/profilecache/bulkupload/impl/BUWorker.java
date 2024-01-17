package cz.vodafone.profilecache.bulkupload.impl;

import cz.vodafone.profilecache.bulkupload.AbstractWorker;
import cz.vodafone.profilecache.bulkupload.BulkUploadClientException;
import cz.vodafone.profilecache.bulkupload.BulkUploadDAO;
import cz.vodafone.profilecache.model.Profile;
import org.apache.log4j.Logger;

import java.util.Vector;

/**
 * Created by: xpetsora
 * Date: 05.11.2013
 */
public class BUWorker extends AbstractWorker {
    private static final Logger LOG = Logger.getLogger(BUWorker.class);
    private BUPool pool;
    private MCPool mcPool;
    private Vector<Profile> data;

    public BUWorker(BUPool pool, MCPool mcPool, Vector<Profile> data) {
        this.pool = pool;
        this.mcPool = mcPool;
        this.data = data;
    }

    @Override
    protected void processData() {
        long startTime = 0;
        if (LOG.isDebugEnabled()) {
            startTime = System.currentTimeMillis();
            LOG.debug("Job " + this + " is upserting data chunk to DB using client[" + index + "]");
            if (mcPool == null) {
                LOG.debug("MemCache purge will be skipped.");
            }
        }
        BulkUploadDAO currClient = pool.getClient(index);
        for (Profile profile : data) {
            try {
                if (currClient.upsertProfile(profile) != null) {
                    //submit MC purge req
                    if (mcPool != null) {
                        MCWorker job = new MCWorker(mcPool, profile.getMsisdn());
                        while (mcPool.submit(job) == -1) {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                LOG.error("Cannot sleep!", e);
                            }
                        }
                    }
                }
            } catch (BulkUploadClientException e) {
                LOG.error(String.format("Error upserting profile %s (%s)", profile.toString(), e.getMessage()), e);
            }
        }
        try {
            currClient.commit();
        } catch (BulkUploadClientException e) {
            LOG.error(String.format("Cannot commit (%s)", e.getMessage()), e);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Job " + this + " finished upserting data chunk to DB. Elapsed time: " + (System.currentTimeMillis() - startTime));
        }
    }

    @Override
    protected void free() {
        pool.freeClient(index);
    }
}
