package cz.vodafone.profilecache.model.impl;

import cz.vodafone.profilecache.Utils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class MsisdnValidatorImplTest {

    @Before
    public void before() {
        Utils.initializeLogging();
    }

    @Test
    public void test() {
        MsisdnValidatorImpl validator = new MsisdnValidatorImpl();

        Assert.assertTrue(validator.isMsisdnValid("420777350243"));

        Assert.assertFalse(validator.isMsisdnValid(null));
        Assert.assertFalse(validator.isMsisdnValid(""));
        Assert.assertFalse(validator.isMsisdnValid("42077735024t"));
        Assert.assertFalse(validator.isMsisdnValid("42")); // length less than 3
        Assert.assertFalse(validator.isMsisdnValid("1234567890123456")); // length more than 15
    }

}
