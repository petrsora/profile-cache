package cz.vodafone.profilecache.maintenance.impl;

import cz.vodafone.profilecache.maintenance.PerformanceStatisticsHandler;
import cz.vodafone.profilecache.maintenance.Statistic;

import java.util.concurrent.atomic.AtomicLong;

public class PerformanceStatisticsHandlerImpl implements PerformanceStatisticsHandler {

    private AtomicLong numberOfCorrectTx;
    private AtomicLong numberOfErrorTx;
    private AtomicLong totalTime;
    private AtomicLong maxTime;

    public PerformanceStatisticsHandlerImpl() {
        reset();
    }

    @Override
    public void registerCorrectTx(long time) {
        numberOfCorrectTx.incrementAndGet();
        handleTime(time);
    }

    @Override
    public void registerErrorTx(long time) {
        numberOfErrorTx.incrementAndGet();
        handleTime(time);
    }

    @Override
    /**
     * not a thread safe method - it needs to be handled in client code
     */
    public Statistic getStatisticAndReset() {
        long correct = numberOfCorrectTx.get();
        long error = numberOfErrorTx.get();
        long average = (correct + error == 0) ? 0 : totalTime.get() / (correct + error);
        long max = maxTime.get();
        reset();
        return new Statistic(correct, error, average, max);
    }

    private void reset() {
        numberOfCorrectTx = new AtomicLong(0);
        numberOfErrorTx = new AtomicLong(0);
        totalTime = new AtomicLong(0);
        maxTime = new AtomicLong(0);
    }

    private void handleTime(long time) {
        totalTime.addAndGet(time);
        if (time > maxTime.get()) {
            maxTime.set(time);
        }
    }

}
