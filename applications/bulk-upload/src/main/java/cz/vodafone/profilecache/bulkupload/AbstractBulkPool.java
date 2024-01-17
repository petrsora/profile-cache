package cz.vodafone.profilecache.bulkupload;

import org.apache.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by: xpetsora
 * Date: 05.11.2013
 */
public abstract class AbstractBulkPool {
    private static final Logger LOG = Logger.getLogger(AbstractBulkPool.class);

    protected ExecutorService executor;
    protected int poolSize;
    //protected BulkUploadConfig config;

    private boolean[] clientPoolAlloc;

    abstract protected void initClientPool();

    abstract protected void initClient(int i) throws BulkUploadClientException;

    public AbstractBulkPool(int size) {
        poolSize = size;
        //this.config = config;
        initClientPool();
        clientPoolAlloc = new boolean[poolSize];
        // init clients
        try {
            for (int i = 0; i < poolSize; i++) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Initializing " + this.getClass().getName() + " client [" + i + "]");
                }
                initClient(i);
                if (LOG.isDebugEnabled()) {
                    LOG.debug(this.getClass().getName() + " client [" + i + "] initialized.");
                }
                clientPoolAlloc[i] = false;
            }
        } catch (BulkUploadClientException e) {
            LOG.fatal(String.format("Error initializing client pool! (%s)", e.getMessage()), e);
            System.exit(1);
        }
        // init pool
        executor = Executors.newFixedThreadPool(poolSize);
    }

    synchronized private int allocateClient() {
        for (int i = 0; i < clientPoolAlloc.length; i++) {
            if (!clientPoolAlloc[i]) {
                clientPoolAlloc[i] = true;
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Allocated " + this.getClass().getName() + " client [" + i + "]");
                }
                return i;
            }
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("All " + this.getClass().getName() + " clients busy. No client allocated.");
        }
        return -1;
    }

    public void freeClient(int index) {
        clientPoolAlloc[index] = false;
    }

    public int submit(AbstractWorker job) {
        int client = allocateClient();
        if (client != -1) {
            job.setIndex(client);
            executor.execute(job);
            if (LOG.isDebugEnabled()) {
                LOG.debug("Job " + job + " submitted with client[" + client + "]");
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not submit job " + job + ", no free client!");
            }
        }
        return client;
    }

    protected boolean finished() {
        boolean someBusy = false;
        for (boolean busy : clientPoolAlloc) {
            someBusy = someBusy || busy;
        }
        return !someBusy;
    }

    public abstract void close();
}
