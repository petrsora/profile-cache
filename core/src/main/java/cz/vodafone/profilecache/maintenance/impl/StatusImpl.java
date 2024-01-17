package cz.vodafone.profilecache.maintenance.impl;

import cz.vodafone.profilecache.maintenance.Status;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class StatusImpl implements Status {

    private static final Logger LOG = Logger.getLogger(StatusImpl.class);

    private int resetInterval;

    private long resetTimestamp;
    private final Map<Component, AtomicInteger> errors = new HashMap<Component, AtomicInteger>();
    private AtomicBoolean anyError; // in order to speed up processing. if no error registered, there is no reason to reset

    /**
     * @param resetInterval in seconds
     */
    public StatusImpl(int resetInterval) {
        this.resetInterval = resetInterval * 1000;

        for (Status.Component component : Status.Component.values()) {
            this.errors.put(component, new AtomicInteger(0));
        }
        this.resetTimestamp = System.currentTimeMillis();
        anyError = new AtomicBoolean(false);
        LOG.info("StatusImpl successfully created");
    }

    @Override
    public void registerError(Component component) {
        reset();
        this.errors.get(component).incrementAndGet();
        anyError.set(true);
    }

    @Override
    public int getErrorCount(Component component) {
        if (!anyError.get()) {
            return 0;
        }
        if (reset()) {
            return 0;
        }
        return this.errors.get(component).get();
    }

    /**
     * @return true if counters have been reset
     */
    private boolean reset() {
        if (anyError.get() && (System.currentTimeMillis() > this.resetTimestamp + this.resetInterval)) {
            synchronized (this.errors) {
                if (System.currentTimeMillis() > this.resetTimestamp + this.resetInterval) {
                    for (Status.Component component : Status.Component.values()) {
                        this.errors.get(component).set(0);
                    }
                    this.resetTimestamp = System.currentTimeMillis();
                    this.anyError.set(false);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("All counters have been set to 0");
                    }
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

}
