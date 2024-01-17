package cz.vodafone.profilecache.cache;

import cz.vodafone.profilecache.Utils;
import cz.vodafone.profilecache.location.Location;
import cz.vodafone.profilecache.model.Profile;
import cz.vodafone.profilecache.location.impl.LocationImpl;
import cz.vodafone.profilecache.model.impl.AttributeImpl;
import cz.vodafone.profilecache.model.impl.ProfileImpl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

public class MemcachedCacheTest {

    private Profile PROFILE = new ProfileImpl.Builder().setMsisdn("420777350243").
            setOperatorId(LocationImpl.OPERATOR_ID_VFCZ).
            setAttribute(new AttributeImpl(Profile.ATTR_LOCATION, "V2", new Date())).
            setAttribute(new AttributeImpl(Profile.ATTR_HAS_MPENEZENKA_BARRING, "T", new Date())).
            setAttribute(new AttributeImpl(Profile.ATTR_IS_PREPAID, "F", new Date())).
            setAttribute(new AttributeImpl(Profile.ATTR_HAS_PRSMS_BARRING, "F", new Date())).
            setAttribute(new AttributeImpl(Profile.ATTR_IS_CHILD, "F", new Date())).
            setAttribute(new AttributeImpl(Profile.ATTR_IS_RESTRICTED, "0", new Date())).
            setAttribute(new AttributeImpl(Profile.ATTR_SCHEDULING_PROFILE, "NOLIMIT", new Date())).
            build();

    private MemcachedCache memcachedCache;

    static {
        Utils.initializeLogging();
    }

    @Before
    public void before() throws Exception {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        memcachedCache = new MemcachedCache.Builder().
                setBinaryProtocol().
                setServers(getServers()).
                build();
        memcachedCache.delete(PROFILE.getMsisdn());
    }

    @After
    public void after() {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        memcachedCache.shutdown();
    }

    @Test
    public void tests() throws Exception {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        Assert.assertNull(memcachedCache.get(PROFILE.getMsisdn()));

        Assert.assertFalse(memcachedCache.delete(PROFILE.getMsisdn()));

        Assert.assertTrue(memcachedCache.set(PROFILE));

        Profile profile = memcachedCache.get(PROFILE.getMsisdn());
        Assert.assertNotNull(profile);
        Assert.assertNotEquals(profile, PROFILE);
        Assert.assertEquals(profile.getMsisdn(), PROFILE.getMsisdn());
        Assert.assertEquals(profile.getAttribute(Profile.ATTR_LOCATION).getValue(), "V2");
        Assert.assertEquals(profile.getAttribute(Profile.ATTR_HAS_MPENEZENKA_BARRING).getValue(), "T");
        Assert.assertEquals(profile.getAttribute(Profile.ATTR_IS_PREPAID).getValue(), "F");
        Assert.assertEquals(profile.getAttribute(Profile.ATTR_HAS_PRSMS_BARRING).getValue(), "F");
        Assert.assertEquals(profile.getAttribute(Profile.ATTR_IS_CHILD).getValue(), "F");
        Assert.assertEquals(profile.getAttribute(Profile.ATTR_IS_RESTRICTED).getValue(), "0");
        Assert.assertEquals(profile.getAttribute(Profile.ATTR_SCHEDULING_PROFILE).getValue(), "NOLIMIT");

        Assert.assertTrue(memcachedCache.delete(PROFILE.getMsisdn()));

        Assert.assertNull(memcachedCache.get(PROFILE.getMsisdn()));

    }

    @Test
    public void bulkTest() throws Exception {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        int count = 10000;
        long ts = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            String msisdn = String.format("42077%07d", i);

            Profile profile = new ProfileImpl.Builder().
                    setMsisdn(msisdn).
                    setOperatorId(Location.OPERATOR_ID_VFCZ).
                    setAttribute(new AttributeImpl(Profile.ATTR_LOCATION, "V2", new Date())).
                    setAttribute(new AttributeImpl(Profile.ATTR_HAS_MPENEZENKA_BARRING, "T", new Date())).
                    setAttribute(new AttributeImpl(Profile.ATTR_IS_PREPAID, "F", new Date())).
                    setAttribute(new AttributeImpl(Profile.ATTR_HAS_PRSMS_BARRING, "F", new Date())).
                    setAttribute(new AttributeImpl(Profile.ATTR_IS_CHILD, "F", new Date())).
                    setAttribute(new AttributeImpl(Profile.ATTR_IS_RESTRICTED, "0", new Date())).
                    setAttribute(new AttributeImpl(Profile.ATTR_SCHEDULING_PROFILE, "NOLIMIT", new Date())).
                    build();
            Assert.assertTrue(memcachedCache.set(profile));
        }
        ts = System.currentTimeMillis() - ts;
        System.out.println(String.format("SET: count %d, total %d ms, avg %f ms", count, ts, (1.0 * ts / count)));

        ts = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            String msisdn = String.format("42077%07d", i);
            Assert.assertNotNull(memcachedCache.get(msisdn));
        }
        ts = System.currentTimeMillis() - ts;
        System.out.println(String.format("GET: count %d, total %d ms, avg %f ms", count, ts, (1.0 * ts / count)));

        ts = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            String msisdn = String.format("42077%07d", i);
            Assert.assertTrue(memcachedCache.delete(msisdn));
        }
        ts = System.currentTimeMillis() - ts;
        System.out.println(String.format("DELETE: count %d, total %d ms, avg %f ms", count, ts, (1.0 * ts / count)));
    }

    public static String getServers() {
//        return "s5java31:11211 s5java31:11212";
        return "localhost:11211 localhost:11212";
    }

}
