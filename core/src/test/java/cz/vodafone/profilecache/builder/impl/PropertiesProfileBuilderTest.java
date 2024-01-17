package cz.vodafone.profilecache.builder.impl;

import cz.vodafone.profilecache.Utils;
import cz.vodafone.profilecache.builder.ProfileBuilderException;
import cz.vodafone.profilecache.location.Location;
import cz.vodafone.profilecache.location.impl.LocationImpl;
import cz.vodafone.profilecache.model.Profile;
import cz.vodafone.profilecache.model.impl.ProfileImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.FileReader;
import java.util.Properties;

public class PropertiesProfileBuilderTest {

    @Before
    public void before() {
        Utils.initializeLogging();
    }

    @Test
    public void tests() throws Exception {
        String msisdn = "420777350243";
//        FileReader fr = new FileReader("core/src/test/resources/profiles.properties");
        FileReader fr = new FileReader("src/test/resources/profiles.properties");

        Properties props = new Properties();
        props.load(fr);
        PropertiesProfileBuilder builder = new PropertiesProfileBuilder(props);

        Profile profile = builder.buildProfile(msisdn, new LocationImpl(Location.OPERATOR_ID_VFCZ, "V2"));
        Assert.assertNotNull(profile);
        Assert.assertEquals(profile.getOperatorId(), Location.OPERATOR_ID_VFCZ);
        Assert.assertEquals(profile.getAttribute(ProfileImpl.ATTR_SCHEDULING_PROFILE).getValue(), "NOLIMIT");

        profile = builder.buildProfile(msisdn, new LocationImpl("232", Location.LOCATION_OFFNET));
        Assert.assertNotNull(profile);
        Assert.assertEquals(profile.getOperatorId(), "232");
        Assert.assertEquals(profile.getAttribute(ProfileImpl.ATTR_LOCATION).getValue(), "OFFNET");
        Assert.assertNull(profile.getAttribute(ProfileImpl.ATTR_SCHEDULING_PROFILE));

        try {
            builder.buildProfile(msisdn, new LocationImpl(Location.OPERATOR_ID_VFCZ, "UNKNOWN"));
            throw new Exception();
        } catch (ProfileBuilderException e) {
            // that's ok
        }
    }

}
