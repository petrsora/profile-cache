package cz.vodafone.profilecache.maintenance.impl;

import cz.vodafone.profilecache.maintenance.PerformanceStatisticsHandler;
import cz.vodafone.profilecache.maintenance.Statistic;
import org.junit.Assert;
import org.junit.Test;

public class PerformanceStatisticsHandlerImplTest {

    @Test
    public void test() {
        PerformanceStatisticsHandler handler = new PerformanceStatisticsHandlerImpl();
        handler.registerCorrectTx(500);
        handler.registerErrorTx(100);
        handler.registerCorrectTx(1000);
        handler.registerErrorTx(400);
        Statistic stat = handler.getStatisticAndReset();
        Assert.assertEquals(2, stat.getNumberOfCorrectTx());
        Assert.assertEquals(2, stat.getNumberOfErrorTx());
        Assert.assertEquals(1000, stat.getMaxTime());
        Assert.assertEquals(500, stat.getAverageTime());

        // without time
        handler.registerCorrectTx(0);
        handler.registerErrorTx(0);
        handler.registerCorrectTx(0);
        handler.registerCorrectTx(0);
        stat = handler.getStatisticAndReset();
        Assert.assertEquals(3, stat.getNumberOfCorrectTx());
        Assert.assertEquals(1, stat.getNumberOfErrorTx());
        Assert.assertEquals(0, stat.getMaxTime());
        Assert.assertEquals(0, stat.getAverageTime());
    }

}
