package cz.vodafone.profilecache.maintenance;

public interface PerformanceStatisticsHandler {

    public void registerCorrectTx(long time);

    public void registerErrorTx(long time);

    public Statistic getStatisticAndReset();

}
