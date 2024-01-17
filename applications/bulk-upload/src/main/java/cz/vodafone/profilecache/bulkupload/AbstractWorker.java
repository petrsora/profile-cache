package cz.vodafone.profilecache.bulkupload;

import org.apache.log4j.Logger;

/**
 * Created by: xpetsora
 * Date: 05.11.2013
 */
public abstract class AbstractWorker implements Runnable {
    private static final Logger LOG = Logger.getLogger(AbstractWorker.class);

    protected int index = -1;

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public void run() {
        // chk index
        if (index == -1) {
            LOG.error("Client index is not set!");
            return;
        }
        // process batch
        processData();
        free();

    }

    abstract protected void free();

    abstract protected void processData();
}
