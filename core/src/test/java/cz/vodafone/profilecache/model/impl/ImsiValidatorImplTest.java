package cz.vodafone.profilecache.model.impl;

import cz.vodafone.profilecache.Utils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ImsiValidatorImplTest {

    @Before
    public void before() {
        Utils.initializeLogging();
    }

    @Test
    public void test() {
        ImsiValidatorImpl validator = new ImsiValidatorImpl();

        Assert.assertTrue(validator.isImsiValid("230031029384756"));

        Assert.assertFalse(validator.isImsiValid(null));
        Assert.assertFalse(validator.isImsiValid(""));
        Assert.assertFalse(validator.isImsiValid("23003102938475t"));
    }

}
