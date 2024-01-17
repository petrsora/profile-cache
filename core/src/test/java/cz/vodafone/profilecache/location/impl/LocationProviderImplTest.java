package cz.vodafone.profilecache.location.impl;

import cz.vodafone.profilecache.Utils;
import cz.vodafone.profilecache.location.Location;
import cz.vodafone.profilecache.location.LocationProvider;
import cz.vodafone.profilecache.location.LocationProviderException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;

public class LocationProviderImplTest {

    @Before
    public void before() {
        Utils.initializeLogging();
    }

    @Test
    public void test() throws Exception {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        URL wsdlLocation = new URL("file:/app01/profcach/conf/GetOperatorMembershipEndpointService-1.5.0.wsdl");
        // incorrect url
        LocationProviderImpl locationProvider = new LocationProviderImpl.Builder().
                setWsdlLocation(wsdlLocation).
//                setEndpointAddress("http://b1locd21:6080/dispatcher/getOperatorMembershipEndpoint").
        setEndpointAddress("http://localhost:9191/mockGetOperatorMembershipServiceBinding").
                setApplicationCode("profilecache").
                setUserId("profilecache").
                setUsername("profilecache").
                setPassword("profilecache123").
                build();
        try {
            locationProvider.getLocation("420777350243");
            throw new Exception();
        } catch (LocationProviderException e) {
            // that's ok
        }

        // correct prod url
        locationProvider = new LocationProviderImpl.Builder().
                setWsdlLocation(wsdlLocation).
//                setEndpointAddress("http://b1locd21:6180/dispatcher/getOperatorMembershipEndpoint").
        setEndpointAddress("http://localhost:9090/mockGetOperatorMembershipServiceBinding").
                setApplicationCode("profilecache").
                setUserId("profilecache").
                build();

        Location loc = locationProvider.getLocation("420777350243");
        Assert.assertNotNull(loc);
        Assert.assertEquals(Location.OPERATOR_ID_VFCZ, loc.getOperatorId());
        Assert.assertEquals("V2", loc.getLocation());

        loc = locationProvider.getLocation("420608553286");
        Assert.assertNotNull(loc);
        Assert.assertEquals("232", loc.getOperatorId());
        Assert.assertEquals(Location.LOCATION_OFFNET, loc.getLocation());
    }

    @Test
    public void testGetMsisdn() throws Exception {
        if (!Utils.runIntegrationTests()) {
            return;
        }

        URL wsdlLocation = new URL("file:/app01/profcach/conf/GetOperatorMembershipEndpointService-1.5.0.wsdl");

        LocationProviderImpl locationProvider = new LocationProviderImpl.Builder().
                setWsdlLocation(wsdlLocation).
//                setEndpointAddress("http://b1locd21:6180/dispatcher/getOperatorMembershipEndpoint").
        setEndpointAddress("http://localhost:9090/mockGetOperatorMembershipServiceBinding").
                setApplicationCode("profilecache").
                setUserId("profilecache").
                build();

        String msisdn = locationProvider.getMsisdn("230030090937423");
        Assert.assertNotNull(msisdn);
        Assert.assertEquals("420777350243",msisdn);
    }

}
