package cz.vodafone.profilecache.maintenance.impl;

import cz.vodafone.profilecache.Utils;
import cz.vodafone.profilecache.maintenance.Status;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class StatusImplTest {

    @Before
    public void before() {
        Utils.initializeLogging();
    }

    @Test
    public void test() throws Exception {
        Status status = new StatusImpl(5);
        status.registerError(Status.Component.Cache);
        status.registerError(Status.Component.Cache);
        status.registerError(Status.Component.Cache);

        status.registerError(Status.Component.Database);
        status.registerError(Status.Component.Database);

        status.registerError(Status.Component.LocationProvider);

        Assert.assertEquals(3, status.getErrorCount(Status.Component.Cache));
        Assert.assertEquals(2, status.getErrorCount(Status.Component.Database));
        Assert.assertEquals(1, status.getErrorCount(Status.Component.LocationProvider));

        status.registerError(Status.Component.LocationProvider);
        status.registerError(Status.Component.LocationProvider);

        Assert.assertEquals(3, status.getErrorCount(Status.Component.Cache));
        Assert.assertEquals(2, status.getErrorCount(Status.Component.Database));
        Assert.assertEquals(3, status.getErrorCount(Status.Component.LocationProvider));

        Thread.sleep(5100);
        Assert.assertEquals(0, status.getErrorCount(Status.Component.Cache));
        Assert.assertEquals(0, status.getErrorCount(Status.Component.Database));
        Assert.assertEquals(0, status.getErrorCount(Status.Component.LocationProvider));

        status.registerError(Status.Component.Cache);

        status.registerError(Status.Component.Database);

        status.registerError(Status.Component.LocationProvider);

        Assert.assertEquals(1, status.getErrorCount(Status.Component.Cache));
        Assert.assertEquals(1, status.getErrorCount(Status.Component.Database));
        Assert.assertEquals(1, status.getErrorCount(Status.Component.LocationProvider));
    }

}
