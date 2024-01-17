package cz.vodafone.profilecache.persistence;

import cz.vodafone.profilecache.Utils;
import cz.vodafone.profilecache.location.Location;
import cz.vodafone.profilecache.model.Profile;
import cz.vodafone.profilecache.model.impl.AttributeImpl;
import cz.vodafone.profilecache.model.impl.ProfileImpl;
import cz.vodafone.profilecache.persistence.helper.RawConnectionFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

public class ProviderDaoImplTest {

    private static final String MSISDN = "420777350243";
    private static final String NEW_MSISDN = "420777350244";
    private ProviderDaoImpl providerDao;

    @Before
    public void before() throws Exception {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        Utils.initializeLogging();
        providerDao = new ProviderDaoImpl(getConnectionFactory());

        Profile profile = new ProfileImpl.Builder().
                setMsisdn(MSISDN).
                setOperatorId(Location.OPERATOR_ID_VFCZ).
                setAttribute(new AttributeImpl(Profile.ATTR_LOCATION, "V2", new Date())).
                build();
        providerDao.deleteProfile(profile);

        profile = new ProfileImpl.Builder().
                setMsisdn(NEW_MSISDN).
                setOperatorId(Location.OPERATOR_ID_VFCZ).
                setAttribute(new AttributeImpl(Profile.ATTR_LOCATION, "V2", new Date())).
                build();
        providerDao.deleteProfile(profile);
    }

    @Test
    public void test() throws Exception {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        Profile foundProfile = providerDao.getProfile(MSISDN);
        Assert.assertNull(foundProfile);

        Date attrLastUpdate = new Date();
        Profile profile = new ProfileImpl.Builder().
                setMsisdn(MSISDN).
                setOperatorId(Location.OPERATOR_ID_VFCZ).
                setAttribute(new AttributeImpl(Profile.ATTR_LOCATION, "V2", attrLastUpdate)).
                setAttribute(new AttributeImpl(Profile.ATTR_IS_PREPAID, "T", attrLastUpdate)).
                build();

        Profile insertedProfile = providerDao.insertProfile(profile);
        Assert.assertNotNull(profile.getId());
        System.out.println("inserted profile:" + insertedProfile.toString());

        foundProfile = providerDao.getProfile(MSISDN);
        Assert.assertEquals(foundProfile.getId(), insertedProfile.getId());
        Assert.assertEquals(attrLastUpdate.getTime() / 1000 * 1000, // lower precision in DB (not in millis)
                foundProfile.getAttribute(Profile.ATTR_LOCATION).getLastUpdate().getTime());
        Assert.assertEquals(attrLastUpdate.getTime() / 1000 * 1000, // lower precision in DB (not in millis)
                foundProfile.getAttribute(Profile.ATTR_IS_PREPAID).getLastUpdate().getTime());
        System.out.println("found profile:" + foundProfile.toString());

        profile.setAttribute(new AttributeImpl(Profile.ATTR_HAS_MPENEZENKA_BARRING, "T", new Date()));
        providerDao.updateAttributes(profile);

        foundProfile = providerDao.getProfile(MSISDN);
        Assert.assertEquals(3, foundProfile.getAttributes().size());
        System.out.println("found profile:" + foundProfile.toString());

        providerDao.updateMsisdn(MSISDN, NEW_MSISDN);
        Assert.assertNull(providerDao.getProfile(MSISDN));
        Profile newProfile = providerDao.getProfile(NEW_MSISDN);
        Assert.assertNotNull(newProfile);
        Assert.assertEquals(NEW_MSISDN, newProfile.getMsisdn());
        Assert.assertEquals(3, newProfile.getAttributes().size());
        Assert.assertEquals(insertedProfile.getId(), newProfile.getId());

        providerDao.deleteProfile(foundProfile);
        providerDao.deleteProfile(newProfile);
    }

    public static RawConnectionFactory getConnectionFactory() {
//        return new RawConnectionFactory(
//                "oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:@l5devd31:1523:BMGDEV", "profile_own", "profile_own123");
        return new RawConnectionFactory(
                "oracle.jdbc.driver.OracleDriver", "jdbc:oracle:thin:@localhost:1521:xe", "profile_own", "profile_own123");
    }

}
